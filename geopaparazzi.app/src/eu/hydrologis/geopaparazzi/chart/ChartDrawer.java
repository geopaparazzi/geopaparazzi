/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.chart;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;

/**
 * A chart drawer for canvases.
 * 
 * Adapted from GraphView by Arno den Hond.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ChartDrawer {

    public static final int BAR = 0;
    public static final int LINE = 1;
    public static final int POINTS = 2;

    private int verticalAxisColor = Color.BLACK;
    private int verticalAxisAlpha = 255;
    private int verticalLabelsColor = Color.BLACK;
    private int verticalLabelsAlpha = 255;
    private int horizontalAxisColor = Color.BLACK;
    private int horizontalAxisAlpha = 255;
    private int horizontalLabelsColor = Color.BLACK;
    private int horizontalLabelsAlpha = 255;
    private int chartColor = Color.RED;
    private int chartAlpha = 255;
    private int chartPointColor = Color.RED;
    private int chartPointAlpha = 255;
    private int backgroundColor = Color.WHITE;
    private int backgroundAlpha = 255;

    private Paint paint = new Paint();

    private final String title;
    private final int type;

    public ChartDrawer( String title, int type ) {
        this.title = title;
        this.type = type;
    }

    public void setProperties( int axisColor, int axisAlpha, int labelsColor, int labelsAlpha, int chartColor, int chartAlpha,
            int chartPointColor, int chartPointAlpha, int backgroundColor, int backgroundAlpha ) {
        if (axisColor != Integer.MAX_VALUE) {
            verticalAxisColor = horizontalAxisColor = axisColor;
        }
        if (axisAlpha != Integer.MAX_VALUE) {
            verticalAxisAlpha = horizontalAxisAlpha = axisAlpha;
        }
        if (labelsColor != Integer.MAX_VALUE) {
            verticalLabelsColor = horizontalLabelsColor = labelsColor;
        }
        if (labelsAlpha != Integer.MAX_VALUE) {
            verticalLabelsAlpha = horizontalLabelsAlpha = labelsAlpha;
        }
        if (chartColor != Integer.MAX_VALUE) {
            this.chartColor = chartColor;
        }
        if (chartAlpha != Integer.MAX_VALUE) {
            this.chartAlpha = chartAlpha;
        }
        if (chartPointColor != Integer.MAX_VALUE) {
            this.chartPointColor = chartPointColor;
        }
        if (chartPointAlpha != Integer.MAX_VALUE) {
            this.chartPointAlpha = chartPointAlpha;
        }
        if (backgroundColor != Integer.MAX_VALUE) {
            this.backgroundColor = backgroundColor;
        }
        if (backgroundAlpha != Integer.MAX_VALUE) {
            this.backgroundAlpha = backgroundAlpha;
        }
    }

    public void drawCart( Canvas canvas, float x, float y, float width, float height, float max, float min, String[] verlabels,
            String[] horlabels, float[] xValues, float[] yValues, float border ) {
        paint.setAntiAlias(true);

        RectF rect = new RectF(x, y, x + width, y + height);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        paint.setAlpha(backgroundAlpha);
        canvas.drawRect(rect, paint);
        int alpha = paint.getAlpha();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(255);
        canvas.drawRect(rect, paint);
        paint.setAlpha(alpha);

        float horstart = border * 2;
        width = width - 1;
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);

        paint.setTextAlign(Align.LEFT);
        int vers = verlabels.length - 1;
        for( int i = 0; i < verlabels.length; i++ ) {
            float tmpy = y + ((graphheight / vers) * i) + border;
            paint.setColor(horizontalAxisColor);
            paint.setAlpha(horizontalAxisAlpha);
            canvas.drawLine(x + horstart, tmpy, x + width, tmpy, paint);
            paint.setColor(verticalLabelsColor);
            paint.setAlpha(verticalLabelsAlpha);
            canvas.drawText(verlabels[i], 0, tmpy, paint);
        }
        int hors = horlabels.length - 1;
        for( int i = 0; i < horlabels.length; i++ ) {
            float tmpx = x + ((graphwidth / hors) * i) + horstart;
            paint.setColor(verticalAxisColor);
            paint.setAlpha(verticalAxisAlpha);
            canvas.drawLine(tmpx, y + height - border, tmpx, y + border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i == horlabels.length - 1)
                paint.setTextAlign(Align.RIGHT);
            if (i == 0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(horizontalLabelsColor);
            paint.setAlpha(horizontalLabelsAlpha);
            canvas.drawText(horlabels[i], tmpx, y + height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        if (max != min) {
            float datalength = yValues.length;
            switch( type ) {
            case BAR:
                paint.setColor(chartColor);
                paint.setAlpha(chartAlpha);
                float colwidtBars = (width - (2 * border)) / datalength;
                for( int i = 0; i < yValues.length; i++ ) {
                    float val = yValues[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    canvas.drawRect((i * colwidtBars) + horstart, (border - h) + graphheight, ((i * colwidtBars) + horstart)
                            + (colwidtBars - 1), height - (border - 1), paint);
                }

                break;

            case LINE:

                float colwidthLines = (width - (2 * border)) / datalength;
                float halfcol = colwidthLines / 2;
                float lasth = 0;
                for( int i = 0; i < yValues.length; i++ ) {
                    float val = yValues[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0) {
                        float startX = ((i - 1) * colwidthLines) + (horstart + 1) + halfcol;
                        float startY = y + (border - lasth) + graphheight;
                        float stopX = (i * colwidthLines) + (horstart + 1) + halfcol;
                        float stopY = y + (border - h) + graphheight;

                        paint.setColor(chartColor);
                        paint.setAlpha(chartAlpha);
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(startX, startY, stopX, stopY, paint);
                        float origWidth = paint.getStrokeWidth();
                        if (chartPointColor != Integer.MAX_VALUE) {
                            paint.setColor(chartPointColor);
                            paint.setAlpha(chartPointAlpha);
                            paint.setStyle(Paint.Style.FILL);
                            paint.setStrokeWidth(3);
                            if (i == 1) {
                                canvas.drawPoint(startX, startY, paint);
                            }
                            canvas.drawPoint(stopX, stopY, paint);
                        }
                        paint.setStrokeWidth(origWidth);

                    }
                    lasth = h;
                }

                break;

            default:
                break;
            }
        }

    }
}
