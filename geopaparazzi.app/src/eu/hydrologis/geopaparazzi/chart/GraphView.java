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

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/**
 * GraphView 
 * 
 * Adapted from GraphView by Arno den Hond.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GraphView extends View {

    private float[] xValues;
    private float[] yValues;
    private String[] horlabels;
    private String[] verlabels;

    private ChartDrawer chartDrawer = null;

    public GraphView( Context context, float[] xValues, float[] yValues, String title, String[] horlabels, String[] verlabels,
            int type ) {
        super(context);
        if (yValues == null)
            this.yValues = new float[0];
        else {
            this.xValues = xValues;
            this.yValues = yValues;
        }
        if (title == null)
            title = ""; //$NON-NLS-1$
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;

        chartDrawer = new ChartDrawer(title, type);
    }

    public void setProperties( int axisColor, int axisAlpha, int labelsColor, int labelsAlpha, int chartColor, int chartAlpha,
            int chartPointColor, int chartPointAlpha, int backgroundColor, int backgroundAlpha ) {
        chartDrawer.setProperties(axisColor, axisAlpha, labelsColor, labelsAlpha, chartColor, chartAlpha, chartPointColor,
                chartPointAlpha, backgroundColor, backgroundAlpha);
    }
    
    @Override
    protected void onDraw( Canvas canvas ) {
        chartDrawer.drawCart(canvas, 0, 0, getWidth(), getHeight(), getMax(), getMin(), verlabels, horlabels, xValues, yValues,
                20);
    }

    private float getMax() {
        float largest = Integer.MIN_VALUE;
        for( int i = 0; i < yValues.length; i++ )
            if (yValues[i] > largest)
                largest = yValues[i];
        return largest;
    }

    private float getMin() {
        float smallest = Integer.MAX_VALUE;
        for( int i = 0; i < yValues.length; i++ )
            if (yValues[i] < smallest)
                smallest = yValues[i];
        return smallest;
    }

}
