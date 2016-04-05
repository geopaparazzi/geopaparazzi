/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc;

import gov.nasa.worldwind.util.xml.AbstractXMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEvent;
import gov.nasa.worldwind.util.xml.XMLEventParser;
import gov.nasa.worldwind.util.xml.XMLEventParserContext;
import gov.nasa.worldwind.util.xml.XMLParserException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * Parses an OGC DCPType element.
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version $Id: OGCDCType.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class OGCDCType extends AbstractXMLEventParser {
	protected QName GET;
	protected QName POST;
	protected QName HTTP;
	protected QName ONLINE_RESOURCE;

	public static class DCPInfo {
		protected String protocol;
		protected String method;
		protected OGCOnlineResource onlineResource;

		public DCPInfo(String protocol) {
			this.protocol = protocol;
		}
	}

	protected List<DCPInfo> protocols = new ArrayList<DCPInfo>(1);

	public OGCDCType(String namespaceURI) {
		super(namespaceURI);

		this.initialize();
	}

	@Override
	public XMLEventParser allocate(XMLEventParserContext ctx, XMLEvent event) {
		XMLEventParser defaultParser = null;

		if (ctx.isStartElement(event, ONLINE_RESOURCE)) defaultParser = new OGCOnlineResource(this.getNamespaceURI());

		return ctx.allocate(event, defaultParser);
	}

	private void initialize() {
		GET = new QName(this.getNamespaceURI(), "Get");
		POST = new QName(this.getNamespaceURI(), "Post");
		HTTP = new QName(this.getNamespaceURI(), "HTTP");
		ONLINE_RESOURCE = new QName(this.getNamespaceURI(), "OnlineResource");
	}

	@Override
	protected void doParseEventContent(XMLEventParserContext ctx, XMLEvent event, Object... args) throws XMLParserException {
		if (ctx.isStartElement(event, HTTP)) {
			this.addProtocol(event.getName().getLocalPart());
		} else if (ctx.isStartElement(event, GET) || ctx.isStartElement(event, POST)) {
			this.addRequestMethod(event.getName().getLocalPart());
		} else if (ctx.isStartElement(event, ONLINE_RESOURCE)) {
			XMLEventParser parser = this.allocate(ctx, event);
			if (parser != null) {
				Object o = parser.parse(ctx, event, args);
				if (o != null && o instanceof OGCOnlineResource) this.addOnlineResource((OGCOnlineResource) o);
			}
		}
	}

	public List<DCPInfo> getDCPInfos() {
		return this.protocols;
	}

	protected void addProtocol(String protocol) {
		this.protocols.add(new DCPInfo(protocol));
	}

	protected void addRequestMethod(String requestMethod) {
		DCPInfo dcpi = this.protocols.get(this.protocols.size() - 1);

		if (dcpi.method != null) {
			dcpi = new DCPInfo(dcpi.protocol);
			this.protocols.add(dcpi);
		}

		dcpi.method = requestMethod;
	}

	protected void addOnlineResource(OGCOnlineResource onlineResource) {
		DCPInfo dcpi = this.protocols.get(this.protocols.size() - 1);

		dcpi.onlineResource = onlineResource;
	}

	public OGCOnlineResource getOnlineResouce(String protocol, String requestMethod) {
		for (DCPInfo dcpi : this.getDCPInfos()) {
			if (!dcpi.protocol.equalsIgnoreCase(protocol)) continue;

			if (dcpi.method.equalsIgnoreCase(requestMethod)) return dcpi.onlineResource;
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (DCPInfo dcpi : this.getDCPInfos()) {
			sb.append(dcpi.protocol).append(", ");
			sb.append(dcpi.method).append(", ");
			sb.append(dcpi.onlineResource.toString());
		}

		return sb.toString();
	}
}
