/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.util.WWUtil;

/**
 * @author tag
 * @version $Id: DoubleXMLEventParser.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class DoubleXMLEventParser extends AbstractXMLEventParser {
	public DoubleXMLEventParser() {
	}

	public DoubleXMLEventParser(String namespaceUri) {
		super(namespaceUri);
	}

	public Object parse(XMLEventParserContext ctx, XMLEvent doubleEvent, Object... args) throws XMLParserException

	{
		String s = this.parseCharacterContent(ctx, doubleEvent);
		return s != null ? WWUtil.convertStringToDouble(s) : null;
	}

	public Double parseDouble(XMLEventParserContext ctx, XMLEvent doubleEvent, Object... args) throws XMLParserException

	{
		return (Double) this.parse(ctx, doubleEvent, args);
	}
}
