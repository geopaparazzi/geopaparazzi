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

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYLegendWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;

/**
 * The simplest possible example of using AndroidPlot to plot some data.
 */
public class ProfileChartActivity extends Activity implements View.OnTouchListener, View.OnLongClickListener {

    private XYPlot xyPlotSpeed, xyPlotElev;
    private Boolean series2_onRight = true;
    private LineAndPointFormatter seriesSpeedFormat, seriesElevFormat;

    XYSeries seriesSpeed, seriesElev;
    private Line line;
    private ProgressDialog progressDialog;
    private PointF minXYSpeed;
    private PointF maxXYSpeed;
    private PointF minXYElevation;
    private PointF maxXYElevation;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.profilechart);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                long logid = extras.getLong(Constants.ID);

                line = DaoGpsLog.getGpslogAsLine(logid, -1);

            } catch (IOException e) {
                GPLog.error(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, R.string.an_error_occurred_while_creating_the_chart_, Toast.LENGTH_LONG).show();
        }

        final float f26 = PixelUtils.dpToPix(26);
        final float f10 = PixelUtils.dpToPix(10);
        final SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL);

        xyPlotSpeed = (XYPlot) findViewById(R.id.mySimpleXYPlot_L);
        xyPlotElev = (XYPlot) findViewById(R.id.mySimpleXYPlot_R);

        xyPlotSpeed.setOnTouchListener(this);
        xyPlotSpeed.setOnLongClickListener(this);

        // Disable Hardware Acceleration on the xyPlot view object.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            xyPlotSpeed.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            xyPlotElev.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        /*
         * Setup the Plots
         */
        xyPlotSpeed.setPlotMargins(0, 0, 0, 0);
        xyPlotElev.setPlotMargins(0, 0, 0, 0);

        xyPlotSpeed.setPlotPadding(0, 0, 0, 0);
        xyPlotElev.setPlotPadding(0, 0, 0, 0);

        xyPlotElev.getDomainLabelWidget().setVisible(false);
        xyPlotElev.getRangeLabelWidget().setVisible(false);
        xyPlotElev.getTitleWidget().setVisible(false);

        xyPlotElev.setBorderPaint(null);
        xyPlotElev.setBackgroundPaint(null);

        /* 
         * Setup the Graph Widgets
         */
        XYGraphWidget graphWidget_L = xyPlotSpeed.getGraphWidget();
        XYGraphWidget graphWidget_R = xyPlotElev.getGraphWidget();

        graphWidget_L.setSize(sm);
        graphWidget_R.setSize(sm);

        graphWidget_L.setMargins(0, 0, 0, 0);
        graphWidget_R.setMargins(0, 0, 0, 0);

        graphWidget_L.setPadding(f26, f10, f26, f26);
        graphWidget_R.setPadding(f26, f10, f26, f26);

        graphWidget_L.setRangeAxisPosition(true, false, 4, "10");
        graphWidget_R.setRangeAxisPosition(false, false, 4, "10");

        graphWidget_L.setRangeLabelVerticalOffset(-3);
        graphWidget_R.setRangeLabelVerticalOffset(-3);

        graphWidget_L.setRangeOriginLabelPaint(null);
        graphWidget_R.setRangeOriginLabelPaint(null);

        graphWidget_L.setRangeLabelWidth(0);
        graphWidget_R.setRangeLabelWidth(0);

        graphWidget_L.setDomainLabelWidth(0);
        graphWidget_R.setDomainLabelWidth(0);

        graphWidget_R.setBackgroundPaint(null);
        graphWidget_R.setDomainLabelPaint(null);
        graphWidget_R.setGridBackgroundPaint(null);
        graphWidget_R.setDomainOriginLabelPaint(null);
        graphWidget_R.setRangeOriginLinePaint(null);
        graphWidget_R.setDomainGridLinePaint(null);
        graphWidget_R.setRangeGridLinePaint(null);

        graphWidget_L.getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(8));
        graphWidget_R.getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(8));

        graphWidget_L.getDomainOriginLabelPaint().setTextSize(PixelUtils.dpToPix(8));
        graphWidget_L.getDomainLabelPaint().setTextSize(PixelUtils.dpToPix(8));

        float textSize = graphWidget_L.getRangeLabelPaint().getTextSize();
        graphWidget_L.setRangeLabelVerticalOffset((textSize / 2) * -1);
        graphWidget_R.setRangeLabelVerticalOffset(graphWidget_L.getRangeLabelVerticalOffset());

        /*
         * Position the Graph Widgets in the Centre
         */
        graphWidget_L.position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.CENTER);
        graphWidget_R.position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.CENTER);

        /* 
         * Position the Label Widgets
         */
        xyPlotSpeed.getDomainLabelWidget().setWidth(100);
        xyPlotSpeed.getRangeLabelWidget().setWidth(100);
        xyPlotSpeed.getDomainLabelWidget().position(0, XLayoutStyle.RELATIVE_TO_CENTER, 1, YLayoutStyle.ABSOLUTE_FROM_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
        xyPlotSpeed.getRangeLabelWidget().position(1, XLayoutStyle.ABSOLUTE_FROM_LEFT, -20, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.LEFT_BOTTOM);

        /*
         *  Setup and Position the LEFT Legend
         */
        XYLegendWidget legendWidget_LEFT = xyPlotSpeed.getLegendWidget();
        legendWidget_LEFT.setSize(new SizeMetrics(100, SizeLayoutType.ABSOLUTE, 200, SizeLayoutType.ABSOLUTE));
        legendWidget_LEFT.setPadding(1, 1, 1, 1);
        legendWidget_LEFT.setTableModel(new DynamicTableModel(1, 3));
        legendWidget_LEFT.setIconSizeMetrics(new SizeMetrics(PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE, PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE));
        legendWidget_LEFT.getTextPaint().setTextSize(PixelUtils.dpToPix(9));
        legendWidget_LEFT.position(PixelUtils.dpToPix(30), XLayoutStyle.ABSOLUTE_FROM_LEFT, f10 + 2, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.LEFT_TOP);

       
        /*
         *  Setup and Position the RIGHT Legend
         */
        XYLegendWidget legendWidget_RIGHT = xyPlotElev.getLegendWidget();
        legendWidget_RIGHT.setSize(new SizeMetrics(100, SizeLayoutType.ABSOLUTE, 200, SizeLayoutType.ABSOLUTE));
        legendWidget_RIGHT.setPadding(1, 1, 1, 1);
        legendWidget_RIGHT.setTableModel(new DynamicTableModel(1, 3));
        legendWidget_RIGHT.setIconSizeMetrics(new SizeMetrics(PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE, PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE));
        legendWidget_RIGHT.getTextPaint().setTextSize(PixelUtils.dpToPix(9));
        legendWidget_RIGHT.getTextPaint().setTextAlign(Align.RIGHT);
        legendWidget_RIGHT.setMarginLeft(185);
        legendWidget_RIGHT.position(PixelUtils.dpToPix(30), XLayoutStyle.ABSOLUTE_FROM_RIGHT, f10 + 2, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.RIGHT_TOP);


        // Setup the formatters
        int rgbSpeedLine = Color.rgb(0, 200, 0);
        int rgbSpeedPoints = Color.rgb(0, 100, 0);
        int rgbElevLine = Color.rgb(0, 0, 200);
        int rgbElevPoints = Color.rgb(0, 0, 100);

        seriesSpeedFormat = new LineAndPointFormatter(rgbSpeedLine, rgbSpeedPoints, null, null);
        seriesElevFormat = new LineAndPointFormatter(rgbElevLine, rgbElevPoints, null, null);


    }

    @Override
    protected void onResume() {
        super.onResume();

        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading_data));

        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                try {
                    createDatasetFromProfile();
                } catch (Exception e) {
                    return "ERROR";
                }
                return "OK";
            }

            protected void onPostExecute(String msg) {
                if (msg.equals("OK")) {
                    updateView();
                }
                Utilities.dismissProgressDialog(progressDialog);
            }
        }.execute((String) null);


    }

    private void updateView() {

        // Remove all current series from each plot
        Iterator<XYSeries> iterator1 = xyPlotSpeed.getSeriesSet().iterator();
        while (iterator1.hasNext()) {
            XYSeries setElement = iterator1.next();
            xyPlotSpeed.removeSeries(setElement);
        }
        Iterator<XYSeries> iterator2 = xyPlotElev.getSeriesSet().iterator();
        while (iterator2.hasNext()) {
            XYSeries setElement = iterator2.next();
            xyPlotElev.removeSeries(setElement);
        }

        // Add series to each plot as needed.
        xyPlotSpeed.addSeries(seriesSpeed, seriesSpeedFormat);
        if (series2_onRight) {
            xyPlotElev.addSeries(seriesElev, seriesElevFormat);
        } else {
            xyPlotSpeed.addSeries(seriesElev, seriesElevFormat);
        }

        // Finalise each Plot based on whether they have any series or not.
        if (!xyPlotElev.getSeriesSet().isEmpty()) {
            xyPlotElev.setVisibility(XYPlot.VISIBLE);
            xyPlotElev.redraw();
        } else {
            xyPlotElev.setVisibility(XYPlot.INVISIBLE);
        }

        if (!xyPlotSpeed.getSeriesSet().isEmpty()) {
            xyPlotSpeed.setVisibility(XYPlot.VISIBLE);
            xyPlotSpeed.redraw();
        } else {
            xyPlotSpeed.setVisibility(XYPlot.INVISIBLE);
        }

        xyPlotSpeed.calculateMinMaxVals();
        minXYSpeed = new PointF(xyPlotSpeed.getCalculatedMinX().floatValue(),
                xyPlotSpeed.getCalculatedMinY().floatValue());
        maxXYSpeed = new PointF(xyPlotSpeed.getCalculatedMaxX().floatValue(),
                xyPlotSpeed.getCalculatedMaxY().floatValue());

        xyPlotElev.calculateMinMaxVals();
        minXYElevation = new PointF(xyPlotElev.getCalculatedMinX().floatValue(),
                xyPlotElev.getCalculatedMinY().floatValue());
        maxXYElevation = new PointF(xyPlotElev.getCalculatedMaxX().floatValue(),
                xyPlotElev.getCalculatedMaxY().floatValue());

    }

    /**
     * Create a dataset based on supplied data that are supposed to be coordinates and elevations for a profile view.
     * <p/>
     * <p>
     * Note that this also sets the min and max values
     * of the chart data, so the dataset created should
     * really be used through setDataset, so that the bounds
     * are properly zoomed.
     * </p>
     */
    public void createDatasetFromProfile() throws Exception {
        DynamicDoubleArray lonArray = line.getLonList();
        DynamicDoubleArray latArray = line.getLatList();
        DynamicDoubleArray elevArray = line.getAltimList();
        List<String> dates = line.getDateList();

        double previousLat = 0;
        double previousLon = 0;
        double previousElev = 0;
        double summedDistance = 0.0;
        long previousTime = 0;

        List<Double> xList1 = new ArrayList<Double>(lonArray.size());
        List<Double> yList1 = new ArrayList<Double>(lonArray.size());
        List<Double> yList2 = new ArrayList<Double>(lonArray.size());
        double elevDifference = 0;
        for (int i = 0; i < lonArray.size(); i++) {
            double elev = elevArray.get(i);
            double lat = latArray.get(i);
            double lon = lonArray.get(i);
            String dateStr = dates.get(i);
            long time = Long.parseLong(dateStr);

            double distance = 0.0;
            if (i > 0) {
                Location thisLoc = new Location("dummy1"); //$NON-NLS-1$
                thisLoc.setLongitude(lon);
                thisLoc.setLatitude(lat);
                Location thatLoc = new Location("dummy2"); //$NON-NLS-1$
                thatLoc.setLongitude(previousLon);
                thatLoc.setLatitude(previousLat);
                distance = thisLoc.distanceTo(thatLoc);

                double diff = elev - previousElev;
                if (diff > 0)
                    elevDifference = elevDifference + diff;


                double timeSeconds = (time - previousTime) / 1000.0;
                double speed = Math.sqrt(diff * diff + distance * distance) / timeSeconds;

                yList2.add(speed);
            } else {
                yList2.add(0.0);
            }

            previousLat = lat;
            previousLon = lon;
            previousElev = elev;
            previousTime = time;

            summedDistance = summedDistance + distance;

            xList1.add(summedDistance);
            yList1.add(elev);
        }

        // Setup the Series
        seriesElev = new SimpleXYSeries(xList1, yList1, "Elev [m]");
        seriesSpeed = new SimpleXYSeries(xList1, yList2, "Speed [m/s]");
    }

    // Definition of the touch states
    static final int NONE = 0;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    int mode = NONE;

    PointF firstFinger;
    float distBetweenFingers;
    boolean stopThread = false;

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                stopThread = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingers = spacing(event);
                // the distance check is done to avoid false alarms
                if (distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    scrollElev(oldFirstFinger.x - firstFinger.x);
                    scrollSpeed(oldFirstFinger.x - firstFinger.x);
                    xyPlotSpeed.setDomainBoundaries(minXYSpeed.x, maxXYSpeed.x,
                            BoundaryMode.FIXED);
                    xyPlotElev.setDomainBoundaries(minXYElevation.x, maxXYElevation.x,
                            BoundaryMode.FIXED);
                    xyPlotSpeed.redraw();
                    xyPlotElev.redraw();

                } else if (mode == TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    zoomElev(oldDist / distBetweenFingers);
                    zoomSpeed(oldDist / distBetweenFingers);
                    xyPlotSpeed.setDomainBoundaries(minXYSpeed.x, maxXYSpeed.x,
                            BoundaryMode.FIXED);
                    xyPlotElev.setDomainBoundaries(minXYElevation.x, maxXYElevation.x,
                            BoundaryMode.FIXED);
                    xyPlotSpeed.redraw();
                    xyPlotElev.redraw();
                }
                break;
        }
        return true;
    }


    private void zoomElev(float scale) {
        float domainSpan = maxXYSpeed.x - minXYSpeed.x;
        float domainMidPoint = maxXYSpeed.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;

        minXYSpeed.x = domainMidPoint - offset;
        maxXYSpeed.x = domainMidPoint + offset;

        minXYSpeed.x = Math.min(minXYSpeed.x, seriesSpeed.getX(seriesSpeed.size() - 3)
                .floatValue());
        maxXYSpeed.x = Math.max(maxXYSpeed.x, seriesSpeed.getX(1).floatValue());
        clampToDomainBoundsElev(domainSpan);
    }

    private void zoomSpeed(float scale) {
        float domainSpan = maxXYElevation.x - minXYElevation.x;
        float domainMidPoint = maxXYElevation.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;

        minXYElevation.x = domainMidPoint - offset;
        maxXYElevation.x = domainMidPoint + offset;

        minXYElevation.x = Math.min(minXYElevation.x, seriesElev.getX(seriesElev.size() - 3)
                .floatValue());
        maxXYElevation.x = Math.max(maxXYElevation.x, seriesElev.getX(1).floatValue());
        clampToDomainBoundsSpeed(domainSpan);
    }

    private void scrollElev(float pan) {
        float domainSpan = maxXYSpeed.x - minXYSpeed.x;
        float step = domainSpan / xyPlotSpeed.getWidth();
        float offset = pan * step;
        minXYSpeed.x = minXYSpeed.x + offset;
        maxXYSpeed.x = maxXYSpeed.x + offset;
        clampToDomainBoundsElev(domainSpan);
    }

    private void scrollSpeed(float pan) {
        float domainSpan = maxXYElevation.x - minXYElevation.x;
        float step = domainSpan / xyPlotElev.getWidth();
        float offset = pan * step;
        minXYElevation.x = minXYElevation.x + offset;
        maxXYElevation.x = maxXYElevation.x + offset;
        clampToDomainBoundsSpeed(domainSpan);
    }

    private void clampToDomainBoundsElev(float domainSpan) {
        float leftBoundary = seriesSpeed.getX(0).floatValue();
        float rightBoundary = seriesSpeed.getX(seriesSpeed.size() - 1).floatValue();
        // enforce left scroll boundary:
        if (minXYSpeed.x < leftBoundary) {
            minXYSpeed.x = leftBoundary;
            maxXYSpeed.x = leftBoundary + domainSpan;
        } else if (maxXYSpeed.x > seriesSpeed.getX(seriesSpeed.size() - 1).floatValue()) {
            maxXYSpeed.x = rightBoundary;
            minXYSpeed.x = rightBoundary - domainSpan;
        }
    }

    private void clampToDomainBoundsSpeed(float domainSpan) {
        float leftBoundary = seriesElev.getX(0).floatValue();
        float rightBoundary = seriesElev.getX(seriesElev.size() - 1).floatValue();
        // enforce left scroll boundary:
        if (minXYElevation.x < leftBoundary) {
            minXYElevation.x = leftBoundary;
            maxXYElevation.x = leftBoundary + domainSpan;
        } else if (maxXYElevation.x > seriesElev.getX(seriesElev.size() - 1).floatValue()) {
            maxXYElevation.x = rightBoundary;
            minXYElevation.x = rightBoundary - domainSpan;
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    @Override
    public boolean onLongClick(View v) {
        minXYSpeed.x = seriesSpeed.getX(0).floatValue();
        maxXYSpeed.x = seriesSpeed.getX(seriesSpeed.size() - 1).floatValue();
        xyPlotSpeed.setDomainBoundaries(minXYSpeed.x, maxXYSpeed.x, BoundaryMode.FIXED);
        minXYElevation.x = seriesElev.getX(0).floatValue();
        maxXYElevation.x = seriesElev.getX(seriesElev.size() - 1).floatValue();
        xyPlotElev.setDomainBoundaries(minXYElevation.x, maxXYSpeed.x, BoundaryMode.FIXED);

        xyPlotElev.redraw();
        xyPlotSpeed.redraw();
        return true;
    }
}
