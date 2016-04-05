/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.avlist.AVList;
import javax.xml.namespace.QName;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author tag
 * @version $Id: XMLEvent.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class FakeXMLEvent extends XMLEvent {
	private QName qname;

	public FakeXMLEvent(QName qname) {
		super(0, null);
		this.qname = qname;
	}

	public FakeXMLEvent(String namespace, String localPart) {
		super(0, null);
		this.qname = new QName(namespace, localPart);
	}

	@Override
	public int getLineNumber() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public boolean isStartElement() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public boolean isEndElement() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public boolean isCharacters() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public boolean isWhiteSpace() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public QName getName() {
		return this.qname;
	}

	@Override
	public String getData() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}

	@Override
	public AVList getAttributes() {
		throw new IllegalArgumentException("This is a fake element, only get Name method is allowed!");
	}
}
