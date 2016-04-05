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

package gov.nasa.worldwind.util.xml;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import javax.xml.namespace.QName;
import org.xmlpull.v1.XmlPullParser;

/**
 * Edited By: Nicola Dorigatti, Trilogis (Fixed a bug that was leaving unclosed tags to be unrecognized)
 * 
 * @author tag
 * @version $Id: XMLEvent.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class XMLEvent {
	protected int eventType;
	protected XmlPullParser xpp;

	public XMLEvent(int eventType, XmlPullParser pullParser) {
		this.eventType = eventType;
		this.xpp = pullParser;
	}

	public int getLineNumber() {
		return this.xpp.getLineNumber();
	}

	public boolean isStartElement() {
		return this.eventType == XmlPullParser.START_TAG;
	}

	public boolean isEndElement() {
		return this.eventType == XmlPullParser.END_TAG;
	}

	public boolean isCharacters() {
		return this.eventType == XmlPullParser.TEXT;
	}

	public boolean isWhiteSpace() {
		return this.isCharacters() && (this.getData() == null || this.getData().trim().length() == 0);
	}

	public QName getName() {
		return new QName(this.xpp.getNamespace(), xpp.getName());
	}

	public String getData() {
		return this.xpp.getText();
	}

	public AVList getAttributes() {
		if (this.xpp.getAttributeCount() == 0) return null;

		AVListImpl avList = new AVListImpl();

		for (int i = 0; i < this.xpp.getAttributeCount(); i++) {
			avList.setValue(this.xpp.getAttributeName(i), this.xpp.getAttributeValue(i));
		}

		return avList;
	}
}
