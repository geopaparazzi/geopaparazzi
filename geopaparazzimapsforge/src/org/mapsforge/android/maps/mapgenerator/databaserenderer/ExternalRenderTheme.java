/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator.databaserenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.mapsforge.android.maps.mapgenerator.JobTheme;

/**
 * An ExternalRenderTheme allows for customizing the rendering style of the map via an XML file.
 */
public class ExternalRenderTheme implements JobTheme {
	private static final long serialVersionUID = 1L;

	private final long fileModificationDate;
	private transient int hashCodeValue;
	private final File renderThemeFile;

	/**
	 * @param renderThemeFile
	 *            the XML render theme file.
	 * @throws FileNotFoundException
	 *             if the file does not exist or cannot be read.
	 */
	public ExternalRenderTheme(File renderThemeFile) throws FileNotFoundException {
		if (!renderThemeFile.exists()) {
			throw new FileNotFoundException("file does not exist: " + renderThemeFile);
		} else if (!renderThemeFile.isFile()) {
			throw new FileNotFoundException("not a file: " + renderThemeFile);
		} else if (!renderThemeFile.canRead()) {
			throw new FileNotFoundException("cannot read file: " + renderThemeFile);
		}

		this.fileModificationDate = renderThemeFile.lastModified();
		if (this.fileModificationDate == 0L) {
			throw new FileNotFoundException("cannot read last modification time");
		}
		this.renderThemeFile = renderThemeFile;
		calculateTransientValues();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof ExternalRenderTheme)) {
			return false;
		}
		ExternalRenderTheme other = (ExternalRenderTheme) obj;
		if (this.fileModificationDate != other.fileModificationDate) {
			return false;
		} else if (this.renderThemeFile == null && other.renderThemeFile != null) {
			return false;
		} else if (this.renderThemeFile != null && !this.renderThemeFile.equals(other.renderThemeFile)) {
			return false;
		}
		return true;
	}

	@Override
	public String getRelativePathPrefix() {
		return this.renderThemeFile.getParent();
	}

	@Override
	public InputStream getRenderThemeAsStream() throws FileNotFoundException {
		return new FileInputStream(this.renderThemeFile);
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 1;
		result = 31 * result + (int) (this.fileModificationDate ^ (this.fileModificationDate >>> 32));
		result = 31 * result + ((this.renderThemeFile == null) ? 0 : this.renderThemeFile.hashCode());
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.hashCodeValue = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}
}
