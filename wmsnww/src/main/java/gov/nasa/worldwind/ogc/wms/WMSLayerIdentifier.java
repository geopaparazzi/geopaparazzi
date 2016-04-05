/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wms;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses a WMS layer Identifier element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSLayerIdentifier.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSLayerIdentifier extends AbstractXMLEventParser {
	protected String identifier;
	protected String authority;

	public WMSLayerIdentifier(String namespaceURI) {
		super(namespaceURI);
	}

	@Override
	protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args) {
		AVList attrAvList = event.getAttributes();
		if (null == attrAvList || attrAvList.getEntries().isEmpty()) return;
		// Iterator iter = event.asStartElement().getAttributes();
		// if (iter == null) return;
		Set<Entry<String, Object>> avListEntries = attrAvList.getEntries();
		for (Entry<String, Object> entry : avListEntries) {
			if (entry.getKey().equals("authority") && entry.getValue() != null) this.setAuthority(entry.getValue().toString());

		}
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		// if (attr.getName().getLocalPart().equals("authority") && attr.getValue() != null) this.setAuthority(attr.getValue());
		// }
	}

	public String getIdentifier() {
		return this.getCharacters();
	}

	public String getAuthority() {
		return authority;
	}

	protected void setAuthority(String authority) {
		this.authority = authority;
	}
}
