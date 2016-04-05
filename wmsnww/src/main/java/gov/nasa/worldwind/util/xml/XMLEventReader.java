/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.util.xml;

import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author tag
 * @version $Id: XMLEventReader.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class XMLEventReader {
	protected XmlPullParser parser;

	public XMLEventReader(XmlPullParser parser) {
		this.parser = parser;
	}

	public XMLEvent nextEvent() throws XMLParserException {
		try {
			int eventType = this.parser.next();

			if (eventType == XmlPullParser.END_DOCUMENT) return null;

			return new XMLEvent(eventType, parser);
		} catch (IOException e) {
			throw new XMLParserException(e);
		} catch (XmlPullParserException e) {
			throw new XMLParserException(e);
		}
	}
}
