/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.infinispan.server.endpoint.subsystem;

import static org.infinispan.server.endpoint.subsystem.PrefixResource.PREFIX_ATTRIBUTES;
import static org.infinispan.server.endpoint.subsystem.SniResource.SNI_ATTRIBUTES;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The XML writer for the endpoint subsystem configuration.
 *
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @author <a href="http://www.dataforte.net/blog/">Tristan Tarrant</a>
 *
 */
class EndpointSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

   EndpointSubsystemWriter() {
   }

   @Override
   public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
         throws XMLStreamException {
      context.startSubsystemElement(EndpointSchema.CURRENT.getNamespaceUri(), false);
      final ModelNode node = context.getModelNode();
      writeConnectors(writer, node);
      writer.writeEndElement();
   }

   private void writeConnectors(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
      for (Property property : getConnectorsByType(node, ModelKeys.HOTROD_CONNECTOR)) {
         writeHotRodConnector(writer, property.getValue());
      }
      for (Property property : getConnectorsByType(node, ModelKeys.MEMCACHED_CONNECTOR)) {
         writeMemcachedConnector(writer, property.getValue());
      }
      for (Property property : getConnectorsByType(node, ModelKeys.REST_CONNECTOR)) {
         writeRestConnector(writer, property.getValue());
      }
      for (Property property : getConnectorsByType(node, ModelKeys.ROUTER_CONNECTOR)) {
         writeRouterConnector(writer, property.getValue());
      }
   }

   private List<Property> getConnectorsByType(final ModelNode node, String connectorType) {
      if (node.hasDefined(connectorType)) {
         ModelNode connectors = node.get(connectorType);
         return connectors.asPropertyList();
      } else {
         return Collections.emptyList();
      }
   }

   private void writeHotRodConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      writer.writeStartElement(Element.HOTROD_CONNECTOR.getLocalName());
      writeCommonConnector(writer, connector);
      writeProtocolServerConnector(writer, connector);
      writeTopologyStateTransfer(writer, connector);
      writeAuthentication(writer, connector);
      writeEncryption(writer, connector);
      writer.writeEndElement();
   }

   private void writeMemcachedConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      writer.writeStartElement(Element.MEMCACHED_CONNECTOR.getLocalName());
      writeCommonConnector(writer, connector);
      writeProtocolServerConnector(writer, connector);
      for (SimpleAttributeDefinition attribute : MemcachedConnectorResource.MEMCACHED_CONNECTOR_ATTRIBUTES) {
         attribute.marshallAsAttribute(connector, true, writer);
      }
      writer.writeEndElement();
   }

   private void writeRestConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      writer.writeStartElement(Element.REST_CONNECTOR.getLocalName());
      writeCommonConnector(writer, connector);
      for (SimpleAttributeDefinition attribute : RestConnectorResource.REST_ATTRIBUTES) {
         attribute.marshallAsAttribute(connector, true, writer);
      }
      writeRestAuthentication(writer, connector);
      writeEncryption(writer, connector);
      writeCorsRules(writer, connector);
      writer.writeEndElement();
   }

   private void writeRouterConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
           throws XMLStreamException {
      writer.writeStartElement(Element.ROUTER_CONNECTOR.getLocalName());
      for (SimpleAttributeDefinition attribute : RouterConnectorResource.ROUTER_CONNECTOR_ATTRIBUTES) {
         attribute.marshallAsAttribute(connector, true, writer);
      }

      if (connector.hasDefined(ModelKeys.SINGLE_PORT)) {
         ModelNode singlePort = connector.get(ModelKeys.SINGLE_PORT, ModelKeys.SINGLE_PORT_NAME);
         writer.writeStartElement(Element.SINGLE_PORT.getLocalName());

         for (SimpleAttributeDefinition attribute : SinglePortResource.SINGLE_PORT_ATTRIBUTES) {
            attribute.marshallAsAttribute(singlePort, true, writer);
         }

         writeSinglePortRest(writer, singlePort);
         writeSinglePortHotRod(writer, singlePort);
         writer.writeEndElement(); //single-port
      }

      if(connector.hasDefined(ModelKeys.MULTI_TENANCY)) {
         writer.writeStartElement(Element.MULTI_TENANCY.getLocalName());
         ModelNode multiTenancy = connector.get(ModelKeys.MULTI_TENANCY, ModelKeys.MULTI_TENANCY_NAME);
         writeMultiTenantHotRod(writer, multiTenancy);
         writeMultiTenantRest(writer, multiTenancy);
         writer.writeEndElement(); //multi-tenancy
      }


      writer.writeEndElement(); //router-connector
   }

   private void writeMultiTenantHotRod(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.HOTROD)) {
         for (ModelNode hotrodNode: parentNode.get(ModelKeys.HOTROD).asList()) {
            writer.writeStartElement(Element.HOTROD.getLocalName());
            for (SimpleAttributeDefinition hotrodAttribute : MultiTenantHotRodResource.ROUTER_HOTROD_ATTRIBUTES) {
               hotrodAttribute.marshallAsAttribute(hotrodNode.get(0), true, writer);
            }
            writeSni(writer, hotrodNode.get(0));
            writer.writeEndElement();
         }
      }
   }

   private void writeSinglePortHotRod(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.HOTROD)) {
         for (ModelNode hotrodNode: parentNode.get(ModelKeys.HOTROD).asList()) {
            writer.writeStartElement(Element.HOTROD.getLocalName());
            for (SimpleAttributeDefinition hotrodAttribute : SinglePortHotRodResource.SINGLE_PORT_HOTROD_ATTRIBUTES) {
               hotrodAttribute.marshallAsAttribute(hotrodNode.get(0), true, writer);
            }
            writer.writeEndElement();
         }
      }
   }

   private void writeMultiTenantRest(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.REST)) {
         for (ModelNode hotrodNode: parentNode.get(ModelKeys.REST).asList()) {
            writer.writeStartElement(Element.REST.getLocalName());
            for (SimpleAttributeDefinition hotrodAttribute : MultiTenantRestResource.ROUTER_REST_ATTRIBUTES) {
               hotrodAttribute.marshallAsAttribute(hotrodNode.get(0), true, writer);
            }
            writePrefix(writer, hotrodNode.get(0));
            writer.writeEndElement();
         }
      }
   }

   private void writeSinglePortRest(XMLExtendedStreamWriter writer, ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.REST)) {
         for (ModelNode hotrodNode: parentNode.get(ModelKeys.REST).asList()) {
            writer.writeStartElement(Element.REST.getLocalName());
            for (SimpleAttributeDefinition hotrodAttribute : SinglePortRestResource.SINGLE_PORT_REST_ATTRIBUTES) {
               hotrodAttribute.marshallAsAttribute(hotrodNode.get(0), true, writer);
            }
            writePrefix(writer, hotrodNode.get(0));
            writer.writeEndElement();
         }
      }
   }

   private void writeCommonConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      for (SimpleAttributeDefinition attribute : CommonConnectorResource.COMMON_CONNECTOR_ATTRIBUTES) {
         attribute.marshallAsAttribute(connector, true, writer);
      }
      SimpleListAttributeDefinition ignoredCaches = CommonConnectorResource.IGNORED_CACHES;
      writeListAsAttribute(writer, ignoredCaches.getXmlName(), connector, ignoredCaches.getName());
   }

   private void writeProtocolServerConnector(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      for (SimpleAttributeDefinition attribute : ProtocolServerConnectorResource.PROTOCOL_SERVICE_ATTRIBUTES) {
         attribute.marshallAsAttribute(connector, true, writer);
      }
   }

   private void writeTopologyStateTransfer(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      if (connector.hasDefined(ModelKeys.TOPOLOGY_STATE_TRANSFER)) {
         ModelNode topologyStateTransfer = connector.get(ModelKeys.TOPOLOGY_STATE_TRANSFER,
               ModelKeys.TOPOLOGY_STATE_TRANSFER_NAME);
         writer.writeStartElement(Element.TOPOLOGY_STATE_TRANSFER.getLocalName());
         for (SimpleAttributeDefinition attribute : TopologyStateTransferResource.TOPOLOGY_ATTRIBUTES) {
            attribute.marshallAsAttribute(topologyStateTransfer, true, writer);
         }
         writer.writeEndElement();
      }
   }

   private void writeSni(final XMLExtendedStreamWriter writer, final ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.SNI)) {
         for (ModelNode mapping: parentNode.get(ModelKeys.SNI).asList()) {
            writer.writeStartElement(Element.SNI.getLocalName());
            for (SimpleAttributeDefinition sniMappingAttribute : SNI_ATTRIBUTES) {
               sniMappingAttribute.marshallAsAttribute(mapping.get(0), true, writer);
            }
            writer.writeEndElement();
         }
      }
   }

   private void writePrefix(final XMLExtendedStreamWriter writer, final ModelNode parentNode) throws XMLStreamException {
      if (parentNode.hasDefined(ModelKeys.PREFIX)) {
         for (ModelNode mapping: parentNode.get(ModelKeys.PREFIX).asList()) {
            writer.writeStartElement(Element.PREFIX.getLocalName());
            for (SimpleAttributeDefinition prefixMappingAttribute : PREFIX_ATTRIBUTES) {
               prefixMappingAttribute.marshallAsAttribute(mapping.get(0), true, writer);
            }
            writer.writeEndElement();
         }
      }
   }

   private void writeAuthentication(final XMLExtendedStreamWriter writer, final ModelNode connector)
           throws XMLStreamException {
      if (connector.hasDefined(ModelKeys.AUTHENTICATION)) {
         ModelNode authentication = connector.get(ModelKeys.AUTHENTICATION, ModelKeys.AUTHENTICATION_NAME);
         writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
         for (SimpleAttributeDefinition attribute : AuthenticationResource.AUTHENTICATION_ATTRIBUTES) {
            attribute.marshallAsAttribute(authentication, true, writer);
         }
         writeSasl(writer, authentication);
         writer.writeEndElement();
      }
   }

   private void writeSasl(final XMLExtendedStreamWriter writer, final ModelNode authentication) throws XMLStreamException {
      if (authentication.hasDefined(ModelKeys.SASL)) {
         ModelNode sasl = authentication.get(ModelKeys.SASL, ModelKeys.SASL_NAME);
         writer.writeStartElement(Element.SASL.getLocalName());
         for (AttributeDefinition attribute : SaslResource.SASL_ATTRIBUTES) {
            if (attribute instanceof SimpleAttributeDefinition) {
               ((SimpleAttributeDefinition)attribute).marshallAsAttribute(sasl, true, writer);
            } else if (attribute instanceof StringListAttributeDefinition) {
               writeListAsAttribute(writer, attribute.getXmlName(), sasl, attribute.getName());
            }
         }
         writePolicy(writer, sasl);
         writeProperties(writer, sasl);
         writer.writeEndElement();
      }
   }

   private void writePolicy(final XMLExtendedStreamWriter writer, final ModelNode sasl) throws XMLStreamException {
      if (sasl.hasDefined(ModelKeys.SASL_POLICY)) {
         ModelNode policy = sasl.get(ModelKeys.SASL_POLICY, ModelKeys.SASL_POLICY_NAME);
         writer.writeStartElement(Element.POLICY.getLocalName());
         SaslPolicyResource.FORWARD_SECRECY.marshallAsElement(policy, writer);
         SaslPolicyResource.NO_ACTIVE.marshallAsElement(policy, writer);
         SaslPolicyResource.NO_ANONYMOUS.marshallAsElement(policy, writer);
         SaslPolicyResource.NO_DICTIONARY.marshallAsElement(policy, writer);
         SaslPolicyResource.NO_PLAIN_TEXT.marshallAsElement(policy, writer);
         SaslPolicyResource.PASS_CREDENTIALS.marshallAsElement(policy, writer);
         writer.writeEndElement();
      }
   }

   private void writeRestAuthentication(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      if (connector.hasDefined(ModelKeys.AUTHENTICATION)) {
         ModelNode authentication = connector.get(ModelKeys.AUTHENTICATION, ModelKeys.AUTHENTICATION_NAME);
         writer.writeStartElement(Element.AUTHENTICATION.getLocalName());
         for (SimpleAttributeDefinition attribute : RestAuthenticationResource.AUTHENTICATION_ATTRIBUTES) {
            attribute.marshallAsAttribute(authentication, true, writer);
         }
         writer.writeEndElement();
      }
   }

   private void writeProperties(XMLExtendedStreamWriter writer, ModelNode sasl) throws XMLStreamException {
      if (sasl.hasDefined(ModelKeys.PROPERTY)) {
         for (Property property: sasl.get(ModelKeys.PROPERTY).asPropertyList()) {
            writer.writeStartElement(Element.PROPERTY.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            Property complexValue = property.getValue().asProperty();
            writer.writeCharacters(complexValue.getValue().asString());
            writer.writeEndElement();
         }
      }
   }

   private void writeEncryption(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      if (connector.hasDefined(ModelKeys.ENCRYPTION)) {
         ModelNode encryption = connector.get(ModelKeys.ENCRYPTION, ModelKeys.ENCRYPTION_NAME);
         writer.writeStartElement(Element.ENCRYPTION.getLocalName());
         for (SimpleAttributeDefinition attribute : EncryptionResource.ENCRYPTION_ATTRIBUTES) {
            attribute.marshallAsAttribute(encryption, true, writer);
         }
         writeSni(writer, encryption);
         writer.writeEndElement();
      }
   }

   private void writeCorsRules(final XMLExtendedStreamWriter writer, final ModelNode connector)
         throws XMLStreamException {
      if (connector.hasDefined(ModelKeys.CORS_RULE)) {
         writer.writeStartElement(Element.CORS_RULES.getLocalName());
         for (ModelNode modelRule : connector.get(ModelKeys.CORS_RULE).asList()) {
            String ruleName = modelRule.keys().iterator().next();
            ModelNode rule = modelRule.get(ruleName);
            writer.writeStartElement(Element.CORS_RULE.getLocalName());
            for (AttributeDefinition attributeDefinition : CorsRuleResource.CORS_RULE_ATTRIBUTES) {
               if (attributeDefinition instanceof SimpleAttributeDefinition) {
                  SimpleAttributeDefinition.class.cast(attributeDefinition).marshallAsAttribute(rule, true, writer);
               }
            }
            for (AttributeDefinition attributeDefinition : CorsRuleResource.CORS_RULE_ATTRIBUTES) {
               if (attributeDefinition instanceof ListAttributeDefinition) {
                  ListAttributeDefinition listAttribute = ListAttributeDefinition.class.cast(attributeDefinition);
                  writeListAsElement(writer, rule, listAttribute.getName());
               }
            }
            writer.writeEndElement();
         }
         writer.writeEndElement();
      }
   }

   private void writeListAsElement(XMLExtendedStreamWriter writer, ModelNode node, String key) throws XMLStreamException {
      if (node.hasDefined(key)) {
         ModelNode list = node.get(key);
         List<ModelNode> modelNodes = list.asList();
         writer.writeStartElement(key);
         String text = modelNodes.stream().map(ModelNode::asString).collect(Collectors.joining(","));
         writer.writeCharacters(text);
         writer.writeEndElement();
      }
   }

   private void writeListAsAttribute(XMLExtendedStreamWriter writer, String attributeName, ModelNode node, String key) throws XMLStreamException {
      if (node.hasDefined(key)) {
         StringBuilder result = new StringBuilder();
         ModelNode list = node.get(key);
         if (list.isDefined() && list.getType() == ModelType.LIST) {
            List<ModelNode> nodeList = list.asList();
            for (int i = 0; i < nodeList.size(); i++) {
               result.append(nodeList.get(i).asString());
               if (i < nodeList.size() - 1) {
                  result.append(" ");
               }
            }
            writer.writeAttribute(attributeName, result.toString());
         }
      }
   }
}
