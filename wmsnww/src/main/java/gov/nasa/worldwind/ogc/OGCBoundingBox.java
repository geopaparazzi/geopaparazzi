/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Parses an OGC BoundingBox element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: OGCBoundingBox.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class OGCBoundingBox extends AbstractXMLEventParser {
	private String crs;
	private double minx;
	private double maxx;
	private double miny;
	private double maxy;
	private double resx;
	private double resy;

	public static OGCBoundingBox createFromStrings(String crs, String minx, String maxx, String miny, String maxy, String resx, String resy) {
		OGCBoundingBox bbox = new OGCBoundingBox(null);

		try {
			bbox.crs = crs;
			bbox.minx = Double.parseDouble(minx);
			bbox.maxx = Double.parseDouble(maxx);
			bbox.miny = Double.parseDouble(miny);
			bbox.maxy = Double.parseDouble(maxy);
			bbox.resx = resx != null && !resx.equals("") ? Double.parseDouble(resx) : 0;
			bbox.resy = resy != null && !resy.equals("") ? Double.parseDouble(resy) : 0;
		} catch (NumberFormatException e) {
			String message = Logging.getMessage("XML.ImproperDataType");
			Logging.error(message);
			throw e;
		}

		return bbox;
	}

	public OGCBoundingBox(String namespaceURI) {
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
			if (entry.getKey().equals("CRS") && entry.getValue() != null) {
				String s = entry.getValue().toString();
				if (s != null) this.setCRS(s);
			} else if (entry.getKey().equals("minx") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setMinx(d);
			} else if (entry.getKey().equals("miny") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setMiny(d);
			} else if (entry.getKey().equals("maxx") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setMaxx(d);
			} else if (entry.getKey().equals("maxy") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setMaxy(d);
			} else if (entry.getKey().equals("resx") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setResx(d);
			} else if (entry.getKey().equals("resy") && entry.getValue() != null) {
				Double d = WWUtil.convertStringToDouble(entry.getValue().toString());
				if (d != null) this.setResy(d);
			}
		}
		// while (iter.hasNext()) {
		// Attribute attr = (Attribute) iter.next();
		// if (attr.getName().getLocalPart().equals("CRS") && attr.getValue() != null) {
		// String s = attr.getValue();
		// if (s != null) this.setCRS(s);
		// } else if (attr.getName().getLocalPart().equals("minx") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setMinx(d);
		// } else if (attr.getName().getLocalPart().equals("miny") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setMiny(d);
		// } else if (attr.getName().getLocalPart().equals("maxx") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setMaxx(d);
		// } else if (attr.getName().getLocalPart().equals("maxy") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setMaxy(d);
		// } else if (attr.getName().getLocalPart().equals("resx") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setResx(d);
		// } else if (attr.getName().getLocalPart().equals("resy") && attr.getValue() != null) {
		// Double d = WWUtil.convertStringToDouble(attr.getValue());
		// if (d != null) this.setResy(d);
		// }
		// }
	}

	public String getCRS() {
		return crs;
	}

	protected void setCRS(String crs) {
		this.crs = crs;
	}

	public double getMinx() {
		return minx;
	}

	protected void setMinx(double minx) {
		this.minx = minx;
	}

	public double getMaxx() {
		return maxx;
	}

	protected void setMaxx(double maxx) {
		this.maxx = maxx;
	}

	public double getMiny() {
		return miny;
	}

	protected void setMiny(double miny) {
		this.miny = miny;
	}

	public double getMaxy() {
		return maxy;
	}

	protected void setMaxy(double maxy) {
		this.maxy = maxy;
	}

	public double getResx() {
		return resx;
	}

	protected void setResx(double resx) {
		this.resx = resx;
	}

	public double getResy() {
		return resy;
	}

	protected void setResy(double resy) {
		this.resy = resy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.crs);
		sb.append(": minx = ");
		sb.append(this.minx);
		sb.append(" miny = ");
		sb.append(this.miny);
		sb.append(" maxx = ");
		sb.append(this.maxx);
		sb.append(" maxy = ");
		sb.append(this.maxy);
		sb.append(" resx = ");
		sb.append(this.resx);
		sb.append(" resy = ");
		sb.append(this.resy);

		return sb.toString();
	}
}
