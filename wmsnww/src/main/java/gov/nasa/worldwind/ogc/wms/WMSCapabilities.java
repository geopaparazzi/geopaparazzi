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

import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.ogc.OGCConstants;
import gov.nasa.worldwind.ogc.OGCOnlineResource;
import gov.nasa.worldwind.ogc.OGCRequestDescription;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import gov.nasa.worldwind.util.xml.XMLParserException;
import gov.nasa.worldwind.wms.CapabilitiesRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: WMSCapabilities.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WMSCapabilities extends OGCCapabilities {
	protected static final QName ROOT_ELEMENT_NAME_1_1_1 = new QName("WMT_MS_Capabilities");
	protected static final QName ROOT_ELEMENT_NAME_1_3_0 = new QName("WMS_Capabilities");

	public static WMSCapabilities retrieve(URI uri) throws Exception {
		try {
			CapabilitiesRequest request = new CapabilitiesRequest(uri);

			return new WMSCapabilities(request);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

//	/**
//	 * Parses a WMS capabilities document.
//	 *
//	 * @param docSource
//	 *            the XML source. May be a filename, file, stream or other type allowed by {@link gov.nasa.worldwind.util.WWXML#openEventReader(Object)}.
//	 * @throws IllegalArgumentException
//	 *             if the document source is null.
//	 */
//	public WMSCapabilities(Object docSource) {
//		super(OGCConstants.WMS_NAMESPACE_URI, docSource);
//
//		this.initialize();
//	}

	public WMSCapabilities(CapabilitiesRequest docSource) throws URISyntaxException, MalformedURLException {
		super(OGCConstants.WMS_NAMESPACE_URI, docSource.getUri().toURL());

		this.initialize();
	}

	private void initialize() {
		this.getParserContext().registerParser(new QName(this.getDefaultNamespaceURI(), "Service"), new WMSServiceInformation(this.getNamespaceURI()));
		this.getParserContext().registerParser(new QName(this.getDefaultNamespaceURI(), "Capability"), new WMSCapabilityInformation(this.getNamespaceURI()));
	}

	@Override
	public String getDefaultNamespaceURI() {
		return OGCConstants.WMS_NAMESPACE_URI;
	}

	public boolean isRootElementName(QName candidate) {
		return this.getParserContext().isSameName(candidate, ROOT_ELEMENT_NAME_1_1_1) || this.getParserContext().isSameName(candidate, ROOT_ELEMENT_NAME_1_3_0);
	}

	public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event) {
		if (ctx.isStartElement(event, CAPABILITY)) return ctx.allocate(event, new WMSCapabilityInformation(this.getNamespaceURI()));
		else return super.allocate(ctx, event);
	}

	@Override
	public WMSCapabilities parse(Object... args) throws XMLParserException {
		return (WMSCapabilities) super.parse(args);
	}

	/**
	 * Returns all named layers in the capabilities document.
	 * 
	 * @return an unordered list of the document's named layers.
	 */
	public List<WMSLayerCapabilities> getNamedLayers() {
		if (this.getCapabilityInformation() == null || this.getCapabilityInformation().getLayerCapabilities() == null) return null;

		List<WMSLayerCapabilities> namedLayers = new ArrayList<WMSLayerCapabilities>();

		for (WMSLayerCapabilities layer : this.getCapabilityInformation().getLayerCapabilities()) {
			namedLayers.addAll(layer.getNamedLayers());
		}

		return namedLayers;
	}

	public WMSLayerCapabilities getLayerByName(String name) {
		if (WWUtil.isEmpty(name)) return null;

		List<WMSLayerCapabilities> namedLayers = this.getNamedLayers();
		for (WMSLayerCapabilities layer : namedLayers) {
			if (layer.getName().equals(name)) return layer;
		}

		return null;
	}

	public WMSCapabilityInformation getCapabilityInformation() {
		return (WMSCapabilityInformation) super.getCapabilityInformation();
	}

	public Set<String> getImageFormats() {
		Set<OGCRequestDescription> requestDescriptions = this.getCapabilityInformation().getRequestDescriptions();
		for (OGCRequestDescription rd : requestDescriptions) {
			if (rd.getRequestName().equals("GetMap")) return rd.getFormats();
		}

		return null;
	}

	public Long getLayerLatestLastUpdateTime(WMSCapabilities caps, String[] layerNames) {
		if (caps == null) {
			String message = Logging.getMessage("nullValue.WMSCapabilities");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (layerNames == null) {
			String message = Logging.getMessage("nullValue.WMSLayerNames");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		String lastUpdate = null;

		for (String name : layerNames) {
			WMSLayerCapabilities layer = this.getLayerByName(name);
			if (layer == null) continue;

			String update = layer.getLastUpdate();
			if (update != null && update.length() > 0 && (lastUpdate == null || update.compareTo(lastUpdate) > 0)) lastUpdate = update;
		}

		if (lastUpdate != null) {
			try {
				return Long.parseLong(lastUpdate);
			} catch (NumberFormatException e) {
				String message = Logging.getMessage("generic.ConversionError", lastUpdate);
				Logging.error(message);
			}
		}

		return null;
	}

	public Double[] getLayerExtremeElevations(WMSCapabilities caps, String[] layerNames) {
		if (caps == null) {
			String message = Logging.getMessage("nullValue.WMSCapabilities");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (layerNames == null) {
			String message = Logging.getMessage("nullValue.WMSLayerNames");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		Double extremeMin = null;
		Double extremeMax = null;

		for (String name : layerNames) {
			WMSLayerCapabilities layer = caps.getLayerByName(name);
			if (layer == null) continue;

			Double min = layer.getExtremeElevationMin();
			if (min != null && (extremeMin == null || min.compareTo(min) > 0)) extremeMin = min;

			Double max = layer.getExtremeElevationMax();
			if (max != null && (extremeMax == null || max.compareTo(max) > 0)) extremeMax = max;
		}

		if (extremeMin != null || extremeMax != null) {
			Double[] extremes = new Double[] { null, null };

			if (extremeMin != null) extremes[0] = extremeMin;
			if (extremeMax != null) extremes[1] = extremeMax;

			return extremes;
		}

		return null;
	}

	public OGCRequestDescription getRequestDescription(String requestName) {
		for (OGCRequestDescription rd : this.getCapabilityInformation().getRequestDescriptions()) {
			if (rd.getRequestName().equalsIgnoreCase(requestName)) return rd;
		}

		return null;
	}

	public String getRequestURL(String requestName, String protocol, String requestMethod) {
		OGCRequestDescription rd = this.getRequestDescription(requestName);
		if (rd != null) {
			OGCOnlineResource ol = rd.getOnlineResouce(protocol, requestMethod);
			return ol != null ? ol.getHref() : null;
		}

		return null;
	}

	@Override
	public String toString() // TODO: Complete this method
	{
		StringBuilder sb = new StringBuilder(super.toString());

		sb.append("LAYERS\n");

		for (WMSLayerCapabilities layerCaps : this.getNamedLayers()) {
			sb.append(layerCaps.toString()).append("\n");
		}

		return sb.toString();
	}
}
