package org.infinispan.server.test.persistence;

import java.util.Properties;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public abstract class DatabaseInfo {

    private Properties properties;

    public DatabaseInfo(Properties properties) {
        this.properties = properties;
    }

    public String getDataColumType() {
        return properties.getProperty("data.column.type");
    }

    public String getTimeStampColumType() {
        return properties.getProperty("timestamp.column.type");
    }

    public String getIdColumType() {
        return properties.getProperty("id.column.type");
    }

    public abstract String jdbcUrl();

    public abstract String username();

    public abstract String password();

    public abstract String driverClassName();

}
