package org.infinispan.server.test.persistence;

import org.infinispan.persistence.jdbc.DatabaseType;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * @author Gustavo Lira &lt;glira@redhat.com&gt;
 * @since 10.0
 **/
public class JdbcDatabaseContainerFactory {

   protected static final String MYSQL_IMAGE = "mysql";
   protected static final String POSTGRES_IMAGE = "postgres";
   protected static final String MARIADB_IMAGE = "mariadb";
   protected static final String DB2_IMAGE ="ibmcom/db2";
   protected static final String MS_SQL_IMAGE = "mcr.microsoft.com/mssql/server";
   protected static final String ORACLE_XE_IMAGE = "christophesurmont/oracle-xe-11g";  //oracle 11g
   protected static final String H2_IMAGE = "oscarfonts/h2";

   public static JdbcDatabaseContainer get(DatabaseType database, String version) {

      switch (database) {
         case MYSQL:
            return new MySQLContainer(getContainerImage(MYSQL_IMAGE, version));
         case POSTGRES:
            return new PostgreSQLContainer(getContainerImage(POSTGRES_IMAGE, version));
         case DB2:
            return new Db2Container(getContainerImage(DB2_IMAGE, version)).acceptLicense();
         case MARIADB:
            return new MariaDBContainer(getContainerImage(MARIADB_IMAGE, version));
         case SQL_SERVER:
            return new MSSQLServerContainer(getContainerImage(MS_SQL_IMAGE, version));
         case ORACLE_XE:
            return new OracleContainer(getContainerImage(ORACLE_XE_IMAGE, version));
         default:
            return getH2Container();
      }
   }

   private static JdbcDatabaseContainer getH2Container() {
      ImageFromDockerfile image = new ImageFromDockerfile()
            .withDockerfileFromBuilder(builder-> {
               builder
                     .from(H2_IMAGE)
                     .expose(1521)
                     .build();
            });
      return new H2DatabaseContainer(image);
   }

   private static String getContainerImage(String database, String version) {
      return String.format("%s:%s", database, version);
   }
}
