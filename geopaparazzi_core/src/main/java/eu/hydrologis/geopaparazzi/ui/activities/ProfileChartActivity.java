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

package eu.hydrologis.geopaparazzi.ui.activities;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

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
import com.vividsolutions.jts.geom.Coordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.objects.Line;
import eu.hydrologis.geopaparazzi.utilities.Constants;
import eu.hydrologis.geopaparazzi.utilities.FeatureSlidingAverage;

/**
 * The profile chart activity.
 *
 * @author Andrea Antonello
 */
public class ProfileChartActivity extends Activity implements View.OnTouchListener, View.OnClickListener {

    private XYPlot xyPlotSpeed, xyPlotElev;
    private LineAndPointFormatter seriesSpeedFormat, seriesElevFormat;

    XYSeries seriesSpeed, seriesElev;
    private Line line;
    private PointF minXYSpeed;
    private PointF maxXYSpeed;
    private PointF minXYElevation;
    private PointF maxXYElevation;
    private TextView infoTextView;
    private double elevDifference;
    private FloatingActionButton resetButton;
    private View textLayout;
    private StringAsyncTask resumeTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilechart);

        int backgroundColor = Compat.getColor(this, R.color.main_background);

        // colors
        int rgbSpeedLine = Color.rgb(0, 200, 0);
        int rgbSpeedPoints = Color.rgb(0, 100, 0);
        int rgbElevLine = Color.rgb(0, 0, 200);
        int rgbElevPoints = Color.rgb(0, 0, 100);

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
            GPDialogs.warningDialog(this, getString(R.string.an_error_occurred_while_creating_the_chart_), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            return;
        }

        final float f26 = PixelUtils.dpToPix(26);
        final float f10 = PixelUtils.dpToPix(10);
        final SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL);

        xyPlotSpeed = (XYPlot) findViewById(R.id.speed_plot);
        xyPlotElev = (XYPlot) findViewById(R.id.elevation_plot);
        xyPlotSpeed.setOnTouchListener(this);

        textLayout = findViewById(R.id.info_text_layout);
        textLayout.setVisibility(View.GONE);
        infoTextView = (TextView) findViewById(R.id.info_text);
        resetButton = (FloatingActionButton) findViewById(R.id.reset_chart_button);
        resetButton.setOnClickListener(this);
        resetButton.hide();

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
        XYGraphWidget graphWidgetSpeed = xyPlotSpeed.getGraphWidget();
        XYGraphWidget graphWidgetElev = xyPlotElev.getGraphWidget();

        graphWidgetSpeed.setSize(sm);
        graphWidgetElev.setSize(sm);

        graphWidgetSpeed.setMargins(0, 0, 0, 0);
        graphWidgetElev.setMargins(0, 0, 0, 0);

        graphWidgetSpeed.setPadding(f26, f10, f26, f26);
        graphWidgetElev.setPadding(f26, f10, f26, f26);

        graphWidgetSpeed.setRangeAxisPosition(true, false, 4, "10");
        graphWidgetElev.setRangeAxisPosition(false, false, 4, "10");

        graphWidgetSpeed.setRangeLabelVerticalOffset(-3);
        graphWidgetElev.setRangeLabelVerticalOffset(-3);

        graphWidgetSpeed.setRangeOriginLabelPaint(null);
        graphWidgetElev.setRangeOriginLabelPaint(null);

        graphWidgetSpeed.setRangeLabelWidth(0);
        graphWidgetElev.setRangeLabelWidth(0);

        graphWidgetSpeed.setDomainLabelWidth(0);
        graphWidgetElev.setDomainLabelWidth(0);

        graphWidgetElev.setBackgroundPaint(null);
        graphWidgetElev.setDomainLabelPaint(null);
        graphWidgetElev.setGridBackgroundPaint(null);
        graphWidgetElev.setDomainOriginLabelPaint(null);
        graphWidgetElev.setRangeOriginLinePaint(null);
        graphWidgetElev.setDomainGridLinePaint(null);
        graphWidgetElev.setRangeGridLinePaint(null);


        graphWidgetSpeed.getBackgroundPaint().setColor(backgroundColor);
        graphWidgetSpeed.getGridBackgroundPaint().setColor(backgroundColor);

        graphWidgetSpeed.getRangeOriginLinePaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getRangeOriginLinePaint().setStrokeWidth(3f);
        graphWidgetSpeed.getDomainOriginLinePaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getDomainOriginLinePaint().setStrokeWidth(3f);

        graphWidgetSpeed.getRangeGridLinePaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getRangeGridLinePaint().setStrokeWidth(1f);
        graphWidgetSpeed.getDomainGridLinePaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getDomainGridLinePaint().setStrokeWidth(1f);

        graphWidgetSpeed.getRangeLabelPaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getDomainLabelPaint().setColor(rgbSpeedPoints);
        graphWidgetSpeed.getDomainOriginLabelPaint().setColor(rgbSpeedPoints);
        Paint rangeOriginLabelPaint = graphWidgetSpeed.getRangeOriginLabelPaint();
        if (rangeOriginLabelPaint == null) {
            rangeOriginLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rangeOriginLabelPaint.setStyle(Paint.Style.STROKE);
            graphWidgetSpeed.setRangeOriginLabelPaint(rangeOriginLabelPaint);
        }
        rangeOriginLabelPaint.setColor(rgbSpeedPoints);

        graphWidgetElev.getRangeLabelPaint().setColor(rgbElevPoints);
        Paint rangeOriginLabelPaint2 = graphWidgetElev.getRangeOriginLabelPaint();
        if (rangeOriginLabelPaint2 == null) {
            rangeOriginLabelPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            rangeOriginLabelPaint2.setStyle(Paint.Style.STROKE);
            graphWidgetElev.setRangeOriginLabelPaint(rangeOriginLabelPaint2);
        }
        rangeOriginLabelPaint2.setColor(rgbElevPoints);


        graphWidgetSpeed.getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(8));
        graphWidgetElev.getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(8));

        graphWidgetSpeed.getDomainOriginLabelPaint().setTextSize(PixelUtils.dpToPix(8));
        graphWidgetSpeed.getDomainLabelPaint().setTextSize(PixelUtils.dpToPix(8));

        float textSize = graphWidgetSpeed.getRangeLabelPaint().getTextSize();
        graphWidgetSpeed.setRangeLabelVerticalOffset((textSize / 2) * -1);
        graphWidgetElev.setRangeLabelVerticalOffset(graphWidgetSpeed.getRangeLabelVerticalOffset());

        /*
         * Position the Graph Widgets in the Centre
         */
        graphWidgetSpeed.position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.CENTER);
        graphWidgetElev.position(0, XLayoutStyle.ABSOLUTE_FROM_CENTER, 0, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.CENTER);

        /* 
         * Position the Label Widgets
         */
        xyPlotSpeed.getDomainLabelWidget().setWidth(100);
        xyPlotSpeed.getRangeLabelWidget().setWidth(100);
        xyPlotSpeed.getDomainLabelWidget().position(0, XLayoutStyle.RELATIVE_TO_CENTER, 1, YLayoutStyle.ABSOLUTE_FROM_BOTTOM, AnchorPosition.BOTTOM_MIDDLE);
        xyPlotSpeed.getRangeLabelWidget().position(1, XLayoutStyle.ABSOLUTE_FROM_LEFT, -20, YLayoutStyle.ABSOLUTE_FROM_CENTER, AnchorPosition.LEFT_BOTTOM);



        /*
         *  Setup and Position the speed Legend
         */
        XYLegendWidget legendWidgetSpeed = xyPlotSpeed.getLegendWidget();
        legendWidgetSpeed.setSize(new SizeMetrics(100, SizeLayoutType.ABSOLUTE, 200, SizeLayoutType.ABSOLUTE));
        legendWidgetSpeed.setPadding(1, 1, 1, 1);
        legendWidgetSpeed.setTableModel(new DynamicTableModel(1, 3));
        legendWidgetSpeed.setIconSizeMetrics(new SizeMetrics(PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE, PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE));
        legendWidgetSpeed.getTextPaint().setColor(rgbSpeedPoints);
        legendWidgetSpeed.getTextPaint().setTextSize(PixelUtils.dpToPix(9));
        legendWidgetSpeed.position(PixelUtils.dpToPix(30), XLayoutStyle.ABSOLUTE_FROM_LEFT, f10 + 2, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.LEFT_TOP);

       
        /*
         *  Setup and Position the elev Legend
         */
        XYLegendWidget legendWidgetElev = xyPlotElev.getLegendWidget();
        legendWidgetElev.setSize(new SizeMetrics(100, SizeLayoutType.ABSOLUTE, 200, SizeLayoutType.ABSOLUTE));
        legendWidgetElev.setPadding(1, 1, 1, 1);
        legendWidgetElev.setTableModel(new DynamicTableModel(1, 3));
        legendWidgetElev.setIconSizeMetrics(new SizeMetrics(PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE, PixelUtils.dpToPix(10), SizeLayoutType.ABSOLUTE));
        legendWidgetElev.getTextPaint().setTextSize(PixelUtils.dpToPix(9));
        legendWidgetElev.getTextPaint().setColor(rgbElevPoints);
        legendWidgetElev.getTextPaint().setTextAlign(Align.RIGHT);
        legendWidgetElev.setMarginLeft(185);
        legendWidgetElev.position(PixelUtils.dpToPix(30), XLayoutStyle.ABSOLUTE_FROM_RIGHT, f10 + 2, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.RIGHT_TOP);


        // Setup the formatters
        seriesSpeedFormat = new LineAndPointFormatter(rgbSpeedLine, rgbSpeedPoints, null, null);
        seriesElevFormat = new LineAndPointFormatter(rgbElevLine, rgbElevPoints, null, null);


    }

    @Override
    protected void onResume() {
        super.onResume();
        //$NON-NLS-1$
        resumeTask = new StringAsyncTask(this) {
            protected String doBackgroundWork() {
                try {
                    createDatasetFromProfile();
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                    return "ERROR";
                }
                return "";
            }

            protected void doUiPostWork(String response) {
                dispose();
                if (response.length() != 0) {
                    GPDialogs.warningDialog(ProfileChartActivity.this, response, null);
                } else {
                    updateView();
                }
            }
        };
        resumeTask.setProgressDialog("", "Loading data...", false, null);
        resumeTask.execute();
    }

    @Override
    protected void onDestroy() {
        if (resumeTask!= null) resumeTask.dispose();
        super.onDestroy();
    }

    private void updateView() {

        // Remove all current series from each plot
        for (XYSeries setElement : xyPlotSpeed.getSeriesSet()) {
            xyPlotSpeed.removeSeries(setElement);
        }
        for (XYSeries setElement : xyPlotElev.getSeriesSet()) {
            xyPlotElev.removeSeries(setElement);
        }

        // Add series to each plot as needed.
        xyPlotSpeed.addSeries(seriesSpeed, seriesSpeedFormat);
        xyPlotElev.addSeries(seriesElev, seriesElevFormat);

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


        infoTextView.setText(getString(R.string.active_elevation_diff) + (int) elevDifference + "m");
    }

    /**
     * Create a dataset based on supplied data that are supposed to be coordinates and elevations for a profile view.
     */
    public void createDatasetFromProfile() throws Exception {
        DynamicDoubleArray lonArray = line.getLonList();
        DynamicDoubleArray latArray = line.getLatList();
        DynamicDoubleArray elevArray = line.getAltimList();
        List<String> dates = line.getDateList();

        double previousLat = 0;
        double previousLon = 0;
        double summedDistance = 0.0;
        long previousTime = 0;

        List<Coordinate> elevList = new ArrayList<>(lonArray.size());
        List<Coordinate> speedList = new ArrayList<>(lonArray.size());

        for (int i = 0; i < lonArray.size(); i++) {
            double elev = elevArray.get(i);
            double lat = latArray.get(i);
            double lon = lonArray.get(i);
            String dateStr = dates.get(i);
            long time = Long.parseLong(dateStr);

            double distance = 0.0;
            double speedKmH = 0.0;
            if (i > 0) {
                Location thisLoc = new Location("dummy1"); //$NON-NLS-1$
                thisLoc.setLongitude(lon);
                thisLoc.setLatitude(lat);
                Location thatLoc = new Location("dummy2"); //$NON-NLS-1$
                thatLoc.setLongitude(previousLon);
                thatLoc.setLatitude(previousLat);
                distance = thisLoc.distanceTo(thatLoc);

                double timeSeconds = (time - previousTime) / 1000.0;
                speedKmH = 3.6 * distance / timeSeconds;
            }

            previousLat = lat;
            previousLon = lon;
            previousTime = time;

            summedDistance = summedDistance + distance;

            elevList.add(new Coordinate(summedDistance, elev));
            speedList.add(new Coordinate(summedDistance, speedKmH));
        }

        int lookAhead = 20;
        double slide = 1;
        FeatureSlidingAverage fsaElev = new FeatureSlidingAverage(elevList);
        List<Coordinate> smoothedElev = fsaElev.smooth(lookAhead, false, slide);
        FeatureSlidingAverage fsaSpeed = new FeatureSlidingAverage(speedList);
        List<Coordinate> smoothedSpeed = fsaSpeed.smooth(lookAhead, false, slide);

        int size = lonArray.size();
        List<Double> finalYList1 = new ArrayList<>(size);
        List<Double> finalYList2 = new ArrayList<>(lonArray.size());
        List<Double> finalXList1 = new ArrayList<>(lonArray.size());

        elevDifference = 0;
        double previousElev = 0;
        for (int i = 0; i < size; i++) {
            Coordinate elev = smoothedElev.get(i);
            Coordinate speed = smoothedSpeed.get(i);

            finalXList1.add(elev.x);
            finalYList1.add(elev.y);
            finalYList2.add(speed.y);

            if (i > 0) {
                double diff = elev.y - previousElev;
                if (diff > 0)
                    elevDifference = elevDifference + diff;
            }
            previousElev = elev.y;
        }

        // Setup the Series
        seriesElev = new SimpleXYSeries(finalXList1, finalYList1, "Elev [m]");
        seriesSpeed = new SimpleXYSeries(finalXList1, finalYList2, "Speed [km/h]");
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
        try {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: // Start gesture
                    textLayout.setVisibility(View.VISIBLE);
                    firstFinger = new PointF(event.getX(), event.getY());
                    mode = ONE_FINGER_DRAG;
                    stopThread = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    textLayout.setVisibility(View.GONE);

                    PointF upPoint = new PointF(event.getX(), event.getY());
                    if (!firstFinger.equals(upPoint.x, upPoint.y))
                        resetButton.show();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN: // second finger
                    distBetweenFingers = spacing(event);
                    // the distance check is done to avoid false alarms
                    if (distBetweenFingers > 5f) {
                        mode = TWO_FINGERS_DRAG;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    textLayout.setVisibility(View.GONE);
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
        } catch (Exception e) {
            GPLog.error(this, null, e);
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
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public void onClick(View v) {
        minXYSpeed.x = seriesSpeed.getX(0).floatValue();
        maxXYSpeed.x = seriesSpeed.getX(seriesSpeed.size() - 1).floatValue();
        xyPlotSpeed.setDomainBoundaries(minXYSpeed.x, maxXYSpeed.x, BoundaryMode.FIXED);
        minXYElevation.x = seriesElev.getX(0).floatValue();
        maxXYElevation.x = seriesElev.getX(seriesElev.size() - 1).floatValue();
        xyPlotElev.setDomainBoundaries(minXYElevation.x, maxXYSpeed.x, BoundaryMode.FIXED);

        xyPlotElev.redraw();
        xyPlotSpeed.redraw();
        resetButton.hide();
    }
}
