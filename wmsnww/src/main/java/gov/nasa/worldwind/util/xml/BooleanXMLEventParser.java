/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.util.WWUtil;

/**
 * @author tag
 * @version $Id: BooleanXMLEventParser.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class BooleanXMLEventParser extends AbstractXMLEventParser
{
    public BooleanXMLEventParser()
    {
    }

    public BooleanXMLEventParser(String namespaceUri)
    {
        super(namespaceUri);
    }

    public Object parse(XMLEventParserContext ctx, XMLEvent booleanEvent, Object... args)
        throws XMLParserException

    {
        String s = this.parseCharacterContent(ctx, booleanEvent);
        if (s == null)
            return false;

        s = s.trim();

        if (s.length() > 1)
            return s.equalsIgnoreCase("true");

        return WWUtil.convertNumericStringToBoolean(s);
    }
}
