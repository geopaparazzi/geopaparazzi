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
package gov.nasa.worldwind.wms;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class provides a means to construct an OGC web service request, such as WMS GetMap or WFS GetCapabilities.
 * 
 * @author tag
 * @version $Id: Request.java 733 2012-09-02 17:15:09Z dcollins $
 */
public abstract class Request {
	private URI uri;

	// Use a TreeMap to hold the query params so that they'll always be attached to the
	// URL query string in the same order. This allows a simple string comparison to
	// determine whether two url strings address the same document.
	private TreeMap<String, String> queryParams = new TreeMap<String, String>();

	/** Constructs a request for the default service, WMS. */
	protected Request() {
		this.initialize(null);
	}

	/**
	 * Constructs a request for the default service, WMS, and a specified server.
	 * 
	 * @param uri
	 *            the address of the web service. May be null when this constructor invoked by subclasses.
	 * @throws URISyntaxException
	 *             if the web service address is not a valid URI.
	 */
	protected Request(URI uri) throws URISyntaxException {
		this(uri, null);
	}

	/**
	 * Constructs a request for a specified service at a specified server.
	 * 
	 * @param uri
	 *            the address of the web service. May be null.
	 * @param service
	 *            the service name. Common names are WMS, WFS, WCS, etc. May by null when this constructor is
	 *            invoked by subclasses.
	 * @throws URISyntaxException
	 *             if the web service address is not a valid URI.
	 */
	protected Request(URI uri, String service) throws URISyntaxException {
		if (uri != null) {
			try {
				this.setUri(uri);
			} catch (URISyntaxException e) {
				Logging.verbose(Logging.getMessage("generic.URIInvalid", uri.toString()));
				throw e;
			}
		}

		this.initialize(service);
	}

	/**
	 * Copy constructor. Performs a shallow copy.
	 * 
	 * @param sourceRequest
	 *            the request to copy.
	 * @throws IllegalArgumentException
	 *             if copy source is null.
	 * @throws URISyntaxException
	 *             if the web service address is not a valid URI.
	 */
	public Request(Request sourceRequest) throws URISyntaxException {
		if (sourceRequest == null) {
			String message = Logging.getMessage("nullValue.CopyConstructorSourceIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		sourceRequest.copyParamsTo(this);
		this.setUri(sourceRequest.getUri());
	}

	protected void initialize(String service) {
		this.queryParams.put("SERVICE", service != null ? service : "WMS");
		this.queryParams.put("EXCEPTIONS", "application/vnd.ogc.se_xml");
	}

	private void copyParamsTo(Request destinationRequest) {
		if (destinationRequest == null) {
			String message = Logging.getMessage("nullValue.CopyTargetIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		for (Map.Entry<String, String> entry : this.queryParams.entrySet()) {
			destinationRequest.setParam((String) (entry).getKey(), (String) (entry).getValue());
		}
	}

	protected void setUri(URI uri) throws URISyntaxException {
		if (uri == null) {
			String message = Logging.getMessage("nullValue.URIIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		try {
			this.uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), this.buildQueryString(uri.getQuery()), null);
		} catch (URISyntaxException e) {
			String message = Logging.getMessage("generic.URIInvalid", uri.toString());
			Logging.verbose(message);
			throw e;
		}
	}

	public String getRequestName() {
		return this.getParam("REQUEST");
	}

	public String getVersion() {
		return this.getParam("VERSION");
	}

	public void setVersion(String version) {
		if (version == null) {
			String message = Logging.getMessage("nullValue.WMSVersionIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.setParam("VERSION", version);
	}

	public String getService() {
		return this.getParam("SERVICE");
	}

	public void setService(String service) {
		if (service == null) {
			String message = Logging.getMessage("nullValue.WMSServiceNameIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.setParam("SERVICE", service);
	}

	public void setParam(String key, String value) {
		if (key != null) this.queryParams.put(key, value);
	}

	public String getParam(String key) {
		return key != null ? this.queryParams.get(key) : null;
	}

	public URI getUri() throws URISyntaxException {
		if (this.uri == null) return null;

		try {
			return new URI(this.uri.getScheme(), this.uri.getUserInfo(), this.uri.getHost(), this.uri.getPort(), uri.getPath(), this.buildQueryString(uri.getQuery()), null);
		} catch (URISyntaxException e) {
			String message = Logging.getMessage("generic.URIInvalid", uri.toString());
			Logging.verbose(message);
			throw e;
		}
	}

	private String buildQueryString(String existingQueryString) {
		StringBuffer queryString = new StringBuffer(existingQueryString != null ? existingQueryString : "");

		if (queryString.length() > 1 && queryString.lastIndexOf("&") != queryString.length() - 1) queryString = queryString.append("&");

		for (Map.Entry<String, String> entry : this.queryParams.entrySet()) {
			if ((entry).getKey() != null && (entry).getValue() != null) {
				queryString.append((entry).getKey());
				queryString.append("=");
				queryString.append((entry).getValue());
				queryString.append("&");
			}
		}

		// Remove a trailing ampersand
		if (WWUtil.isEmpty(existingQueryString)) {
			int trailingAmpersandPosition = queryString.lastIndexOf("&");
			if (trailingAmpersandPosition >= 0) queryString.deleteCharAt(trailingAmpersandPosition);
		}

		return queryString.toString();
	}

	@Override
	public String toString() {
		String errorMessage = "Error converting wms-request URI to string.";
		try {
			URI fullUri = this.getUri();
			return fullUri != null ? fullUri.toString() : errorMessage;
		} catch (URISyntaxException e) {
			return errorMessage;
		}
	}
}
