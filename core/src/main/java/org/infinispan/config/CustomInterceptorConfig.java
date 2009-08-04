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

import java.util.Locale;
import java.util.Properties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import net.jcip.annotations.Immutable;

import org.infinispan.config.ConfigurationElement.Cardinality;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.util.TypedProperties;

/**
 * Holds information about the custom interceptors defined in the configuration file.
 *
 * <p>
 * Note that class CustomInterceptorConfig contains JAXB annotations. These annotations determine how XML
 * configuration files are read into instances of configuration class hierarchy as well as they
 * provide meta data for configuration file XML schema generation. Please modify these annotations
 * and Java element types they annotate with utmost understanding and care.
 *
 * @author Mircea.Markus@jboss.com
 * @author Vladimir Blagojevic
 * @since 4.0
 */
@Immutable
@ConfigurationElement(name = "interceptor", parent = "customInterceptors" ,
         cardinalityInParent=Cardinality.UNBOUNDED)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="interceptor")
public class CustomInterceptorConfig extends AbstractNamedCacheConfigurationBean {
   
   @XmlTransient
   private CommandInterceptor interceptor;
   
   @XmlTransient
   private boolean isFirst;
   
   @XmlTransient
   private boolean isLast;
   
   @XmlAttribute
   private Integer index = -1;
   
   @XmlAttribute
   private String after;
   
   @XmlAttribute
   private String before;
   
   @XmlAttribute
   private Position position;   
   
   @XmlAttribute(name="class")
   private String className;
   
   @XmlElement
   private TypedProperties properties = EMPTY_PROPERTIES;

   public CustomInterceptorConfig() {
      super();
   }

   /**
    * Builds a custom interceptor configuration.
    *
    * @param interceptor interceptor instance, already initialized with all attributes specified in the configuration
    * @param first       true if you wan this to be the first interceptor in the chain
    * @param last        true if you wan this to be the last interceptor in the chain
    * @param index       an absolute position within the interceptor chain
    * @param after       if you want this interceptor immediately after the specified class in the chain
    * @param before      immediately before the specified class in the chain
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor, boolean first, boolean last, int index,
                                  String after, String before) {
      this.interceptor = interceptor;
      isFirst = first;
      isLast = last;
      this.index = index;
      this.after = after;
      this.before = before;
   }

   /**
    * Builds a custom interceptor configuration.
    *
    * @param interceptor interceptor instance, already initialized with all attributes specified in the configuration
    * @param first       true if you wan this to be the first interceptor in the chain
    * @param last        true if you wan this to be the last interceptor in the chain
    * @param index       an absolute position within the interceptor chain
    * @param after       if you want this interceptor immediately after the specified class in the chain
    * @param before      immediately before the specified class in the chain
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor, boolean first, boolean last, int index,
                                  Class<? extends CommandInterceptor> after, Class<? extends CommandInterceptor> before) {
      this.interceptor = interceptor;
      isFirst = first;
      isLast = last;
      this.index = index;
      this.after = after == null ? null : after.getName();
      this.before = before == null ? null : before.getName();
   }

   /**
    * Constructs an interceptor config based on the supplied interceptor instance.
    *
    * @param interceptor
    */
   public CustomInterceptorConfig(CommandInterceptor interceptor) {
      this.interceptor = interceptor;
   }
   
   public Properties getProperties() {
      return properties;
   }
   
   @ConfigurationProperty(name = "anyCustomProperty", parentElement = "interceptor")  
   public void setProperties(Properties properties) {
      this.properties = toTypedProperties(properties);
   }

   public Position getPosition() {
      return position;
   }

   public void setPosition(Position position) {
      this.position = position;
   }

   public String getClassName() {
      return className;
   }
   
   @ConfigurationAttribute(name = "class", 
            containingElement = "interceptor") 
   public void setClassName(String className) {
      this.className = className;
   }

   /**
    * Shall this interceptor be the first one in the chain?
    */

   public void setFirst(boolean first) {
      testImmutability("first");
      isFirst = first;
   }

   /**
    * Shall this intercepto be the last one in the chain?
    */
   public void setLast(boolean last) {
      testImmutability("last");
      isLast = last;
   }
   
   @ConfigurationAttribute(name = "position", 
            containingElement = "interceptor") 
   public void setPosition(String pos) {
      setPosition(Position.valueOf(uc(pos)));
   }

   /**
    * Put this interceptor at the specified index, after the default chain is built. If the index is not valid (negative
    * or grater than the size of the chain) an {@link ConfigurationException} is thrown at construction time.
    */
   @ConfigurationAttribute(name = "index", 
            containingElement = "interceptor") 
   public void setIndex(int index) {
      testImmutability("index");
      this.index = index;
   }

   /**
    * Adds the interceptor immediately after the first occurance of an interceptor having the given class. If the chain
    * does not contain such an interceptor then this interceptor definition is ignored.
    */
   @ConfigurationAttribute(name = "after", 
            containingElement = "interceptor") 
   public void setAfterInterceptor(String afterClass) {
      testImmutability("after");
      this.after = afterClass;
   }

   /**
    * Adds the interceptor immediately after the first occurance of an interceptor having the given class. If the chain
    * does not contain such an interceptor then this interceptor definition is ignored.
    */
   public void setAfterInterceptor(Class<? extends CommandInterceptor> interceptorClass) {
      setAfterInterceptor(interceptorClass.getName());
   }

   /**
    * Adds the interceptor immediately before the first occurance of an interceptor having the given class. If the chain
    * does not contain such an interceptor then this interceptor definition is ignored.
    */
   @ConfigurationAttribute(name = "before", 
            containingElement = "interceptor") 
   public void setBeforeInterceptor(String beforeClass) {
      testImmutability("before");
      this.before = beforeClass;
   }

   /**
    * Adds the interceptor immediately before the first occurance of an interceptor having the given class. If the chain
    * does not contain such an interceptor then this interceptor definition is ignored.
    */
   public void setBeforeInterceptor(Class<? extends CommandInterceptor> interceptorClass) {
      setBeforeInterceptor(interceptorClass.getName());
   }

   /**
    * Returns a the interceptor that we want to add to the chain.
    */
   public CommandInterceptor getInterceptor() {      
      return interceptor;
   }
   
   /**
    * Returns a the interceptor that we want to add to the chain.
    */
   public void setInterceptor(CommandInterceptor interceptor) {      
      this.interceptor = interceptor;
   }

   /**
    * @see #setFirst(boolean)
    */
   public boolean isFirst() {
      return isFirst;
   }

   /**
    * @see #setLast(boolean)
    */
   public boolean isLast() {
      return isLast;
   }

   /**
    * @see #getIndex()
    */
   public int getIndex() {
      return index;
   }

   /**
    * @see #getAfter()
    */
   public String getAfter() {
      return after;
   }

   /**
    * @see #getBefore()
    */
   public String getBefore() {
      return before;
   }

   public String toString() {
      return "CustomInterceptorConfig{" +
            "interceptor='" + interceptor + '\'' +
            ", isFirst=" + isFirst +
            ", isLast=" + isLast +
            ", index=" + index +
            ", after='" + after + '\'' +
            ", before='" + before + '\'' +
            '}';
   }

   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CustomInterceptorConfig)) return false;

      CustomInterceptorConfig that = (CustomInterceptorConfig) o;

      if (index != that.index) return false;
      if (isFirst != that.isFirst) return false;
      if (isLast != that.isLast) return false;
      if (after != null ? !after.equals(that.after) : that.after != null) return false;
      if (before != null ? !before.equals(that.before) : that.before != null) return false;
      if (interceptor != null ? !interceptor.equals(that.interceptor) : that.interceptor != null)
         return false;
      return true;
   }

   public int hashCode() {
      int result;
      result = (interceptor != null ? interceptor.hashCode() : 0);
      result = 31 * result + (isFirst ? 1 : 0);
      result = 31 * result + (isLast ? 1 : 0);
      result = 31 * result + index;
      result = 31 * result + (after != null ? after.hashCode() : 0);
      result = 31 * result + (before != null ? before.hashCode() : 0);
      return result;
   }

   @Override
   public CustomInterceptorConfig clone() throws CloneNotSupportedException {
      CustomInterceptorConfig dolly = (CustomInterceptorConfig) super.clone();
      dolly.interceptor = interceptor;
      dolly.isFirst = isFirst;
      dolly.isLast = isLast;
      dolly.after = after;
      dolly.before = before;
      return dolly;
   }
   
   protected String uc(String s) {
      return s == null ? null : s.toUpperCase(Locale.ENGLISH);
   }
   
   enum Position {
      FIRST,LAST;
   }
}
