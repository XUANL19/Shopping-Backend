package com.shopping.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = "com.shopping.order.dao")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    public String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        CreateKeyspaceSpecification specification = CreateKeyspaceSpecification
                .createKeyspace(keyspaceName)
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(1);
        return Collections.singletonList(specification);
    }

    @Override
    protected List<String> getStartupScripts() {
        return Collections.unmodifiableList(List.of(
                // Create order_item UDT
                "CREATE TYPE IF NOT EXISTS " + keyspaceName + ".order_item (" +
                        "upc bigint," +
                        "purchase_count int" +
                        ");",

                // Create orders table
                "CREATE TABLE IF NOT EXISTS " + keyspaceName + ".orders (" +
                        "order_id text PRIMARY KEY," +
                        "user_id uuid," +  // Using UUID type
                        "order_status text," +
                        "items list<frozen<order_item>>," +
                        "created_at timestamp," +
                        "updated_at timestamp," +
                        "idempotency_key text" +
                        ");",

                // Create indexes
                "CREATE INDEX IF NOT EXISTS orders_user_id_idx ON " +
                        keyspaceName + ".orders (user_id);",
                "CREATE INDEX IF NOT EXISTS orders_idempotency_key_idx ON " +
                        keyspaceName + ".orders (idempotency_key);"
        ));
    }
}