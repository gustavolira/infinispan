package org.infinispan.server.functional;

import org.infinispan.server.test.persistence.DatabaseServerRule;
import org.infinispan.server.test.InfinispanServerRule;
import org.infinispan.server.test.InfinispanServerTestConfiguration;
import org.infinispan.server.test.ServerRunMode;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
@RunWith(Suite.class)
@Suite.SuiteClasses({
        PooledConnectionOperations.class
})
public class PersistenceIT {

   protected static final Integer NUM_SERVERS = 1;

   @ClassRule
   public static InfinispanServerRule SERVERS = new InfinispanServerRule(new InfinispanServerTestConfiguration("configuration/ClusteredServerTest.xml")
           .numServers(NUM_SERVERS).runMode(ServerRunMode.CONTAINER).artifacts("com.h2database:h2:1.4.199"));

   @ClassRule
   public static DatabaseServerRule DATABASE = new DatabaseServerRule(SERVERS);

}

