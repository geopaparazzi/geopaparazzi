/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wms;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses a WMS layer Extent element. These elements are defined only in WMS 1.1.1.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSLayerExtent.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSLayerExtent extends AbstractXMLEventParser {
	protected String extent;
	protected String name;
	protected String defaultValue;
	protected Boolean nearestValue;

	public WMSLayerExtent(String namespaceURI) {
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
			if (entry.getKey().equals("name") && entry.getValue() != null) this.setName(entry.getValue().toString());
			else if (entry.getKey().equals("default") && entry.getValue() != null) this.setDefaultValue(entry.getValue().toString());
			else if (entry.getKey().equals("nearestValue") && entry.getValue() != null) {
				Boolean d = WWUtil.convertStringToBoolean(entry.getValue().toString());
				if (d != null) this.setNearestValue(d);
			}
		}
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		// if (attr.getName().getLocalPart().equals("name") && attr.getValue() != null) this.setName(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("default") && attr.getValue() != null) this.setDefaultValue(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("nearestValue") && attr.getValue() != null) {
		// Boolean d = WWUtil.convertStringToBoolean(attr.getValue());
		// if (d != null) this.setNearestValue(d);
		// }
		// }
	}

	public String getExtent() {
		return this.getCharacters();
	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	protected void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Boolean isNearestValue() {
		return nearestValue;
	}

	protected void setNearestValue(Boolean nearestValue) {
		this.nearestValue = nearestValue;
	}
}
