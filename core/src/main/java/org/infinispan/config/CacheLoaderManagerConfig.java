/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.config;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.infinispan.config.parsing.CacheLoaderManagerConfigReader;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.util.Util;

/**
 * Holds the configuration of the cache loader chain.  ALL cache loaders should be defined using this class, adding
 * individual cache loaders to the chain by calling {@link CacheLoaderManagerConfig#addCacheLoaderConfig}
 * 
 * <p>
 * Note that class CacheLoaderManagerConfig contains JAXB annotations. These annotations determine how XML
 * configuration files are read into instances of configuration class hierarchy as well as they
 * provide meta data for configuration file XML schema generation. Please modify these annotations
 * and Java element types they annotate with utmost understanding and care.
 *
 * @author <a href="mailto:manik@jboss.org">Manik Surtani (manik@jboss.org)</a>
 * @author Brian Stansberry
 * @author Vladimir Blagojevic 
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @since 4.0
 */
@ConfigurationElement(name="loaders",parent="default",customReader=CacheLoaderManagerConfigReader.class)
@XmlAccessorType(XmlAccessType.FIELD)
public class CacheLoaderManagerConfig extends AbstractNamedCacheConfigurationBean {
   private static final long serialVersionUID = 2210349340378984424L;

   @XmlAttribute
   private Boolean passivation = false;
   
   @XmlAttribute
   private Boolean preload = false;
   
   @XmlAttribute
   private Boolean shared = false;
  
   @XmlElement(name="loader")
   private List<CacheLoaderConfig> cacheLoaderConfigs = new LinkedList<CacheLoaderConfig>();



   @ConfigurationAttribute(name = "preload", 
            containingElement = "loaders")
   public void setPreload(boolean preload) {
      testImmutability("preload");
      this.preload = preload;
   }


   @ConfigurationAttribute(name = "passivation", 
            containingElement = "loaders")
   public void setPassivation(boolean passivation) {
      testImmutability("passivation");
      this.passivation = passivation;
   }

   public boolean isPassivation() {
      return passivation;
   }

   public void addCacheLoaderConfig(CacheLoaderConfig clc) {
      testImmutability("cacheLoaderConfigs");
      cacheLoaderConfigs.add(clc);
   }

   public List<CacheLoaderConfig> getCacheLoaderConfigs() {
      return cacheLoaderConfigs;
   }

   public void setCacheLoaderConfigs(List<CacheLoaderConfig> configs) {
      testImmutability("cacheLoaderConfigs");
      this.cacheLoaderConfigs = configs == null ? new LinkedList<CacheLoaderConfig>() : configs;
   }

   public CacheLoaderConfig getFirstCacheLoaderConfig() {
      if (cacheLoaderConfigs.size() == 0) return null;
      return cacheLoaderConfigs.get(0);
   }

   public boolean useChainingCacheLoader() {
      return !isPassivation() && cacheLoaderConfigs.size() > 1;
   }

   @Override
   public String toString() {
      return new StringBuilder().append("CacheLoaderManagerConfig{").append("shared=").append(
               shared).append(", passivation=").append(passivation).append(", preload='").append(
               preload).append('\'').append(", cacheLoaderConfigs.size()=").append(
               cacheLoaderConfigs.size()).append('}').toString();
   }


   @ConfigurationAttribute(name = "shared", 
            containingElement = "loaders")
   public void setShared(boolean shared) {
      testImmutability("shared");
      this.shared = shared;
   }

   public boolean isShared() {
      return shared;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;

      if (obj instanceof CacheLoaderManagerConfig) {
         CacheLoaderManagerConfig other = (CacheLoaderManagerConfig) obj;
         return (this.passivation == other.passivation)
               && (this.shared == other.shared)
               && Util.safeEquals(this.preload, other.preload)
               && Util.safeEquals(this.cacheLoaderConfigs, other.cacheLoaderConfigs);
      }
      return false;
   }

   @Override
   public int hashCode() {
      int result = 19;
      result = 51 * result + (passivation ? 0 : 1);
      result = 51 * result + (shared ? 0 : 1);
      result = 51 * result + (preload ? 0 : 1);
      result = 51 * result + (cacheLoaderConfigs == null ? 0 : cacheLoaderConfigs.hashCode());
      return result;
   }


   @Override
   public CacheLoaderManagerConfig clone() throws CloneNotSupportedException {
      CacheLoaderManagerConfig clone = (CacheLoaderManagerConfig) super.clone();
      if (cacheLoaderConfigs != null) {
         List<CacheLoaderConfig> clcs = new LinkedList<CacheLoaderConfig>();
         for (CacheLoaderConfig clc : cacheLoaderConfigs) {
            clcs.add(clc.clone());
         }
         clone.setCacheLoaderConfigs(clcs);
      }
      return clone;
   }

   /**
    * Loops through all individual cache loader configs and checks if fetchPersistentState is set on any of them
    */
   public boolean isFetchPersistentState() {
      for (CacheLoaderConfig iclc : cacheLoaderConfigs) {
         if (iclc instanceof CacheStoreConfig)
            if (((CacheStoreConfig) iclc).isFetchPersistentState()) return true;
      }
      return false;
   }

   public boolean isPreload() {
      return preload;
   }
}
