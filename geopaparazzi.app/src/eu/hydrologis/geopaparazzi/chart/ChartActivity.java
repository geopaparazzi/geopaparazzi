///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.hydrologis.geopaparazzi.chart;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.afree.chart.AFreeChart;
//import org.afree.chart.ChartRenderingInfo;
//import org.afree.chart.ChartTouchEvent;
//import org.afree.chart.ChartTouchListener;
//import org.afree.chart.axis.ValueAxis;
//import org.afree.chart.plot.Marker;
//import org.afree.chart.plot.Plot;
//import org.afree.chart.plot.PlotRenderingInfo;
//import org.afree.chart.plot.ValueMarker;
//import org.afree.chart.plot.XYPlot;
//import org.afree.data.xy.XYDataset;
//import org.afree.graphics.geom.RectShape;
//import org.afree.ui.Layer;
//import org.afree.ui.RectangleInsets;
//
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.Toast;
//import eu.geopaparazzi.library.chart.XYChartView;
//import eu.geopaparazzi.library.util.DynamicDoubleArray;
//import eu.geopaparazzi.library.util.debug.Debug;
//import eu.geopaparazzi.library.util.debug.Logger;
//import eu.hydrologis.geopaparazzi.R;
//import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
//import eu.hydrologis.geopaparazzi.util.Constants;
//import eu.hydrologis.geopaparazzi.util.Line;
//
///**
// * Activity that creates a chartview.
// * 
// * Adapted from GraphViewDemo by Arno den Hond.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class ChartActivity extends Activity implements ChartTouchListener {
//    private Button zoomtoButton;
//    private CheckBox selectionCheckbox;
//    private XYChartView chartView;
//
//    private List<Marker> markers = new ArrayList<Marker>();
//    private List<double[]> markerValues = new ArrayList<double[]>();
//
//    @Override
//    public void onCreate( Bundle savedInstanceState ) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.profilechart);
//
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            try {
//                long logid = extras.getLong(Constants.ID);
//
//                Line line = DaoGpsLog.getGpslogAsLine(this, logid, -1);
//
//                makeProfilePlot(line);
//            } catch (IOException e) {
//                if (Debug.D)
//                    Logger.e(this, e.getLocalizedMessage(), e);
//                e.printStackTrace();
//            }
//
//        } else {
//            Toast.makeText(this, R.string.an_error_occurred_while_creating_the_chart_, Toast.LENGTH_LONG).show();
//        }
//
//    }
//
//    @SuppressWarnings("nls")
//    private void makeProfilePlot( final Line line ) {
//
//        chartView = (XYChartView) findViewById(R.id.profilechartview);
//        chartView.addChartTouchListener(this);
//
//        selectionCheckbox = (CheckBox) findViewById(R.id.selectionmodecheck);
//        selectionCheckbox.setOnClickListener(new Button.OnClickListener(){
//            public void onClick( View v ) {
//                boolean checked = selectionCheckbox.isChecked();
//                zoomtoButton.setEnabled(checked);
//                markers.clear();
//                markerValues.clear();
//                if (!checked)
//                    chartView.clearMarkers();
//            }
//        });
//
//        zoomtoButton = (Button) findViewById(R.id.zoomtobutton);
//        zoomtoButton.setEnabled(false);
//        zoomtoButton.setOnClickListener(new Button.OnClickListener(){
//            public void onClick( View v ) {
//
//                if (markerValues.size() > 0) {
//                    double[] values = markerValues.get(0);
//                    double xMin = values[0];
//                    double yMin = values[1];
//                    double xMax = values[0];
//                    double yMax = values[1];
//
//                    if (markerValues.size() > 1) {
//                        values = markerValues.get(1);
//                        xMin = Math.min(xMin, values[0]);
//                        yMin = Math.min(yMin, values[1]);
//                        xMax = Math.max(xMax, values[0]);
//                        yMax = Math.max(yMax, values[1]);
//                    }
//                    chartView.zoomToSelection(xMin, xMax, Double.NaN, Double.NaN);
//                }
//
//            }
//        });
//
//        // final ProfileChartView chartView = new ProfileChartView(this, null);
//        final ProgressDialog progressDialog = ProgressDialog.show(this, "", getString(R.string.loading_data));
//
//        new AsyncTask<String, Void, XYDataset>(){
//            protected XYDataset doInBackground( String... params ) {
//                DynamicDoubleArray altimArray = line.getAltimList();
//                DynamicDoubleArray latArray = line.getLatList();
//                DynamicDoubleArray lonArray = line.getLonList();
//                String title = ChartActivity.this.getString(R.string.profile);
//                return chartView.createDatasetFromProfile(lonArray, latArray, altimArray, title);
//            }
//
//            protected void onPostExecute( XYDataset dataset ) {
//                chartView.setDataset(dataset, getString(R.string.chart_profile_view), getString(R.string.profilelable_distance),
//                        getString(R.string.profilelable_elevation));
//                progressDialog.dismiss();
//            }
//        }.execute((String) null);
//
//        // setContentView(chartView);
//    }
//
//    public void chartTouched( ChartTouchEvent event ) {
//
//        if (selectionCheckbox.isChecked()) {
//            AFreeChart chart = event.getChart();
//            MotionEvent motionEvent = event.getTrigger();
//            ChartRenderingInfo info = chartView.getChartRenderingInfo();
//
//            int x = (int) (motionEvent.getX() / chartView.getChartScaleX());
//            int y = (int) (motionEvent.getY() / chartView.getChartScaleY());
//
//            Plot plot = chart.getPlot();
//            if (plot instanceof XYPlot) {
//                double xValue = -1;
//                double yValue = -1;
//                XYPlot xyPlot = (XYPlot) plot;
//                PlotRenderingInfo plotInfo = info.getPlotInfo();
//                RectShape dataArea = plotInfo.getDataArea();
//                if (dataArea.contains(x, y)) {
//                    ValueAxis xaxis = xyPlot.getDomainAxis();
//                    if (xaxis != null) {
//                        xValue = xaxis.java2DToValue(x, plotInfo.getDataArea(), xyPlot.getDomainAxisEdge());
//                    }
//                    ValueAxis yaxis = xyPlot.getRangeAxis();
//                    if (yaxis != null) {
//                        yValue = yaxis.java2DToValue(y, plotInfo.getDataArea(), xyPlot.getRangeAxisEdge());
//                    }
//                }
//
//                if (markers.size() == 2) {
//                    Marker remove = markers.remove(0);
//                    markerValues.remove(0);
//                    xyPlot.removeDomainMarker(remove, Layer.BACKGROUND);
//                }
//                Marker marker = new ValueMarker(xValue, eu.hydrologis.geopaparazzi.R.color.main_decorations, 1.0f);
//                marker.setLabelOffset(new RectangleInsets(2, 5, 2, 5));
//                xyPlot.addDomainMarker(marker, Layer.BACKGROUND);
//                markers.add(marker);
//                markerValues.add(new double[]{xValue, yValue});
//                chartView.invalidate();
//            }
//        }
//    }
//}
