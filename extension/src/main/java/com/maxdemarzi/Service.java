package com.maxdemarzi;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.cursor.NodeCursor;
import org.neo4j.kernel.api.cursor.PropertyCursor;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Path("/service")
public class Service {

    private static GraphDatabaseService db;
    private final GraphDatabaseAPI dbapi;

    public Service(@Context GraphDatabaseService graphDatabaseService) {
        db = graphDatabaseService;
        dbapi = (GraphDatabaseAPI)graphDatabaseService;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final LoadingCache<String, Long> profiles = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .build(
                    new CacheLoader<String, Long>() {
                        public Long load(String key) {
                            return getProfileNodeId(key);
                        }
                    });

    private static Long getProfileNodeId(String key){
        Node profile = db.findNode(Labels.PROFILES, "_key", key);
        return profile.getId();
    }

    private static final LoadingCache<String, String> profilesKV = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .build(
                    new CacheLoader<String, String>() {
                        public String load(String key) {
                            return getProfileProperties(key);
                        }
                    });

    private static String getProfileProperties(String key) {
        Map<String, Object> results = new HashMap<>();
        try (Transaction tx = db.beginTx()) {
            Node profile = db.findNode(Labels.PROFILES, "_key", key);
            for (String prop : profile.getPropertyKeys()) {
                results.put(prop, profile.getProperty(prop));
            }
        }

        try {
            return objectMapper.writeValueAsString(results);
        } catch (IOException e) {
            return "Error retrieving Profile Properties";
        }
    }

    @GET
    @Path("/helloworld")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("/warmup")
    public String warmUp(@Context GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            for (Node n : GlobalGraphOperations.at(db).getAllNodes()) {
                n.getPropertyKeys();
                for (Relationship relationship : n.getRelationships()) {
                    relationship.getPropertyKeys();
                    relationship.getStartNode();
                }
            }

            for (Relationship relationship : GlobalGraphOperations.at(db).getAllRelationships()) {
                relationship.getPropertyKeys();
                relationship.getNodes();
            }
        }
        return "Warmed up and ready to go!";
    }

    @GET
    @Path("/getDocument/{key}")
    public Response GetDocument(@PathParam("key") String key, @Context GraphDatabaseService db) throws IOException {
        Map<String, Object> results = new HashMap<>();

        try (Transaction tx = db.beginTx()) {
            final Node profile = db.findNode(Labels.PROFILES, "_key", key);
            for (String prop : profile.getPropertyKeys()) {
                results.put(prop, profile.getProperty(prop));
            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/getDocumentSkipIndex/{key}")
    public Response GetDocumentSkipIndex(@PathParam("key") String key, @Context GraphDatabaseService db) throws IOException, ExecutionException {
        Map<String, Object> results = new HashMap<>();

        try (Transaction tx = db.beginTx()) {
            final Node profile = db.getNodeById(profiles.get(key));
            for (String prop : profile.getPropertyKeys()) {
                results.put(prop, profile.getProperty(prop));
            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }

    @GET
    @Path("/getDocumentKV/{key}")
    public Response GetDocumentKV(@PathParam("key") String key, @Context GraphDatabaseService db) throws IOException, ExecutionException {

        return Response.ok().entity(profilesKV.get(key)).build();
    }

    @GET
    @Path("/getDocumentSPI/{key}")
    public Response GetDocumentSPI(@PathParam("key") String key, @Context GraphDatabaseService db) throws IOException, SchemaRuleNotFoundException, IndexNotFoundKernelException, PropertyKeyIdNotFoundKernelException {
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
                    if(props.next()) {
                        propertyId = props.getKeyIndexId();
                        results.put(ops.propertyKeyGetName(propertyId), props.getValue());
                    }
                }

            }
        }

        return Response.ok().entity(objectMapper.writeValueAsString(results)).build();
    }
}