package org.infinispan.server.test;

import java.io.InputStream;
import java.util.Properties;

import org.infinispan.persistence.jdbc.DatabaseType;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class DatabaseServerRule extends InfinispanServerTestMethodRule {

   public static final String DATABASE = "org.infinispan.test.server.jdbc.database";
   public static final String IMAGE_TAG = "org.infinispan.test.server.jdbc.image.tag";

   public Properties dbProperties;
   public JdbcDatabaseContainer container;

   public DatabaseServerRule(InfinispanServerRule infinispanServerRule) {
      super(infinispanServerRule);
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

      String database = System.getProperty(DATABASE, "h2");
      String imageTag = System.getProperty(IMAGE_TAG, "latest");
      String databaseProperties = String.format("/configuration/datasource/%s.properties", database);
      dbProperties = new Properties();

      try(InputStream inputStream= getClass().getResourceAsStream(databaseProperties);) {
         dbProperties.load(inputStream);
         DatabaseType databaseType = DatabaseType.valueOf(database.toUpperCase());
         container = JdbcDatabaseContainerFactory.get(databaseType, imageTag);
         container.start();

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void after() {
      container.stop();
   }

   public String getDataColumType() { return dbProperties.getProperty("data.column.type"); }

   public String getTimeStampColumType() { return dbProperties.getProperty("timestamp.column.type"); }

   public String getIdColumType() { return dbProperties.getProperty("id.column.type"); }

}
