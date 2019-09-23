package org.infinispan.server.functional;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.infinispan.server.test.DatabaseServerRule;
import org.infinispan.server.test.InfinispanServerRule;
import org.infinispan.server.test.InfinispanServerTestConfiguration;
import org.infinispan.server.test.InfinispanServerTestMethodRule;
import org.infinispan.server.test.ServerRunMode;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/

//@Category(Security.class)
public class PersistenceIT {
   @ClassRule
   public static InfinispanServerRule SERVERS = new InfinispanServerRule(new InfinispanServerTestConfiguration("configuration/JDBCTest.xml")
         .numServers(1).runMode(ServerRunMode.CONTAINER).artifacts("com.h2database:h2:1.4.199"));

   @ClassRule
   public static DatabaseServerRule DATABASE = new DatabaseServerRule(SERVERS);

   @Rule
   public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(SERVERS);

   @Test
   public void testReadWrite() {
      try {

         GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder().defaultCacheName("default");
         ConfigurationBuilder builder = new ConfigurationBuilder();
         Configuration build = builder.persistence().addStore(JdbcStringBasedStoreConfigurationBuilder.class)
               .fetchPersistentState(false)
               .ignoreModifications(false)
               .purgeOnStartup(false)
               .shared(false)
               .table()
               .dropOnExit(true)
               .createOnStart(true)
               .tableNamePrefix("ISPN_STRING_TABLE")
               .idColumnName("ID_COLUMN").idColumnType(DATABASE.getIdColumType())
               .dataColumnName("DATA_COLUMN").dataColumnType(DATABASE.getDataColumType())
               .timestampColumnName("TIMESTAMP_COLUMN").timestampColumnType(DATABASE.getTimeStampColumType())
               .connectionPool()
               .connectionUrl(DATABASE.container.getJdbcUrl())
               .username(DATABASE.container.getUsername())
               .password(DATABASE.container.getPassword())
               .driverClass(DATABASE.container.getDriverClassName())
               .build();

         RemoteCache<String, String> cache = new RemoteCacheManager().administration().getOrCreateCache("test", builder.build());
         cache.put("k1", "v1");
         System.out.println(cache.get("k1"));
         cache.put("k1", "v1");
         System.out.println(cache.get("k1"));
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
