Neo4j Unmanaged Extension
=========================

# Instructions

1. Build it:

        mvn clean package

2. Copy target/extension-1.0.jar to the plugins/ directory of your Neo4j server.

3. Download and copy additional jars to the plugins/ directory of your Neo4j server.

        wget http://repo1.maven.org/maven2/com/google/guava/guava/18.0/guava-18.0.jar
        
4. Configure Neo4j by adding a line to conf/neo4j-server.properties:

        org.neo4j.server.thirdparty_jaxrs_classes=com.maxdemarzi=/v1