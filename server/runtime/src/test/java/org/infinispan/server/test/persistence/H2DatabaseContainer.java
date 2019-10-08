package org.infinispan.server.test.persistence;

import java.util.concurrent.Future;

import org.testcontainers.containers.JdbcDatabaseContainer;

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

   @Override
   protected void waitUntilContainerStarted() {
      waitStrategy.waitUntilReady(this);
   }

//   @Override
//   public Driver getJdbcDriverInstance() throws JdbcDatabaseContainer.NoDriverFoundException {
//      try {
//         return (Driver) Class.forName(this.getDriverClassName()).newInstance();
////         return DriverManager.getDriver(getJdbcUrl());
//      } catch (Exception e) {
//         e.printStackTrace();
//      }
//
//      //DriverManager.getDriver()
//      //return driver;
//      return null;
//   }
//   @Override
//   public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
//      return null;
//
//   }

}
