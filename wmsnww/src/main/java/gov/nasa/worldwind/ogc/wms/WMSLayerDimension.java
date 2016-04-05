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
import gov.nasa.worldwind.util.xml.XMLParserException;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses a WMS layer Dimension element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSLayerDimension.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSLayerDimension extends AbstractXMLEventParser {
	protected StringBuilder dimension;
	protected String name;
	protected String units;
	protected String unitSymbol;
	protected String defaultValue;
	protected Boolean multipleValues;
	protected Boolean nearestValue;
	protected Boolean current;

	public WMSLayerDimension(String namespaceURI) {
		super(namespaceURI);
	}

	@Override
	protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLParserException {
		if (event.isCharacters()) {
			String s = ctx.getCharacters(event);
			if (!WWUtil.isEmpty(s)) {
				if (this.dimension == null) this.dimension = new StringBuilder();

				this.dimension.append(s);
			}
		}
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

			else if (entry.getKey().equals("units") && entry.getValue() != null) this.setUnits(entry.getValue().toString());

			else if (entry.getKey().equals("unitSymbol") && entry.getValue() != null) this.setUnitSymbol(entry.getValue().toString());

			else if (entry.getKey().equals("default") && entry.getValue() != null) this.setDefaultValue(entry.getValue().toString());

			else if (entry.getKey().equals("multipleValues") && entry.getValue() != null) {
				Boolean d = WWUtil.convertStringToBoolean(entry.getValue().toString());
				if (d != null) this.setMultipleValues(d);
			} else if (entry.getKey().equals("nearestValue") && entry.getValue() != null) {
				Boolean d = WWUtil.convertStringToBoolean(entry.getValue().toString());
				if (d != null) this.setNearestValue(d);
			} else if (entry.getKey().equals("current") && entry.getValue() != null) {
				Boolean d = WWUtil.convertStringToBoolean(entry.getValue().toString());
				if (d != null) this.setCurrent(d);
			}
		}
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		// if (attr.getName().getLocalPart().equals("name") && attr.getValue() != null) this.setName(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("units") && attr.getValue() != null) this.setUnits(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("unitSymbol") && attr.getValue() != null) this.setUnitSymbol(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("default") && attr.getValue() != null) this.setDefaultValue(attr.getValue());
		//
		// else if (attr.getName().getLocalPart().equals("multipleValues") && attr.getValue() != null) {
		// Boolean d = WWUtil.convertStringToBoolean(attr.getValue());
		// if (d != null) this.setMultipleValues(d);
		// } else if (attr.getName().getLocalPart().equals("nearestValue") && attr.getValue() != null) {
		// Boolean d = WWUtil.convertStringToBoolean(attr.getValue());
		// if (d != null) this.setNearestValue(d);
		// } else if (attr.getName().getLocalPart().equals("current") && attr.getValue() != null) {
		// Boolean d = WWUtil.convertStringToBoolean(attr.getValue());
		// if (d != null) this.setCurrent(d);
		// }
		// }
	}

	public String getDimension() {
		if (this.dimension == null) this.dimension = new StringBuilder();

		return dimension.toString();
	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public String getUnits() {
		return units;
	}

	protected void setUnits(String units) {
		this.units = units;
	}

	public String getUnitSymbol() {
		return unitSymbol;
	}

	protected void setUnitSymbol(String unitSymbol) {
		this.unitSymbol = unitSymbol;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	protected void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Boolean isMultipleValues() {
		return multipleValues;
	}

	protected void setMultipleValues(Boolean multipleValues) {
		this.multipleValues = multipleValues;
	}

	public Boolean isNearestValue() {
		return nearestValue;
	}

	protected void setNearestValue(Boolean nearestValue) {
		this.nearestValue = nearestValue;
	}

	public Boolean isCurrent() {
		return current;
	}

	protected void setCurrent(Boolean current) {
		this.current = current;
	}
}
