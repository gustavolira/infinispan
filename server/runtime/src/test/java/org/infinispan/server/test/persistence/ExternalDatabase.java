package org.infinispan.server.test.persistence;

import java.util.Properties;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class ExternalDatabase extends DatabaseInfo {

    private static final String DRIVER_CLASS = System.getProperty("org.infinispan.test.server.jdbc.database.driverClass");
    public static final String URL = System.getProperty("org.infinispan.test.server.jdbc.database.url");
    public static final String USERNAME = System.getProperty("org.infinispan.test.server.jdbc.database.username");
    public static final String PASSWORD = System.getProperty("org.infinispan.test.server.jdbc.database.password");

    public ExternalDatabase(Properties properties) {
        super(properties);
    }

    public static boolean isExternalDatabase() {
        return URL != null && DRIVER_CLASS != null && USERNAME != null && PASSWORD != null;
    }

    @Override
    public String jdbcUrl() {
        return URL;
    }

    @Override
    public String username() {
        return USERNAME;
    }

    @Override
    public String password() {
        return PASSWORD;
    }

    @Override
    public String driverClassName() {
        return DRIVER_CLASS;
    }
}
