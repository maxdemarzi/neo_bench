package com.maxdemarzi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.maxdemarzi.handlers.GetDocumentHandler;
import com.maxdemarzi.handlers.GetDocumentSPIHandler;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Server {

    public static final String JSON_UTF8 = MediaType.JSON_UTF_8.toString();

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static GraphDatabaseService graphDb;

    public static void main(final String[] args) throws IOException {
        Properties properties = ServerProperties.getProperties();

        graphDb = new HighlyAvailableGraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(new File(properties.getProperty("database.location")))
                .loadPropertiesFromFile(properties.getProperty("db.tuning.properties"))
                .newGraphDatabase();

        registerShutdownHook(graphDb);

        Undertow.builder()
                .addHttpListener(7474, "localhost")
                .setBufferSize(1024 * 16)
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2)
                .setHandler(new RoutingHandler()
                        .add("GET", "/v1/service/getDocument/{key}", new GetDocumentHandler(graphDb, objectMapper))
                        .add("GET", "/v1/service/getDocumentSPI/{key}", new GetDocumentSPIHandler(graphDb, objectMapper)))
                .setWorkerThreads(200)
                .build()
                .start();
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

}
