package org.infinispan.server.test.persistence;

import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.Properties;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class ContainerDatabase  extends DatabaseInfo {

    JdbcDatabaseContainer container;

    public ContainerDatabase(JdbcDatabaseContainer JdbcDatabaseContainer, Properties properties) {
        super(properties);
        this.container = JdbcDatabaseContainer;
    }

    @Override
    public String jdbcUrl() {
        return container.getJdbcUrl();
    }

    @Override
    public String username() {
        return container.getUsername();
    }

    @Override
    public String password() {
        return container.getPassword();
    }

    @Override
    public String driverClassName() {
        return container.getDriverClassName();
    }
}
