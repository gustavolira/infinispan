package org.infinispan.it.endpoints.backwardcompatibility;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.test.AbstractInfinispanTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.testng.AssertJUnit.assertTrue;

@Test(groups = "functional", testName = "it.endpoints.backwardcompatibility.XptoIT")
public class HotrodClientBackwardCompatibilityIT extends AbstractInfinispanTest {

    private static final int TOTAL_ENTRIES = 5;
    private static final Integer ISPN_9 = 9;

    public void testEmbeddedPutHotRodKeySet() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if(getHotrodClientVersion() > ISPN_9) {
            throw new SkipException("This is a backward compatibility test and should use an older ispn client hotrod version");
        }
        RemoteCache<String, String> remoteCache = getRemoteCache();

        Map<String, String> map = new HashMap<>();
        IntStream.rangeClosed(1, TOTAL_ENTRIES).forEach(i -> map.put("key" + i, "value" + i));
        remoteCache.putAll(map);
        //Using reflection to avoid IDE complain about missing method
        Method getBulkMethod = remoteCache.getClass().getDeclaredMethod("getBulk");

        if(System.getProperty("infinisan.enable.hotrod.bulk.really.really.for.the.last.time") == null) {
            try {
                getBulkMethod.invoke(remoteCache);
            } catch (InvocationTargetException ex) {
                String exception = ex.getTargetException().getMessage();
                Assert.assertTrue(exception.equals(UnsupportedOperationException.class.getName()));
                return;
            }
        }

        Map<String, String> bulk = (Map<String, String>) getBulkMethod.invoke(remoteCache);
        IntStream.rangeClosed(1, TOTAL_ENTRIES).forEach(i -> {
            assertTrue(bulk.containsKey("key" + i));
            assertTrue(bulk.containsValue("value" + i));
        });

        Assert.assertEquals(TOTAL_ENTRIES, bulk.size());
        Method getBulkMethodWithSize = remoteCache.getClass().getDeclaredMethod("getBulk", int.class);
        Map<String, String> bulk2 = (Map<String, String>) getBulkMethodWithSize.invoke(remoteCache, 2);
        Assert.assertEquals(2, bulk2.size());
    }

    private int getHotrodClientVersion() {
        String fullVersion = RemoteCache.class.getPackage().getImplementationVersion();
        return Integer.parseInt(fullVersion.substring(0, fullVersion.indexOf(".")));
    }

    private RemoteCache<String, String> getRemoteCache() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer().host("localhost").port(11222);
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(builder.build());
        return remoteCacheManager.administration().getOrCreateCache("default", new org.infinispan.configuration.cache.ConfigurationBuilder().build());
    }
}
