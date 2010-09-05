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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;

/**
 * Activity that creates a chartview.
 * 
 * Adapted from GraphViewDemo by Arno den Hond.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ChartActivity extends Activity {
    private DecimalFormat f = new DecimalFormat("0.0"); //$NON-NLS-1$

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                long logid = extras.getLong(Constants.ID);

                Line line = DaoGpsLog.getGpslogAsLine(this, logid);

                makeProfilePlot(line);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, "An error occurred while creating the chart.", Toast.LENGTH_LONG).show();
        }

    }

    // private void makeXYPlot( GpxItem item ) {
    // String[] verlabels = new String[]{f.format(item.getN()), f.format(item.getS())};
    // String[] horlabels = new String[]{f.format(item.getW()), f.format(item.getE())};
    //
    // List<PointF3D> points = item.read();
    // float[] pts = new float[points.size()];
    // for( int i = 0; i < pts.length; i++ ) {
    // PointF3D point = points.get(i);
    // pts[i] = point.y;
    // }
    // item.clear();
    //
    // GraphView graphView = new GraphView(this, null, pts, "XY View", horlabels, verlabels,
    // ChartDrawer.LINE);
    //
    // Paint back = new Paint();
    // back.setColor(Color.WHITE);
    // Paint chart = new Paint();
    // chart.setColor(Color.RED);
    // Paint labels = new Paint();
    // labels.setColor(Color.BLACK);
    // Paint axis = new Paint();
    // axis.setColor(Color.DKGRAY);
    // graphView.setProperties(axis, labels, chart, back);
    // setContentView(graphView);
    // }

    private void makeProfilePlot( Line line ) {
        List<Double> altims = line.getAltimList();
        float[] pts = new float[altims.size()];
        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;
        for( int i = 0; i < pts.length; i++ ) {
            float altim = altims.get(i).floatValue();
            if (altim > max) {
                max = altim;
            }
            if (altim < min) {
                min = altim;
            }
            pts[i] = altim;
        }
        altims.clear();
        String[] verlabels = new String[]{f.format(max), f.format(min)};
        String[] horlabels = new String[]{"0", "" + (int) line.getLength() + "[m]"};

        GraphView graphView = new GraphView(this, null, pts, getString(R.string.chart_profile_view), horlabels, verlabels,
                ChartDrawer.LINE);
        setContentView(graphView);
    }
}
