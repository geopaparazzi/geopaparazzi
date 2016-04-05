/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gov.nasa.worldwind.ogc.wms;

import gov.nasa.worldwind.ogc.OGCOnlineResource;
import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import gov.nasa.worldwind.util.xml.XMLParserException;
import javax.xml.namespace.QName;

/**
 * Parses a WMS layer Attribution element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSLayerAttribution.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSLayerAttribution extends AbstractXMLEventParser {
	protected QName TITLE;
	protected QName ONLINE_RESOURCE;
	protected QName LOGO_URL;

	protected String title;
	protected OGCOnlineResource onlineResource;
	protected WMSLogoURL logoURL;

	public WMSLayerAttribution(String namespaceURI) {
		super(namespaceURI);

		this.initialize();
	}

	private void initialize() {
		TITLE = new QName(this.getNamespaceURI(), "Title");
		ONLINE_RESOURCE = new QName(this.getNamespaceURI(), "OnlineResource");
		LOGO_URL = new QName(this.getNamespaceURI(), "LogoURL");
	}

	@Override
	public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event) {
		XMLEventParser defaultParser = null;

		if (ctx.isStartElement(event, ONLINE_RESOURCE)) defaultParser = new OGCOnlineResource(this.getNamespaceURI());
		else if (ctx.isStartElement(event, LOGO_URL)) defaultParser = new WMSLayerInfoURL(this.getNamespaceURI());

		return ctx.allocate(event, defaultParser);
	}

	@Override
	protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLParserException {
		if (ctx.isStartElement(event, TITLE)) {
			this.setTitle(ctx.getStringParser().parseString(ctx, event));
		} else if (ctx.isStartElement(event, ONLINE_RESOURCE)) {
			XMLEventParser parser = this.allocate(ctx, event);
			if (parser != null) {
				Object o = parser.parse(ctx, event, args);
				if (o != null && o instanceof OGCOnlineResource) this.setOnlineResource((OGCOnlineResource) o);
			}
		} else if (ctx.isStartElement(event, LOGO_URL)) {
			XMLEventParser parser = this.allocate(ctx, event);
			if (parser != null) {
				Object o = parser.parse(ctx, event, args);
				if (o != null && o instanceof WMSLogoURL) this.setLogoURL((WMSLogoURL) o);
			}
		}
	}

	public String getTitle() {
		return title;
	}

	protected void setTitle(String title) {
		this.title = title;
	}

	public OGCOnlineResource getOnlineResource() {
		return onlineResource;
	}

	protected void setOnlineResource(OGCOnlineResource onlineResource) {
		this.onlineResource = onlineResource;
	}

	public WMSLogoURL getLogoURL() {
		return logoURL;
	}

	protected void setLogoURL(WMSLogoURL logoURL) {
		this.logoURL = logoURL;
	}
}
