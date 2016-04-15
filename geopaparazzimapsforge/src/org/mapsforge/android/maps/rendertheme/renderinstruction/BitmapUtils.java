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
package org.mapsforge.android.maps.rendertheme.renderinstruction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader.TileMode;

final class BitmapUtils {
	private static final String PREFIX_FILE = "file:";
	private static final String PREFIX_JAR = "jar:";

	private static InputStream createInputStream(String relativePathPrefix, String src) throws FileNotFoundException {
		if (src.startsWith(PREFIX_JAR)) {
			String absoluteName = getAbsoluteName(relativePathPrefix, src.substring(PREFIX_JAR.length()));
			InputStream inputStream = BitmapUtils.class.getResourceAsStream(absoluteName);
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + absoluteName);
			}
			return inputStream;
		} else if (src.startsWith(PREFIX_FILE)) {
			File file = getFile(relativePathPrefix, src.substring(PREFIX_FILE.length()));
			if (!file.exists()) {
				throw new IllegalArgumentException("file does not exist: " + file);
			} else if (!file.isFile()) {
				throw new IllegalArgumentException("not a file: " + file);
			} else if (!file.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + file);
			}
			return new FileInputStream(file);
		}

		throw new IllegalArgumentException("invalid bitmap source: " + src);
	}

	private static String getAbsoluteName(String relativePathPrefix, String name) {
		if (name.charAt(0) == '/') {
			return name;
		}
		return relativePathPrefix + name;
	}

	private static File getFile(String parentPath, String pathName) {
		if (pathName.charAt(0) == File.separatorChar) {
			return new File(pathName);
		}
		return new File(parentPath, pathName);
	}

	static Bitmap createBitmap(String relativePathPrefix, String src) throws IOException {
		if (src == null || src.length() == 0) {
			// no image source defined
			return null;
		}

		InputStream inputStream = createInputStream(relativePathPrefix, src);
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();
		return bitmap;
	}

	static BitmapShader createBitmapShader(String relativePathPrefix, String src) throws IOException {
		Bitmap bitmap = BitmapUtils.createBitmap(relativePathPrefix, src);
		if (bitmap == null) {
			return null;
		}

		return new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
	}

	private BitmapUtils() {
		throw new IllegalStateException();
	}
}
