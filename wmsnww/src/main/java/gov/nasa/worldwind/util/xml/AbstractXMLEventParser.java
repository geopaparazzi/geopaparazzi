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
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author tag
 * @version $Id: AbstractXMLEventParser.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class AbstractXMLEventParser implements XMLEventParser {
	protected static final String CHARACTERS_CONTENT = "CharactersContent";

	protected final String namespaceURI;

	protected AVList fields;
	protected XMLEventParser parent;

	/** Construct a parser with no qualifying namespace. */
	public AbstractXMLEventParser() {
		this.namespaceURI = null;
	}

	/**
	 * Constructs a parser and qualifies it for a specified namespace.
	 * 
	 * @param namespaceURI
	 *            the qualifying namespace URI. May be null to indicate no namespace qualification.
	 */
	public AbstractXMLEventParser(String namespaceURI) {
		this.namespaceURI = namespaceURI;
	}

	/**
	 * Returns the qualifying namespace URI specified at construction.
	 * 
	 * @return the namespace URI. Returns null if no name space was specified at construction.
	 */
	public String getNamespaceURI() {
		return this.namespaceURI;
	}

	public XMLEventParser newInstance() throws Exception {
		Constructor<? extends AbstractXMLEventParser> constructor = this.getAConstructor(String.class);
		if (constructor != null) return constructor.newInstance(this.getNamespaceURI());

		constructor = this.getAConstructor();
		if (constructor != null) return constructor.newInstance();

		return null;
	}

	public void setField(QName keyName, Object value) {
		this.setField(keyName.getLocalPart(), value);
	}

	public void setField(String keyName, Object value) {
		if (this.fields == null) this.fields = new AVListImpl();

		this.fields.setValue(keyName, value);
	}

	public void setFields(Map<String, Object> newFields) {
		if (this.fields == null) this.fields = new AVListImpl();

		for (Map.Entry<String, Object> nf : newFields.entrySet()) {
			this.setField(nf.getKey(), nf.getValue());
		}
	}

	public Object getField(QName keyName) {
		return this.fields != null ? this.getField(keyName.getLocalPart()) : null;
	}

	public Object getField(String keyName) {
		return this.fields != null ? this.fields.getValue(keyName) : null;
	}

	public boolean hasField(QName keyName) {
		return this.hasField(keyName.getLocalPart());
	}

	public boolean hasField(String keyName) {
		return this.fields != null && this.fields.hasKey(keyName);
	}

	public void removeField(String keyName) {
		if (this.fields != null) this.fields.removeKey(keyName);
	}

	public boolean hasFields() {
		return this.fields != null;
	}

	public AVList getFields() {
		return this.fields;
	}

	protected AbstractXMLEventParser mergeFields(AbstractXMLEventParser s1, AbstractXMLEventParser s2) {
		for (Map.Entry<String, Object> entry : s2.getFields().getEntries()) {
			if (!s1.hasField(entry.getKey())) s1.setField(entry.getKey(), entry.getValue());
		}

		return this;
	}

	protected AbstractXMLEventParser overrideFields(AbstractXMLEventParser s1, AbstractXMLEventParser s2) {
		if (s2.getFields() != null) {
			for (Map.Entry<String, Object> entry : s2.getFields().getEntries()) {
				s1.setField(entry.getKey(), entry.getValue());
			}
		}

		return this;
	}

	public XMLEventParser getParent() {
		return this.parent;
	}

	public void setParent(XMLEventParser parent) {
		this.parent = parent;
	}

	public void freeResources() {
		// Override in subclass to free any large resources.
	}

	protected Constructor<? extends AbstractXMLEventParser> getAConstructor(Class<?>... parameterTypes) {
		try {
			return this.getClass().getConstructor(parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public XMLEventParser getRoot() {
		XMLEventParser parser = this;

		while (true) {
			XMLEventParser parent = parser.getParent();
			if (parent == null) return parser;
			parser = parent;
		}
	}

	/**
	 * Create a parser for a specified event.
	 * 
	 * @param ctx
	 *            the current parser context.
	 * @param event
	 *            the event for which the parser is created. Only the event type is used; the new parser can operate
	 *            on any event of that type.
	 * @return the new parser.
	 */
	public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event) {
		if (ctx == null) {
			String message = Logging.getMessage("nullValue.ParserContextIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		XMLEventParser parser = ctx.allocate(event);
		if (parser != null) parser.setParent(this);

		return parser;
	}

	/** {@inheritDoc} */
	public Object parse(XMLEventParserContext ctx, XMLEvent inputEvent, Object... args) throws XMLParserException {
		if (ctx == null) {
			String message = Logging.getMessage("nullValue.ParserContextIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (inputEvent == null) {
			String message = Logging.getMessage("nullValue.EventIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		try {
			// Parse this event's attributes
			this.doParseEventAttributes(ctx, inputEvent, args);

			// Build the symbol table
			String id = (String) this.getField("id");
			if (id != null) ctx.addId(id, this);
		} catch (Exception e) {
			ctx.firePropertyChange(new XMLParserNotification(ctx, XMLParserNotification.EXCEPTION, inputEvent, "XML.ExceptionParsingElement", null, e));
		}
		// XXX: Fix for the unclosed tag. A final Input event that is an holder for the tag parsed at the moment
		final XMLEvent fixedInputEvent = doCopyInputEvent(inputEvent);

		// Parse the event's sub-elements.
		XMLEvent event = ctx.nextEvent();
		while (event != null) {
			if (ctx.isEndElement(event, fixedInputEvent)) {
				if (this.hasField(CHARACTERS_CONTENT)) {
					StringBuilder sb = (StringBuilder) this.getField(CHARACTERS_CONTENT);
					if (sb != null && sb.length() > 0) this.setField(CHARACTERS_CONTENT, sb.toString());
					else this.removeField(CHARACTERS_CONTENT);
				}

				return this;
			}

			try {
				if (event.isCharacters()) this.doAddCharacters(ctx, event, args);
				else this.doParseEventContent(ctx, event, args);
			} catch (Exception e) {
				ctx.firePropertyChange(new XMLParserNotification(ctx, XMLParserNotification.EXCEPTION, event, "XML.ExceptionParsingElement", null, e));
			}

			event = ctx.nextEvent();
		}

		return null;
	}

	private XMLEvent doCopyInputEvent(XMLEvent inputEvent) {
		XMLEvent newEv = new FakeXMLEvent(inputEvent.getName().getNamespaceURI(), inputEvent.getName().getLocalPart());
		return newEv;
	}

	protected void doAddCharacters(XMLEventParserContext ctx, XMLEvent event, Object... args) {
		String s = ctx.getCharacters(event);
		if (WWUtil.isEmpty(s)) return;

		StringBuilder sb = (StringBuilder) this.getField(CHARACTERS_CONTENT);
		if (sb != null) sb.append(s);
		else this.setField(CHARACTERS_CONTENT, new StringBuilder(s));
	}

	public String getCharacters() {
		return (String) this.getField(CHARACTERS_CONTENT);
	}

	/**
	 * Parse an event's sub-elements.
	 * 
	 * @param ctx
	 *            a current parser context.
	 * @param event
	 *            the event to parse.
	 * @param args
	 *            an optional list of arguments that may by used by subclasses.
	 * @throws XMLParserException
	 *             if a parser exception occurs during event-stream reading.
	 * @throws IOException
	 *             if an error occurs while reading the parser stream.
	 */
	protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLParserException {
		// Override in subclass to parse an event's sub-elements.
		if (event.isStartElement()) {
			XMLEventParser parser = this.allocate(ctx, event);

			if (parser == null) {
				ctx.firePropertyChange(new XMLParserNotification(ctx, XMLParserNotification.UNRECOGNIZED, event, "XML.UnrecognizedElement", null, event));
				parser = ctx.getUnrecognizedElementParser();

				// Register an unrecognized parser for the element type.
				QName elementName = event.getName();
				if (elementName != null) ctx.registerParser(elementName, parser);
			}

			if (parser != null) {
				Object o = parser.parse(ctx, event, args);
				if (o == null) return;

				this.doAddEventContent(o, ctx, event, args);
			}
		}
	}

	protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args) {
		// Override in subclass if need to react to certain elements.
		this.setField(event.getName(), o);
	}

	/**
	 * Parse an event's attributes.
	 * 
	 * @param ctx
	 *            a current parser context.
	 * @param event
	 *            the event to parse.
	 * @param args
	 *            an optional list of arguments that may by used by subclasses.
	 */
	protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLParserException {
		AVList attributes = event.getAttributes();
		if (attributes == null) return;

		for (Map.Entry<String, Object> entry : attributes.getEntries()) {
			this.doAddEventAttribute(entry.getKey(), entry.getValue(), ctx, event, args);
		}
	}

	protected void doAddEventAttribute(String key, Object value, XMLEventParserContext ctx, XMLEvent event, Object... args) {
		// Override in subclass if need to react to certain attributes.
		this.setField(key, value.toString());
	}

	protected String parseCharacterContent(XMLEventParserContext ctx, XMLEvent stringEvent, Object... args) throws XMLParserException {
		StringBuilder value = new StringBuilder();

		for (XMLEvent event = ctx.nextEvent(); event != null; event = ctx.nextEvent()) {
			if (ctx.isEndElement(event, stringEvent)) return value.length() > 0 ? value.toString() : null;

			if (event.isCharacters()) {
				String s = ctx.getCharacters(event);
				if (s != null) value.append(s);
			}
		}

		return null;
	}
}
