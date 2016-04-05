/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.avlist.AVList;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * @author tag
 * @version $Id: XMLEventParserContext.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public interface XMLEventParserContext extends AVList {
	/**
	 * Identifies the name of the parser handling unrecognized elements. Can be used to explicitly specify the context's
	 * parser-table entry for unrecognized elements.
	 */
	final static String UNRECOGNIZED_ELEMENT_PARSER = "gov.nasa.worldwind.util.xml.UnknownElementParser";

	/**
	 * Returns the event reader associated with the context.
	 * 
	 * @return the associated event reader, or null if no reader is associated.
	 */
	XMLEventReader getEventReader();

	/**
	 * Returns a new parser for a specified event.
	 * 
	 * @param event
	 *            indicates the element name for which a parser is created.
	 * @return the new parser, or null if no parser has been registered for the specified event's element name.
	 */
	XMLEventParser getParser(XMLEvent event);

	/**
	 * Returns a new parser for a specified element name.
	 * 
	 * @param eventName
	 *            indicates the element name for which a parser is created.
	 * @return the new parser, or null if no parser has been registered for the specified element name.
	 */
	XMLEventParser getParser(QName eventName);

	/**
	 * Determines whether an event is a start event for a specific event type.
	 * 
	 * @param event
	 *            an event identifying the event type of interest.
	 * @param elementName
	 *            the event name.
	 * @return true if the event is a start event for the named event type.
	 */
	boolean isStartElement(XMLEvent event, QName elementName);

	/**
	 * Determines whether an event is the corresponding end element for a specified start event.
	 * <p/>
	 * Note: Only the event's element name and type are compared. The method returns true if the start and end events are the corresponding event types for an
	 * element of the same name.
	 * 
	 * @param event
	 *            the event of interest.
	 * @param startElement
	 *            the start event associated with the potential end event.
	 * @return true if the event is the corresponding end event to the specified start event, otherwise false.
	 */
	boolean isEndElement(XMLEvent event, XMLEvent startElement);

	/**
	 * Returns the text associated with the event.
	 * 
	 * @param event
	 *            the event of interest.
	 * @return the event's characters, or null if the event is not a character event.
	 */
	String getCharacters(XMLEvent event);

	/**
	 * Returns the default parser for a simple string.
	 * 
	 * @return a string parser.
	 */
	StringXMLEventParser getStringParser();

	/**
	 * Returns a parser for a simple double.
	 * 
	 * @return a double parser.
	 */
	DoubleXMLEventParser getDoubleParser();

	/**
	 * Returns the default parser for a simple boolean.
	 * 
	 * @return a boolean parser.
	 */
	BooleanXMLEventParser getBooleanParser();

	/**
	 * Returns the default parser for a simple boolean integer (0 or 1).
	 * 
	 * @return a boolean integer parser.
	 */
	BooleanIntegerXMLEventParser getBooleanIntegerParser();

	/**
	 * Returns the default parser for a simple integer.
	 * 
	 * @return an integer parser.
	 */
	IntegerXMLEventParser getIntegerParser();

	/**
	 * Registers a parser for a specified element name.
	 * 
	 * @param elementName
	 *            the element name for which to return a parser.
	 * @param parser
	 *            the parser to register.
	 */
	void registerParser(QName elementName, XMLEventParser parser);

	/**
	 * Returns the next event in the event stream associated with this context.
	 * 
	 * @return the next event,
	 * @throws XMLParserException
	 *             if a parsing exception occurs.
	 */
	XMLEvent nextEvent() throws XMLParserException;

	/**
	 * Returns the context's default namespace URI.
	 * 
	 * @return the context's default namespace URI.
	 * @see #setDefaultNamespaceURI(String)
	 */
	String getDefaultNamespaceURI();

	/**
	 * Specifies the context's default namespace URI. Must be called prior to initiating the parser table if this
	 * context's parsers will be qualified for the default namespace.
	 * 
	 * @param defaultNamespaceURI
	 *            the default namespace URI.
	 * @see #getDefaultNamespaceURI()
	 * @see #isSameName(QName, QName)
	 */
	void setDefaultNamespaceURI(String defaultNamespaceURI);

	/**
	 * Determines whether two element names are the same.
	 * 
	 * @param qa
	 *            first element name
	 * @param qb
	 *            second element name
	 * @return true if both names have the same namespace (or no namespace) and local name, or if either name has no
	 *         namespace but the namespace of the other is the context's default namespace.
	 */
	boolean isSameName(QName qa, QName qb);

	/**
	 * Create a parser for a specified event's element name, if a parser for that name is registered with the context.
	 * 
	 * @param event
	 *            the event whose element name identifies the parser to create.
	 * @param defaultParser
	 *            a parser to return if no parser is registered for the specified name. May be null.
	 * @return a new parser, or the specified default parser if no parser has been registered for the element name.
	 */
	XMLEventParser allocate(XMLEvent event, XMLEventParser defaultParser);

	/**
	 * Create a parser for a specified event's element name, if a parser for that name is registered with the context.
	 * 
	 * @param event
	 *            the event whose element name identifies the parser to create.
	 * @return a new parser, or the specified default parser if no parser has been registered for the element name.
	 */
	XMLEventParser allocate(XMLEvent event);

	/**
	 * Determines whether two fully qualified attribute names are the same.
	 * 
	 * @param qa
	 *            the first attribute name.
	 * @param qb
	 *            the second attribute name.
	 * @return true if the names are the same, otherwise false.
	 */
	boolean isSameAttributeName(QName qa, QName qb);

	/**
	 * Returns the table associating objects with their <i>id</i> attribute as specified in the object's KML file.
	 * 
	 * @return the mapping table.
	 */
	Map<String, Object> getIdTable();

	/**
	 * Adds a mapping of an <i>id</i> attribute to its associated KML object.
	 * 
	 * @param id
	 *            the object id. If null, this method returns without creating a mapping.
	 * @param o
	 *            the object to associate with the id.
	 */
	void addId(String id, Object o);

	/**
	 * Resolves references to elements in the same KML file. Certain KML elements such as <i>styleUrl</i> may contain
	 * references to other elements within the same KML file. (A leading "#" indicates a reference to an element within
	 * the same file.) This method searches the elements in the file for these references and adds the object for the
	 * element they refer to the referring object's field table.
	 * 
	 * @param referenceName
	 *            the element name of the elements whose references this method resolves. An example is
	 *            <i>styleUrl</i>. Resolution is performed for only elements of this name.
	 * @param fieldName
	 *            the key used to identify the resolved object in a parser's field table. After this method
	 *            resolves references, the referenced object can be obtained by calling the parsers
	 *            {@link AbstractXMLEventParser#getField(QName)} method with the <code>fieldName</code>
	 *            specified here as the name argument.
	 * @param parser
	 *            the parser whose references to resolve.
	 */
	@Deprecated
	void resolveInternalReferences(String referenceName, String fieldName, AbstractXMLEventParser parser);

	/**
	 * Specify the object to receive notifications, which are sent when exceptions occur during parsing and when
	 * unrecognized element types are encountered. See {@link gov.nasa.worldwind.util.xml.XMLParserNotification} for
	 * more information.
	 * <p/>
	 * The parser context may have only one notification listener. That listener may be changed at any time.
	 * 
	 * @param listener
	 *            the object to receive notification events.
	 */
	void setNotificationListener(XMLParserNotificationListener listener);

	/**
	 * Indicates whether the specified namespace URI is the default namespace URI used by this parser context.
	 * 
	 * @param namespaceURI
	 *            the namespace URI to check.
	 * @return true if the specified namespace is the default namespace, otherwise false.
	 */
	boolean isDefaultNamespace(String namespaceURI);

	/**
	 * Returns a parser to handle unrecognized elements. The default unrecognized event parser is {@link UnrecognizedXMLEventParser}
	 * .
	 * 
	 * @return a parser to handle unrecognized elements.
	 */
	XMLEventParser getUnrecognizedElementParser();
}
