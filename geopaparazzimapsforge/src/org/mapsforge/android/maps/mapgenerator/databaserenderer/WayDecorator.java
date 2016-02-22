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

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Paint;

final class WayDecorator {
	/**
	 * Minimum distance in pixels before the symbol is repeated.
	 */
	private static final int DISTANCE_BETWEEN_SYMBOLS = 200;

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

	/**
	 * Distance in pixels to skip from both ends of a segment.
	 */
	private static final int SEGMENT_SAFETY_DISTANCE = 30;

	static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol, float[][] coordinates,
			List<SymbolContainer> waySymbols) {
		int skipPixels = SEGMENT_SAFETY_DISTANCE;

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[0][1];

		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float symbolAngle;
		for (int i = 2; i < coordinates[0].length; i += 2) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			while (segmentLengthRemaining - skipPixels > SEGMENT_SAFETY_DISTANCE) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				symbolAngle = (float) Math.toDegrees(Math.atan2(currentY - previousY, currentX - previousX));

				waySymbols.add(new SymbolContainer(symbolBitmap, previousX, previousY, alignCenter, symbolAngle));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				skipPixels = DISTANCE_BETWEEN_SYMBOLS;
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < SEGMENT_SAFETY_DISTANCE) {
				skipPixels = SEGMENT_SAFETY_DISTANCE;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(String textKey, Paint paint, Paint outline, float[][] coordinates,
			List<WayTextContainer> wayNames) {
		// calculate the way name length plus some margin of safety
		float wayNameWidth = paint.measureText(textKey) + 10;

		int skipPixels = 0;

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[0][1];

		// find way segments long enough to draw the way name on them
		for (int i = 2; i < coordinates[0].length; i += 2) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {
				float[] wayNamePath = new float[4];
				// check to prevent inverted way names
				if (previousX <= currentX) {
					wayNamePath[0] = previousX;
					wayNamePath[1] = previousY;
					wayNamePath[2] = currentX;
					wayNamePath[3] = currentY;
				} else {
					wayNamePath[0] = currentX;
					wayNamePath[1] = currentY;
					wayNamePath[2] = previousX;
					wayNamePath[3] = previousY;
				}
				wayNames.add(new WayTextContainer(wayNamePath, textKey, paint));
				if (outline != null) {
					wayNames.add(new WayTextContainer(wayNamePath, textKey, outline));
				}

				skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	private WayDecorator() {
		throw new IllegalStateException();
	}
}
