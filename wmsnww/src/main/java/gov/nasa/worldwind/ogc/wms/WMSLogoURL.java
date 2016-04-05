/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wms;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses a WMS layer LogoURL element. Also used for WMS layer LegendURL elements.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSLogoURL.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSLogoURL extends WMSLayerInfoURL {
	protected Integer width;
	protected Integer height;

	public WMSLogoURL(String namespaceURI) {
		super(namespaceURI);
	}

	@Override
	protected void doParseEventAttributes(XMLEventParserContext ctx, XMLEvent event, Object... args) {
		super.doParseEventAttributes(ctx, event, args);
		AVList attrAvList = event.getAttributes();
		if (null == attrAvList || attrAvList.getEntries().isEmpty()) return;
		// Iterator iter = event.asStartElement().getAttributes();
		// if (iter == null) return;
		Set<Entry<String, Object>> avListEntries = attrAvList.getEntries();
		for (Entry<String, Object> entry : avListEntries) {
			if (entry.getKey().equals("width") && entry.getValue() != null) {
				Integer i = WWUtil.convertStringToInteger(entry.getValue().toString());
				if (i != null) this.setWidth(i);
			}

			if (entry.getKey().equals("height") && entry.getValue() != null) {
				Integer i = WWUtil.convertStringToInteger(entry.getValue().toString());
				if (i != null) this.setHeight(i);
			}
		}
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		//
		// if (attr.getName().getLocalPart().equals("width") && attr.getValue() != null) {
		// Integer i = WWUtil.convertStringToInteger(attr.getValue());
		// if (i != null) this.setWidth(i);
		// }
		//
		// if (attr.getName().getLocalPart().equals("height") && attr.getValue() != null) {
		// Integer i = WWUtil.convertStringToInteger(attr.getValue());
		// if (i != null) this.setHeight(i);
		// }
		// }
	}

	public Integer getWidth() {
		return width;
	}

	protected void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	protected void setHeight(Integer height) {
		this.height = height;
	}
}
