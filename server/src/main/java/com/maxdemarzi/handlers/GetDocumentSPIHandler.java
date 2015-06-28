package com.maxdemarzi.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxdemarzi.Labels;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.cursor.NodeCursor;
import org.neo4j.kernel.api.cursor.PropertyCursor;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.maxdemarzi.Server.JSON_UTF8;

public class GetDocumentSPIHandler implements HttpHandler {

    private static GraphDatabaseService db;
    private final GraphDatabaseAPI dbapi;
    private static ObjectMapper objectMapper;

    public GetDocumentSPIHandler(GraphDatabaseService graphDB, ObjectMapper objectMapper) {
        this.db = graphDB;
        this.objectMapper = objectMapper;
        this.dbapi = (GraphDatabaseAPI)graphDB;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, JSON_UTF8);
        String key = exchange.getAttachment(io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY)
                .getParameters().get("key");

        Map<String, Object> results = new HashMap<>();

        try (Transaction tx = db.beginTx()) {
            ThreadToStatementContextBridge ctx = dbapi.getDependencyResolver().resolveDependency(ThreadToStatementContextBridge.class);
            ReadOperations ops = ctx.get().readOperations();
            int labelId = ops.labelGetForName(Labels.PROFILES.name());
            int propertyId = ops.propertyKeyGetForName("_key");
            IndexDescriptor indexDescriptor = ops.indexesGetForLabelAndPropertyKey(labelId, propertyId);
            NodeCursor nodeCursor =  ops.nodeCursorGetFromIndexLookup( indexDescriptor, key);
            if (nodeCursor.next()) {
                try (PropertyCursor props = nodeCursor.properties()){
                    while(props.next()) {
                        propertyId = props.getKeyIndexId();
                        results.put(ops.propertyKeyGetName(propertyId), props.getValue());
                    }
                }

            }
        }

        exchange.setResponseCode(200);
        exchange.getResponseSender().send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(results)));
    }
}