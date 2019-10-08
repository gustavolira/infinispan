package org.infinispan.server.functional;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.server.test.InfinispanServerTestMethodRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class PooledConnectionOperations {

    @Rule
    public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(PersistenceIT.SERVERS);

    private ConfigurationBuilder createConfigurationBuilder() {

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class)
                .fetchPersistentState(false)
                .ignoreModifications(false)
                .purgeOnStartup(false)
                .shared(false)
                .table()
                .dropOnExit(true)
                .createOnStart(true)
                .tableNamePrefix("ISPN_STRING_TABLE")
                .idColumnName("ID_COLUMN").idColumnType(PersistenceIT.DATABASE.databaseInfo.getIdColumType())
                .dataColumnName("DATA_COLUMN").dataColumnType(PersistenceIT.DATABASE.databaseInfo.getDataColumType())
                .timestampColumnName("TIMESTAMP_COLUMN").timestampColumnType(PersistenceIT.DATABASE.databaseInfo.getTimeStampColumType())
                .connectionPool()
                .connectionUrl(PersistenceIT.DATABASE.databaseInfo.jdbcUrl())
                .username(PersistenceIT.DATABASE.databaseInfo.username())
                .password(PersistenceIT.DATABASE.databaseInfo.password())
                .driverClass(PersistenceIT.DATABASE.databaseInfo.driverClassName());
        return builder;
    }

    @Test
    public void testTwoCachesSameCacheStore() {
        RemoteCache<String, String> cache1 = SERVER_TEST.getHotRodCache(createConfigurationBuilder(), "cache1");
        RemoteCache<String, String> cache2 = SERVER_TEST.getHotRodCache(createConfigurationBuilder(), "cache2");
        cache1.put("k1", "v1");
        String firstK1 = cache1.get("k1");
        assertEquals("v1", firstK1);
        assertNull(cache2.get("k1"));

        cache2.put("k2", "v2");
        assertEquals("v2", cache2.get("k2"));
        assertNull(cache1.get("k2"));

        assertCleanCacheAndStore(cache1);
        assertCleanCacheAndStore(cache2);

    }

    @Test
    public void testPutGetRemove() {

        RemoteCache<String, String> cache = SERVER_TEST.getHotRodCache(createConfigurationBuilder(), "test");
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        assertNotNull(cache.get("k1"));
        assertNotNull(cache.get("k2"));

        cache.stop();
        cache.start();

        assertNotNull(cache.get("k1"));
        assertNotNull(cache.get("k2"));
        assertEquals("v1", cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
        cache.remove("k1");
        assertNull(cache.get("k1"));

    }

    protected void assertCleanCacheAndStore(RemoteCache cache) {
        cache.clear();
        assertEquals(0, cache.size());

    }

}
