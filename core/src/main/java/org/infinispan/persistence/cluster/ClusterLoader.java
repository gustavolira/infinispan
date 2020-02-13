package org.infinispan.persistence.cluster;

import static org.infinispan.util.logging.Log.PERSISTENCE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.remote.ClusteredGetCommand;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.util.EnumUtil;
import org.infinispan.configuration.cache.ClusterLoaderConfiguration;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.context.Flag;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.persistence.spi.CacheLoader;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.LocalOnlyCacheLoader;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.impl.MapResponseCollector;
import org.infinispan.util.ByteString;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Cache loader that consults other members in the cluster for values. A <code>remoteCallTimeout</code> property is
 * required, a <code>long</code> that specifies in milliseconds how long to wait for results before returning a null.
 *
 * @author Mircea.Markus@jboss.com
 */
@ConfiguredBy(ClusterLoaderConfiguration.class)
public class ClusterLoader implements CacheLoader, LocalOnlyCacheLoader {
   private static final Log log = LogFactory.getLog(ClusterLoader.class);

   private RpcManager rpcManager;
   private AdvancedCache<?, ?> cache;
   private CommandsFactory commandsFactory;
   private KeyPartitioner keyPartitioner;

   private ClusterLoaderConfiguration configuration;
   private InitializationContext ctx;
   private ByteString cacheName;

   @Override
   public void init(InitializationContext ctx) {
      this.ctx = ctx;
      cache = ctx.getCache().getAdvancedCache();
      commandsFactory = cache.getComponentRegistry().getCommandsFactory();
      cacheName = ByteString.fromString(cache.getName());
      rpcManager = cache.getRpcManager();
      this.configuration = ctx.getConfiguration();
      keyPartitioner = cache.getComponentRegistry().getComponent(KeyPartitioner.class);
   }

   @Override
   public MarshallableEntry loadEntry(Object key) throws PersistenceException {
      if (!isCacheReady()) return null;

      ClusteredGetCommand clusteredGetCommand = commandsFactory.buildClusteredGetCommand(key,
            keyPartitioner.getSegment(key), EnumUtil.bitSetOf(Flag.SKIP_OWNERSHIP_CHECK));

      Collection<Response> responses;
      try {
         clusteredGetCommand.setTopologyId(rpcManager.getTopologyId());
         CompletionStage<Map<Address, Response>> getAll = rpcManager.invokeCommandOnAll(clusteredGetCommand,
               MapResponseCollector.ignoreLeavers(), rpcManager.getSyncRpcOptions());
         responses = rpcManager.blocking(getAll).values();
      } catch (Exception e) {
         PERSISTENCE.errorDoingRemoteCall(e);
         throw new PersistenceException(e);
      }
      if (responses.isEmpty()) return null;

      Response response;
      if (responses.size() > 1) {
         // Remove duplicates before deciding if multiple responses were received
         Set<Response> setResponses = new HashSet<>(responses);
         if (setResponses.size() > 1)
            throw new PersistenceException(String.format(
                  "Responses contains more than 1 element and these elements are not equal, so can't decide which one to use: %s",
                  setResponses));
         response = setResponses.iterator().next();
      } else {
         response = responses.iterator().next();
      }

      if (response.isSuccessful() && response instanceof SuccessfulResponse) {
         InternalCacheValue value = (InternalCacheValue) ((SuccessfulResponse) response).getResponseValue();
         return value == null ? null :
               ctx.getMarshallableEntryFactory().create(key, value.getValue());
      }

      PERSISTENCE.unknownResponsesFromRemoteCache(responses);
      throw new PersistenceException("Unknown responses");
   }


   @Override
   public boolean contains(Object key) {
      return loadEntry(key) != null;
   }

   @Override
   public void start() {
      //nothing to do here
   }

   @Override
   public void stop() {
      //nothing to do here
   }

   /**
    * A test to check whether the cache is in its started state.  If not, calls should not be made as the channel may
    * not have properly started, blocks due to state transfers may be in progress, etc.
    *
    * @return true if the cache is in its STARTED state.
    */
   protected boolean isCacheReady() {
      return cache.getStatus() == ComponentStatus.RUNNING;
   }
}
