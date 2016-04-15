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

import java.io.IOException;
import java.util.List;

import org.mapsforge.android.maps.rendertheme.RenderCallback;
import org.mapsforge.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforge.core.model.Tag;
import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader;

/**
 * Represents a closed polygon on the map.
 */
public final class Area implements RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @param relativePathPrefix
	 *            the prefix for relative resource paths.
	 * @return a new Area with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static Area create(String elementName, Attributes attributes, int level, String relativePathPrefix)
			throws IOException {
		String src = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name)) {
				src = value;
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(strokeWidth);
		return new Area(relativePathPrefix, src, fill, stroke, strokeWidth, level);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	private final Paint fill;
	private final int level;
	private final Paint outline;
	private final float strokeWidth;

	private Area(String relativePathPrefix, String src, int fill, int stroke, float strokeWidth, int level)
			throws IOException {
		super();

		Shader shader = BitmapUtils.createBitmapShader(relativePathPrefix, src);

		if (fill == Color.TRANSPARENT) {
			this.fill = null;
		} else {
			this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.fill.setShader(shader);
			this.fill.setStyle(Style.FILL);
			this.fill.setColor(fill);
			this.fill.setStrokeCap(Cap.ROUND);
		}

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
		} else {
			this.outline = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.outline.setStyle(Style.STROKE);
			this.outline.setColor(stroke);
			this.outline.setStrokeCap(Cap.ROUND);
		}

		this.strokeWidth = strokeWidth;
		this.level = level;
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
		if (this.outline != null) {
			renderCallback.renderArea(this.outline, this.level);
		}
		if (this.fill != null) {
			renderCallback.renderArea(this.fill, this.level);
		}
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.outline != null) {
			this.outline.setStrokeWidth(this.strokeWidth * scaleFactor);
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}
