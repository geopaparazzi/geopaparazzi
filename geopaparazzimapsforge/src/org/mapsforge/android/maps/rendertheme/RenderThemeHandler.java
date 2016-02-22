/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.rendertheme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.android.maps.rendertheme.renderinstruction.Area;
import org.mapsforge.android.maps.rendertheme.renderinstruction.Caption;
import org.mapsforge.android.maps.rendertheme.renderinstruction.Circle;
import org.mapsforge.android.maps.rendertheme.renderinstruction.Line;
import org.mapsforge.android.maps.rendertheme.renderinstruction.LineSymbol;
import org.mapsforge.android.maps.rendertheme.renderinstruction.PathText;
import org.mapsforge.android.maps.rendertheme.renderinstruction.Symbol;
import org.mapsforge.core.util.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler to parse XML render theme files.
 */
public final class RenderThemeHandler extends DefaultHandler {
	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE;
	}

	private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
	private static final String ELEMENT_NAME_RULE = "rule";
	private static final Logger LOGGER = Logger.getLogger(RenderThemeHandler.class.getName());
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	/**
	 * @param jobTheme
	 *            the JobTheme to create a RenderTheme from.
	 * @return a new RenderTheme which is created by parsing the XML data from the input stream.
	 * @throws SAXException
	 *             if an error occurs while parsing the render theme XML.
	 * @throws ParserConfigurationException
	 *             if an error occurs while creating the XML parser.
	 * @throws IOException
	 *             if an I/O error occurs while reading from the input stream.
	 */
	public static RenderTheme getRenderTheme(JobTheme jobTheme) throws SAXException, ParserConfigurationException,
			IOException {
		RenderThemeHandler renderThemeHandler = new RenderThemeHandler(jobTheme.getRelativePathPrefix());
		XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		xmlReader.setContentHandler(renderThemeHandler);
		InputStream inputStream = null;
		try {
			inputStream = jobTheme.getRenderThemeAsStream();
			xmlReader.parse(new InputSource(inputStream));
			return renderThemeHandler.renderTheme;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	/**
	 * Logs the given information about an unknown XML attribute.
	 * 
	 * @param element
	 *            the XML element name.
	 * @param name
	 *            the XML attribute name.
	 * @param value
	 *            the XML attribute value.
	 * @param attributeIndex
	 *            the XML attribute index position.
	 */
	public static void logUnknownAttribute(String element, String name, String value, int attributeIndex) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("unknown attribute in element ");
		stringBuilder.append(element);
		stringBuilder.append(" (");
		stringBuilder.append(attributeIndex);
		stringBuilder.append("): ");
		stringBuilder.append(name);
		stringBuilder.append('=');
		stringBuilder.append(value);
		LOGGER.info(stringBuilder.toString());
	}

	private Rule currentRule;
	private final Stack<Element> elementStack = new Stack<Element>();
	private int level;
	private final String relativePathPrefix;
	private RenderTheme renderTheme;
	private final Stack<Rule> ruleStack = new Stack<Rule>();

	private RenderThemeHandler(String relativePathPrefix) {
		super();
		this.relativePathPrefix = relativePathPrefix;
	}

	@Override
	public void endDocument() {
		if (this.renderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		this.renderTheme.setLevels(this.level);
		this.renderTheme.complete();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		this.elementStack.pop();

		if (ELEMENT_NAME_RULE.equals(localName)) {
			this.ruleStack.pop();
			if (this.ruleStack.empty()) {
				this.renderTheme.addRule(this.currentRule);
			} else {
				this.currentRule = this.ruleStack.peek();
			}
		}
	}

	@Override
	public void error(SAXParseException exception) {
		LOGGER.log(Level.SEVERE, null, exception);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if (ELEMENT_NAME_RENDER_THEME.equals(localName)) {
				checkState(localName, Element.RENDER_THEME);
				this.renderTheme = RenderTheme.create(localName, attributes);
			}

			else if (ELEMENT_NAME_RULE.equals(localName)) {
				checkState(localName, Element.RULE);
				Rule rule = Rule.create(localName, attributes, this.ruleStack);
				if (!this.ruleStack.empty()) {
					this.currentRule.addSubRule(rule);
				}
				this.currentRule = rule;
				this.ruleStack.push(this.currentRule);
			}

			else if ("area".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Area area = Area.create(localName, attributes, this.level++, this.relativePathPrefix);
				this.ruleStack.peek().addRenderingInstruction(area);
			}

			else if ("caption".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Caption caption = Caption.create(localName, attributes);
				this.currentRule.addRenderingInstruction(caption);
			}

			else if ("circle".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Circle circle = Circle.create(localName, attributes, this.level++);
				this.currentRule.addRenderingInstruction(circle);
			}

			else if ("line".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = Line.create(localName, attributes, this.level++, this.relativePathPrefix);
				this.currentRule.addRenderingInstruction(line);
			}

			else if ("lineSymbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = LineSymbol.create(localName, attributes, this.relativePathPrefix);
				this.currentRule.addRenderingInstruction(lineSymbol);
			}

			else if ("pathText".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				PathText pathText = PathText.create(localName, attributes);
				this.currentRule.addRenderingInstruction(pathText);
			}

			else if ("symbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = Symbol.create(localName, attributes, this.relativePathPrefix);
				this.currentRule.addRenderingInstruction(symbol);
			}

			else {
				throw new SAXException("unknown element: " + localName);
			}
		} catch (IllegalArgumentException e) {
			throw new SAXException(null, e);
		} catch (IOException e) {
			throw new SAXException(null, e);
		}
	}

	@Override
	public void warning(SAXParseException exception) {
		LOGGER.log(Level.SEVERE, null, exception);
	}

	private void checkElement(String elementName, Element element) throws SAXException {
		switch (element) {
			case RENDER_THEME:
				if (!this.elementStack.empty()) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RULE:
				Element parentElement = this.elementStack.peek();
				if (parentElement != Element.RENDER_THEME && parentElement != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_INSTRUCTION:
				if (this.elementStack.peek() != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;
		}

		throw new SAXException("unknown enum value: " + element);
	}

	private void checkState(String elementName, Element element) throws SAXException {
		checkElement(elementName, element);
		this.elementStack.push(element);
	}
}
