package org.infinispan.server.test.persistence;

import java.io.InputStream;
import java.util.Properties;

import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.persistence.jdbc.DatabaseType;
import org.infinispan.server.test.InfinispanServerRule;
import org.infinispan.server.test.InfinispanServerTestMethodRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class DatabaseServerRule extends InfinispanServerTestMethodRule {

   public static final String DATABASE = "org.infinispan.test.server.jdbc.database";
   public static final String IMAGE_TAG = "org.infinispan.test.server.jdbc.image.tag";
   public DatabaseInfo databaseInfo;
   public Properties dbProperties;
   private JdbcDatabaseContainer container;
   private InfinispanServerRule infinispanServerRule;

   public DatabaseServerRule(InfinispanServerRule infinispanServerRule) {
      super(infinispanServerRule);
      this.infinispanServerRule = infinispanServerRule;
   }

   @Override
   public Statement apply(Statement base, Description description) {
      return new Statement() {
         @Override
         public void evaluate() throws Throwable {
            before();
            try {
               base.evaluate();
            } finally {
               after();
            }
         }


      };
   }

   private void before() {

      if(infinispanServerRule.getServerDriver().getStatus() != ComponentStatus.RUNNING) {
         throw new IllegalStateException("Infinispan Server should be running");
      }

      String database = System.getProperty(DATABASE, "h2");
      String imageTag = System.getProperty(IMAGE_TAG, "latest");
      String databaseProperties = String.format("/configuration/datasource/%s.properties", database);
      dbProperties = new Properties();

      try(InputStream inputStream= getClass().getResourceAsStream(databaseProperties);) {
         dbProperties.load(inputStream);
         DatabaseType databaseType = DatabaseType.valueOf(database.toUpperCase());
         container = JdbcDatabaseContainerFactory.get(databaseType, imageTag);
         container.waitingFor(Wait.forListeningPort());

         databaseInfo = getDatabaseInformations();

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private DatabaseInfo getDatabaseInformations() {
      if(ExternalDatabase.isExternalDatabase()) {
         return new ExternalDatabase(dbProperties);
      } else {
         container.start();
         return new ContainerDatabase(container, dbProperties);
      }

   }

   private void after() {
      container.stop();
   }

}
