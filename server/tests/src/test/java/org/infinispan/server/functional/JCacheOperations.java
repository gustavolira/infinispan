package org.infinispan.server.functional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.Properties;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.server.test.junit4.InfinispanServerRule;
import org.infinispan.server.test.junit4.InfinispanServerTestMethodRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/
public class JCacheOperations {
   @ClassRule
   public static InfinispanServerRule SERVERS = ClusteredIT.SERVERS;
   @Rule
   public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(SERVERS);

   @Test
   public void testJCacheOperations() throws IOException {
      Properties properties = new Properties();
      InetAddress serverAddress = SERVERS.getServerDriver().getServerAddress(0);
      properties.put(ConfigurationProperties.SERVER_LIST, serverAddress.getHostAddress() + ":11222");
      properties.put(ConfigurationProperties.CACHE_PREFIX + SERVER_TEST.getMethodName() + ConfigurationProperties.CACHE_TEMPLATE_NAME_SUFFIX, "org.infinispan.DIST_SYNC");
      File file = new File("target/test-classes/jcache-hotrod-client.properties");
      try (FileOutputStream fos = new FileOutputStream(file)) {
         properties.store(fos, null);
      }
      URI uri = file.toURI();
      CachingProvider provider = Caching.getCachingProvider();
      try (CacheManager cacheManager = provider.getCacheManager(uri, this.getClass().getClassLoader())) {
         Cache<String, String> cache = cacheManager.getCache(SERVER_TEST.getMethodName());
         cache.put("k1", "v1");
         int size = getCacheSize(cache);
         assertEquals(1, size);
         assertEquals("v1", cache.get("k1"));
         cache.remove("k1");
         assertEquals(0, getCacheSize(cache));
      }
   }

   @Test
   public void shouldCreateReplicatedCache() throws Exception {
      URI uri = getClass().getClassLoader().getResource("configuration/cache-container/replicated.xml").toURI();
      String chacheName = "replicated-cache";
      InetAddress serverAddress = SERVERS.getServerDriver().getServerAddress(0);
      Properties vendorProperties = new Properties();
      vendorProperties.put("infinispan.client.hotrod.server_list", serverAddress.getHostAddress() + ":11222");
      vendorProperties.put("infinispan.client.hotrod.marshaller", "org.infinispan.commons.marshall.JavaSerializationMarshaller");
      vendorProperties.put("infinispan.client.hotrod.java_serial_whitelist", ".*");
      CacheManager cacheManager = Caching.getCachingProvider("org.infinispan.jcache.remote.JCachingProvider").getCacheManager(uri, Thread.currentThread().getContextClassLoader(), vendorProperties);
      cacheManager.createCache(chacheName, new MutableConfiguration<>());

      RestClient restClient = SERVER_TEST.rest().get();
      RestResponse restResponse = restClient.cache(chacheName).configuration().toCompletableFuture().get();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode cacheConfig = mapper.readTree(restResponse.getBody());

      assertNull(cacheConfig.get("local-cache"));
      assertNotNull(cacheConfig.get("replicated-cache"));
   }

   private int getCacheSize(Cache<String, String> cache) {
      int size = 0;
      for (Iterator<Cache.Entry<String, String>> it = cache.iterator(); it.hasNext(); it.next()) {
         ++size;
      }
      return size;
   }
}
