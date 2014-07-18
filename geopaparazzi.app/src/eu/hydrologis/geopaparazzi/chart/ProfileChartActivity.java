/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.hydrologis.geopaparazzi.chart;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Paint.Align;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.IOException;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;

/**
 * Profile chart class.
 * 
 * @author 4ViewSoft
 */
public class ProfileChartActivity extends Activity {
    /**
     * 
     */
    public static final String TYPE = "type"; //$NON-NLS-1$
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;
    private String mDateFormat;
    private GraphicalView mChartView;
    private Line line;
    private double yMin;
    private double yMax;
    private ProgressDialog progressDialog;

    @SuppressWarnings("nls")
    @Override
    protected void onRestoreInstanceState( Bundle savedState ) {
        super.onRestoreInstanceState(savedState);
        mDataset = (XYMultipleSeriesDataset) savedState.getSerializable("dataset");
        mRenderer = (XYMultipleSeriesRenderer) savedState.getSerializable("renderer");
        mCurrentSeries = (XYSeries) savedState.getSerializable("current_series");
        mCurrentRenderer = (XYSeriesRenderer) savedState.getSerializable("current_renderer");
        mDateFormat = savedState.getString("date_format");
    }

    @SuppressWarnings("nls")
    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dataset", mDataset);
        outState.putSerializable("renderer", mRenderer);
        outState.putSerializable("current_series", mCurrentSeries);
        outState.putSerializable("current_renderer", mCurrentRenderer);
        outState.putString("date_format", mDateFormat);
    }

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
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

        mRenderer.setApplyBackgroundColor(true);
        int backColor = getResources().getColor(R.color.main_background);
        mRenderer.setBackgroundColor(backColor);
        mRenderer.setMarginsColor(backColor);
        int forColor = getResources().getColor(R.color.main_decorations_dark);
        mRenderer.setLabelsColor(forColor);
        mRenderer.setXLabelsColor(forColor);
        mRenderer.setYLabelsColor(0, forColor);
        mRenderer.setAxesColor(forColor);

        int forColorLight = getResources().getColor(R.color.main_decorations);
        mRenderer.setGridColor(forColorLight);

        mRenderer.setAxisTitleTextSize(16);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);

        // top, left, bottom, right
        mRenderer.setMargins(new int[]{25, 50, 10, 5});
        // mRenderer.setYLabelsAngle(270f);
        mRenderer.setYLabelsAlign(Align.RIGHT);
        mRenderer.setShowGrid(true);

        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setPointSize(2);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChartView == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            mChartView = ChartFactory.getLineChartView(this, mDataset, mRenderer);
            mRenderer.setClickEnabled(true);
            mRenderer.setSelectableBuffer(100);

            mChartView.setBackgroundColor(getResources().getColor(R.color.main_background));

            /*
             * events
             */
            mChartView.setOnLongClickListener(new View.OnLongClickListener(){
                public boolean onLongClick( View v ) {
                    SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                    if (seriesSelection != null) {
                        Toast.makeText(ProfileChartActivity.this,
                                "X = " + seriesSelection.getXValue() + "\nY = " + seriesSelection.getValue(), Toast.LENGTH_LONG)
                                .show();
                    }
                    return true;
                }
            });
            layout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading_data));

            new AsyncTask<String, Void, XYSeries>(){
                protected XYSeries doInBackground( String... params ) {
                    DynamicDoubleArray altimArray = line.getAltimList();
                    DynamicDoubleArray latArray = line.getLatList();
                    DynamicDoubleArray lonArray = line.getLonList();
                    String title = getString(R.string.profile);
                    return createDatasetFromProfile(lonArray, latArray, altimArray, title);
                }

                protected void onPostExecute( XYSeries dataset ) {

                    mDataset.addSeries(dataset);
                    mCurrentSeries = dataset;
                    XYSeriesRenderer renderer = new XYSeriesRenderer();
                    mRenderer.addSeriesRenderer(renderer);
                    renderer.setPointStyle(PointStyle.CIRCLE);
                    renderer.setFillPoints(true);
                    mCurrentRenderer = renderer;

                    // chartView.setDataset(dataset, getString(R.string.chart_profile_view),
                    // getString(R.string.profilelable_distance),
                    // getString(R.string.profilelable_elevation));

                    mRenderer.setChartTitle(getString(R.string.chart_profile_view));
                    mRenderer.setXTitle(getString(R.string.profilelable_distance));
                    mRenderer.setYTitle(getString(R.string.profilelable_elevation));
                    if (mChartView != null) {
                        mChartView.repaint();
                    }
                    Utilities.dismissProgressDialog(progressDialog);
                }
            }.execute((String) null);

            // boolean enabled = mDataset.getSeriesCount() > 0;
            // setSeriesEnabled(enabled);
        } else {
            mChartView.repaint();
        }
    }

    /**
     * Create a dataset based on supplied data that are supposed to be coordinates and elevations for a profile view.
     * 
     * <p>
     * Note that this also sets the min and max values 
     * of the chart data, so the dataset created should
     * really be used through setDataset, so that the bounds 
     * are properly zoomed.
     * </p>
     * 
     * @param lonArray the array of longitudes.
     * @param latArray the array of latitudes.
     * @param elevArray the array of elevations.
     * @param seriesName the name to label the series with.
     * @return the {@link XYSeries dataset}.
     */
    public XYSeries createDatasetFromProfile( DynamicDoubleArray lonArray, DynamicDoubleArray latArray,
            DynamicDoubleArray elevArray, String seriesName ) {
        XYSeries xyS = new XYSeries(seriesName);

        yMin = Double.POSITIVE_INFINITY;
        yMax = Double.NEGATIVE_INFINITY;

        double plat = 0;
        double plon = 0;
        double summedDistance = 0.0;
        for( int i = 0; i < lonArray.size(); i++ ) {
            double elev = elevArray.get(i);
            double lat = latArray.get(i);
            double lon = lonArray.get(i);

            double distance = 0.0;
            if (i > 0) {
                Location thisLoc = new Location("dummy1"); //$NON-NLS-1$
                thisLoc.setLongitude(lon);
                thisLoc.setLatitude(lat);
                Location thatLoc = new Location("dummy2"); //$NON-NLS-1$
                thatLoc.setLongitude(plon);
                thatLoc.setLatitude(plat);
                distance = thisLoc.distanceTo(thatLoc);
            }
            plat = lat;
            plon = lon;
            summedDistance = summedDistance + distance;

            yMin = Math.min(yMin, elev);
            yMax = Math.max(yMax, elev);

            xyS.add(summedDistance, (int) elev);
        }
        return xyS;
    }

}
