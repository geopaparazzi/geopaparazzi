/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * Parses an OGC OnlineResource element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: OGCOnlineResource.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class OGCOnlineResource extends AbstractXMLEventParser {
	protected QName HREF;
	protected QName TYPE;

	protected String type;
	protected String href;

	public OGCOnlineResource(String namespaceURI) {
		super(namespaceURI);

		this.initialize();
	}

	private void initialize() {
		HREF = new QName(WWXML.XLINK_URI, "href");
		TYPE = new QName(WWXML.XLINK_URI, "type");
	}

	@Override
	protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args) {
		AVList attrAvList = event.getAttributes();
		if (null == attrAvList || attrAvList.getEntries().isEmpty()) return;
		Set<Entry<String, Object>> avListEntries = attrAvList.getEntries();
		for (Entry<String, Object> entry : avListEntries) {
			if (ctx.isSameAttributeName(new QName(entry.getKey()), HREF)) this.setHref(entry.getValue().toString());
			else if (ctx.isSameAttributeName(new QName(entry.getKey()), TYPE)) this.setType(entry.getValue().toString());
		}

		// FIXME tryed to bring this class to Android, XMLEvent is no long part of Java Default Classpath, it uses pull parser
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		// if (ctx.isSameAttributeName(attr.getName(), HREF)) this.setHref(attr.getValue());
		// else if (ctx.isSameAttributeName(attr.getName(), TYPE)) this.setType(attr.getValue());
		// }
	}

	public String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	public String getHref() {
		return href;
	}

	protected void setHref(String href) {
		this.href = href;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("href: ").append(this.href != null ? this.href : "null");
		sb.append(", type: ").append(this.type != null ? this.type : "null");

		return sb.toString();
	}
}
