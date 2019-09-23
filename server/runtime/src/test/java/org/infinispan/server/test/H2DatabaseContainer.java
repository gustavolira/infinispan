package org.infinispan.server.test;

import java.util.concurrent.Future;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class H2DatabaseContainer extends JdbcDatabaseContainer {

   public H2DatabaseContainer(Future image) {
      super(image);
   }

   @Override
   public String getDriverClassName() {
      return "org.h2.Driver";
   }

   @Override
   public String getJdbcUrl() {
      return "jdbc:h2:mem:DB_CLOSE_DELAY=-1";
   }

   @Override
   public String getUsername() {
      return "test";
   }

   @Override
   public String getPassword() {
      return "test";
   }

   @Override
   protected String getTestQueryString() {
      return "SELECT 1";
   }
}
