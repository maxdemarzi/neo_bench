package com.maxdemarzi.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxdemarzi.Labels;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.maxdemarzi.Server.JSON_UTF8;

public class GetDocumentHandler implements HttpHandler {

    private static GraphDatabaseService db;
    private static ObjectMapper objectMapper;

    public GetDocumentHandler(GraphDatabaseService graphDB, ObjectMapper objectMapper) {
        this.db = graphDB;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, JSON_UTF8);
        String key = exchange.getAttachment(io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY)
                .getParameters().get("key");

        Map<String, Object> results = new HashMap<>();

        try (Transaction tx = db.beginTx()) {
            final Node profile = db.findNode(Labels.PROFILES, "_key", key);
            for (String prop : profile.getPropertyKeys()) {
                results.put(prop, profile.getProperty(prop));
            }
        }

        exchange.setResponseCode(200);
        exchange.getResponseSender().send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(results)));
    }
}
