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

import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * A CanvasRasterer uses a Canvas for drawing.
 * 
 * @see <a href="http://developer.android.com/reference/android/graphics/Canvas.html">Canvas</a>
 */
class CanvasRasterer {
	private static final Paint PAINT_BITMAP_FILTER = new Paint(Paint.FILTER_BITMAP_FLAG);
	private static final Paint PAINT_TILE_COORDINATES = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_COORDINATES_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_FRAME = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final float[] TILE_FRAME = new float[] { 0, 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE, Tile.TILE_SIZE,
			Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };

	private static void configurePaints() {
		PAINT_TILE_COORDINATES.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES.setTextSize(20);

		PAINT_TILE_COORDINATES_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_TILE_COORDINATES_STROKE.setStrokeWidth(5);
		PAINT_TILE_COORDINATES_STROKE.setTextSize(20);
		PAINT_TILE_COORDINATES_STROKE.setColor(Color.WHITE);
	}

	private final Canvas canvas;
	private final Path path;
	private final Matrix symbolMatrix;

	CanvasRasterer() {
		this.canvas = new Canvas();
		this.symbolMatrix = new Matrix();
		this.path = new Path();
		this.path.setFillType(Path.FillType.EVEN_ODD);
		configurePaints();
	}

	private void drawTileCoordinate(String string, int offsetY) {
		this.canvas.drawText(string, 20, offsetY, PAINT_TILE_COORDINATES_STROKE);
		this.canvas.drawText(string, 20, offsetY, PAINT_TILE_COORDINATES);
	}

	void drawNodes(List<PointTextContainer> pointTextContainers) {
		for (int index = pointTextContainers.size() - 1; index >= 0; --index) {
			PointTextContainer pointTextContainer = pointTextContainers.get(index);

			if (pointTextContainer.paintBack != null) {
				this.canvas.drawText(pointTextContainer.text, pointTextContainer.x, pointTextContainer.y,
						pointTextContainer.paintBack);
			}

			this.canvas.drawText(pointTextContainer.text, pointTextContainer.x, pointTextContainer.y,
					pointTextContainer.paintFront);
		}
	}

	void drawSymbols(List<SymbolContainer> symbolContainers) {
		for (int index = symbolContainers.size() - 1; index >= 0; --index) {
			SymbolContainer symbolContainer = symbolContainers.get(index);

			if (symbolContainer.alignCenter) {
				int pivotX = symbolContainer.symbol.getWidth() >> 1;
				int pivotY = symbolContainer.symbol.getHeight() >> 1;
				this.symbolMatrix.setRotate(symbolContainer.rotation, pivotX, pivotY);
				this.symbolMatrix.postTranslate(symbolContainer.x - pivotX, symbolContainer.y - pivotY);
			} else {
				this.symbolMatrix.setRotate(symbolContainer.rotation);
				this.symbolMatrix.postTranslate(symbolContainer.x, symbolContainer.y);
			}

			this.canvas.drawBitmap(symbolContainer.symbol, this.symbolMatrix, PAINT_BITMAP_FILTER);
		}
	}

	void drawTileCoordinates(Tile tile) {
		drawTileCoordinate("X: " + tile.tileX, 30);
		drawTileCoordinate("Y: " + tile.tileY, 60);
		drawTileCoordinate("Z: " + tile.zoomLevel, 90);
	}

	void drawTileFrame() {
		this.canvas.drawLines(TILE_FRAME, PAINT_TILE_FRAME);
	}

	void drawWayNames(List<WayTextContainer> wayTextContainers) {
		for (int index = wayTextContainers.size() - 1; index >= 0; --index) {
			WayTextContainer wayTextContainer = wayTextContainers.get(index);
			this.path.rewind();

			float[] textCoordinates = wayTextContainer.coordinates;
			this.path.moveTo(textCoordinates[0], textCoordinates[1]);
			for (int i = 2; i < textCoordinates.length; i += 2) {
				this.path.lineTo(textCoordinates[i], textCoordinates[i + 1]);
			}
			this.canvas.drawTextOnPath(wayTextContainer.text, this.path, 0, 3, wayTextContainer.paint);
		}
	}

	void drawWays(List<List<List<ShapePaintContainer>>> drawWays) {
		int levelsPerLayer = drawWays.get(0).size();

		for (int layer = 0, layers = drawWays.size(); layer < layers; ++layer) {
			List<List<ShapePaintContainer>> shapePaintContainers = drawWays.get(layer);

			for (int level = 0; level < levelsPerLayer; ++level) {
				List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

				for (int index = wayList.size() - 1; index >= 0; --index) {
					ShapePaintContainer shapePaintContainer = wayList.get(index);
					this.path.rewind();

					switch (shapePaintContainer.shapeContainer.getShapeType()) {
						case CIRCLE:
							CircleContainer circleContainer = (CircleContainer) shapePaintContainer.shapeContainer;
							this.path.addCircle(circleContainer.x, circleContainer.y, circleContainer.radius,
									Path.Direction.CCW);
							break;

						case WAY:
							WayContainer wayContainer = (WayContainer) shapePaintContainer.shapeContainer;
							float[][] coordinates = wayContainer.coordinates;
							for (int j = 0; j < coordinates.length; ++j) {
								// make sure that the coordinates sequence is not empty
								if (coordinates[j].length > 2) {
									this.path.moveTo(coordinates[j][0], coordinates[j][1]);
									for (int i = 2; i < coordinates[j].length; i += 2) {
										this.path.lineTo(coordinates[j][i], coordinates[j][i + 1]);
									}
								}
							}
							break;
					}
					this.canvas.drawPath(this.path, shapePaintContainer.paint);
				}
			}
		}
	}

	void fill(int color) {
		this.canvas.drawColor(color);
	}

	void setCanvasBitmap(Bitmap bitmap) {
		this.canvas.setBitmap(bitmap);
	}
}
