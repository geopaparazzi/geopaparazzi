///* ===========================================================
// * AFreeChart : a free chart library for Android(tm) platform.
// *              (based on JFreeChart and JCommon)
// * ===========================================================
// *
// * (C) Copyright 2010, by ICOMSYSTECH Co.,Ltd.
// * (C) Copyright 2000-2008, by Object Refinery Limited and Contributors.
// *
// * Project Info:
// *    AFreeChart: http://code.google.com/p/afreechart/
// *    JFreeChart: http://www.jfree.org/jfreechart/index.html
// *    JCommon   : http://www.jfree.org/jcommon/index.html
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Lesser General Public License for more details.
// * 
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *
// * [Android is a trademark of Google Inc.]
// *
// * -----------------
// * ChartView.java
// * -----------------
// * (C) Copyright 2010, by ICOMSYSTECH Co.,Ltd.
// *
// * Original Author:  Niwano Masayoshi (for ICOMSYSTECH Co.,Ltd);
// * Contributor(s):   -;
// *
// * Changes
// * -------
// * 19-Nov-2010 : Version 0.0.1 (NM);
// * 14-Jan-2011 : renamed method name
// * 14-Jan-2011 : Updated API docs
// */
//
//package eu.geopaparazzi.library.chart;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.EventListener;
//import java.util.Iterator;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//import org.afree.chart.AFreeChart;
//import org.afree.chart.ChartFactory;
//import org.afree.chart.ChartRenderingInfo;
//import org.afree.chart.ChartTouchEvent;
//import org.afree.chart.ChartTouchListener;
//import org.afree.chart.axis.NumberAxis;
//import org.afree.chart.axis.ValueAxis;
//import org.afree.chart.entity.ChartEntity;
//import org.afree.chart.entity.EntityCollection;
//import org.afree.chart.event.ChartChangeEvent;
//import org.afree.chart.event.ChartChangeListener;
//import org.afree.chart.event.ChartProgressEvent;
//import org.afree.chart.event.ChartProgressListener;
//import org.afree.chart.plot.Marker;
//import org.afree.chart.plot.Movable;
//import org.afree.chart.plot.Plot;
//import org.afree.chart.plot.PlotOrientation;
//import org.afree.chart.plot.PlotRenderingInfo;
//import org.afree.chart.plot.XYPlot;
//import org.afree.chart.plot.Zoomable;
//import org.afree.chart.renderer.xy.XYItemRenderer;
//import org.afree.chart.renderer.xy.XYLineAndShapeRenderer;
//import org.afree.data.xy.XYDataset;
//import org.afree.data.xy.XYSeries;
//import org.afree.data.xy.XYSeriesCollection;
//import org.afree.graphics.SolidColor;
//import org.afree.graphics.geom.Dimension;
//import org.afree.graphics.geom.RectShape;
//import org.afree.ui.Layer;
//import org.afree.ui.RectangleInsets;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.PointF;
//import android.location.Location;
//import android.os.Handler;
//import android.util.AttributeSet;
//import android.view.MotionEvent;
//import android.view.View;
//import eu.geopaparazzi.library.util.DynamicDoubleArray;
//import eu.geopaparazzi.library.util.debug.Debug;
//import eu.geopaparazzi.library.util.debug.Logger;
//
///**
// * A view that contains an updatable chart.
// * 
// * <p>The chart is created empty at first and can be populated 
// * through:</p>
// * <ul>
// *   <li>use of {@link XYChartView#createDatasetFromXY(double[], double[], String)} or 
// *       {@link XYChartView#createDatasetFromProfile(double[], double[], double[], String)}
// *       to create a dataset from XY values
// *       </li>
// *   <li>use of {@link XYChartView#setDataset(XYDataset, String, String, String)}
// *       to update the chart</li>
// * </ul>
// */
//public class XYChartView extends View implements ChartChangeListener, ChartProgressListener {
//
//    /** The user interface thread handler. */
//    private Handler mHandler;
//
//    public XYChartView( Context context ) {
//        super(context);
//        mHandler = new Handler();
//        this.initialize();
//    }
//
//    public XYChartView( Context context, AttributeSet attrs ) {
//        super(context, attrs);
//
//        mHandler = new Handler();
//        this.initialize();
//    }
//
//    /**
//     * initialize parameters
//     */
//    private void initialize() {
//
//        this.chartMotionListeners = new CopyOnWriteArrayList<ChartTouchListener>();
//        this.info = new ChartRenderingInfo();
//        this.minimumDrawWidth = DEFAULT_MINIMUM_DRAW_WIDTH;
//        this.minimumDrawHeight = DEFAULT_MINIMUM_DRAW_HEIGHT;
//        this.maximumDrawWidth = DEFAULT_MAXIMUM_DRAW_WIDTH;
//        this.maximumDrawHeight = DEFAULT_MAXIMUM_DRAW_HEIGHT;
//        this.moveTriggerDistance = DEFAULT_MOVE_TRIGGER_DISTANCE;
//        new SolidColor(Color.BLUE);
//        new SolidColor(Color.argb(0, 0, 255, 63));
//        // new java.util.ArrayList();
//
//        final AFreeChart chart = createChart(createEmptyDataset());
//
//        setChart(chart);
//    }
//
//    /**
//     * Create a dataset based on supplied XY data.
//     * 
//     * <p>
//     * Note that this also sets the min and max values 
//     * of the chart data, so the dataset created should
//     * really be used through setDataset, so that the bounds 
//     * are properly zoomed.
//     * 
//     * @param xArray the array of ordered X values.
//     * @param yArray the array of ordered Y values.
//     * @param seriesName the name to label the serie with.
//     * @return the {@link XYSeriesCollection dataset}.
//     */
//    public XYSeriesCollection createDatasetFromXY( DynamicDoubleArray xArray, DynamicDoubleArray yArray, String seriesName ) {
//        XYSeries xyS = new XYSeries(seriesName, true, true);
//
//        int size = xArray.size();
//        xMin = 0;
//        xMax = 1.0;
//        if (size > 0) {
//            xMin = xArray.get(0);
//            if (size > 1) {
//                xMax = xArray.get(size - 1);
//            } else {
//                // fake it
//                xMax = xMin + 1.0;
//            }
//        }
//        yMin = Double.POSITIVE_INFINITY;
//        yMax = Double.NEGATIVE_INFINITY;
//
//        for( int i = 0; i < size; i++ ) {
//            double y = yArray.get(i);
//            yMin = Math.min(yMin, y);
//            yMax = Math.max(yMax, y);
//            xyS.add(xArray.get(i), y);
//        }
//
//        if (Math.abs(yMin - yMax) < 0.000001) {
//            // wider range
//            yMin = yMin - 1;
//            yMax = yMax + 1;
//        }
//
//        xMaxAll = xMax;
//        xMinAll = xMin;
//        yMaxAll = yMax;
//        yMinAll = yMin;
//
//        XYSeriesCollection xySC = new XYSeriesCollection();
//        xySC.addSeries(xyS);
//        return xySC;
//    }
//
//    // /**
//    // * Create a dataset based on supplied XY data.
//    // *
//    // * <p>
//    // * Note that this also sets the min and max values
//    // * of the chart data, so the dataset created should
//    // * really be used through setDataset, so that the bounds
//    // * are properly zoomed.
//    // *
//    // * @param xArray the array of ordered X values.
//    // * @param yArray the array of ordered Y values.
//    // * @param seriesName the name to label the serie with.
//    // * @return the {@link XYSeriesCollection dataset}.
//    // */
//    // public XYSeriesCollection createDatasetFromXY( List<Float> xArray, List<Float> yArray, String
//    // seriesName ) {
//    // XYSeries xyS = new XYSeries(seriesName, true, true);
//    //
//    // int size = xArray.size();
//    // xMin = xArray.get(0);
//    // xMax = xArray.get(size - 1);
//    // yMin = Double.POSITIVE_INFINITY;
//    // yMax = Double.NEGATIVE_INFINITY;
//    //
//    // for( int i = 0; i < size; i++ ) {
//    // double y = yArray.get(i);
//    // yMin = Math.min(yMin, y);
//    // yMax = Math.max(yMax, y);
//    // xyS.add(xArray.get(i).doubleValue(), y);
//    // }
//    //
//    // if (Math.abs(yMin - yMax) < 0.000001) {
//    // // wider range
//    // yMin = yMin - 1;
//    // yMax = yMax + 1;
//    // }
//    //
//    // xMaxAll = xMax;
//    // xMinAll = xMin;
//    // yMaxAll = yMax;
//    // yMinAll = yMin;
//    //
//    // XYSeriesCollection xySC = new XYSeriesCollection();
//    // xySC.addSeries(xyS);
//    // return xySC;
//    // }
//
//    /**
//     * Create a dataset based on supplied data that are supposed to be coordinates and elevations for a profile view.
//     * 
//     * <p>
//     * Note that this also sets the min and max values 
//     * of the chart data, so the dataset created should
//     * really be used through setDataset, so that the bounds 
//     * are properly zoomed.
//     * </p>
//     * 
//     * @param lonArray the array of longitudes.
//     * @param latArray the array of latitudes.
//     * @param elevArray the array of elevations.
//     * @param seriesName the name to label the serie with.
//     * @return the {@link XYSeriesCollection dataset}.
//     */
//    public XYSeriesCollection createDatasetFromProfile( DynamicDoubleArray lonArray, DynamicDoubleArray latArray,
//            DynamicDoubleArray elevArray, String seriesName ) {
//        XYSeries xyS = new XYSeries(seriesName, true, true);
//
//        xMin = 0;
//        xMax = 1; // just in case no points are in
//        yMin = Double.POSITIVE_INFINITY;
//        yMax = Double.NEGATIVE_INFINITY;
//
//        double plat = 0;
//        double plon = 0;
//        double summedDistance = 0.0;
//        for( int i = 0; i < lonArray.size(); i++ ) {
//            double elev = elevArray.get(i);
//            double lat = latArray.get(i);
//            double lon = lonArray.get(i);
//
//            double distance = 0.0;
//            if (i > 0) {
//                Location thisLoc = new Location("dummy1"); //$NON-NLS-1$
//                thisLoc.setLongitude(lon);
//                thisLoc.setLatitude(lat);
//                Location thatLoc = new Location("dummy2"); //$NON-NLS-1$
//                thatLoc.setLongitude(plon);
//                thatLoc.setLatitude(plat);
//                distance = thisLoc.distanceTo(thatLoc);
//            }
//            plat = lat;
//            plon = lon;
//            summedDistance = summedDistance + distance;
//
//            yMin = Math.min(yMin, elev);
//            yMax = Math.max(yMax, elev);
//
//            xyS.add(summedDistance, elev);
//        }
//        xMax = summedDistance;
//
//        xMaxAll = xMax;
//        xMinAll = xMin;
//        yMaxAll = yMax;
//        yMinAll = yMin;
//
//        XYSeriesCollection xySC = new XYSeriesCollection();
//        xySC.addSeries(xyS);
//        return xySC;
//    }
//
//    /**
//     * Draw the given dataset on the chart.
//     * 
//     * @param xyDataset the dataset to draw.
//     */
//    public void setDataset( XYDataset xyDataset, String title, String xLabel, String yLabel ) {
//        if (xyDataset == null) {
//            xyDataset = createEmptyDataset();
//        }
//
//        chart.setTitle(title);
//        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setDataset(xyDataset);
//
//        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
//        domainAxis.setRange(xMin, xMax);
//        domainAxis.setLabel(xLabel);
//        ValueAxis valueAxis = plot.getRangeAxis();
//        valueAxis.setRange(yMin, yMax);
//        valueAxis.setLabel(yLabel);
//
//        invalidate();
//    }
//
//    /**
//     * Creates a chart, initially empty. Needs a call to createDataset and setDataset.
//     *
//     * @param dataset  a dataset.
//     * @return a chart object.
//     */
//    @SuppressWarnings("nls")
//    private AFreeChart createChart( XYDataset dataset ) {
//
//        AFreeChart chart = ChartFactory.createXYLineChart("", "", "", dataset, // data
//                PlotOrientation.VERTICAL, false, // create legend?
//                true, // generate tooltips?
//                false // generate URLs?
//                );
//
//        chart.setBackgroundPaintType(new SolidColor(Color.WHITE));
//
//        XYPlot plot = (XYPlot) chart.getPlot();
//        plot.setBackgroundPaintType(new SolidColor(Color.LTGRAY));
//        plot.setDomainGridlinePaintType(new SolidColor(Color.WHITE));
//        plot.setRangeGridlinePaintType(new SolidColor(Color.WHITE));
//        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
//        // plot.setDomainCrosshairVisible(true);
//        // plot.setRangeCrosshairVisible(true);
//
//        XYItemRenderer r = plot.getRenderer();
//        if (r instanceof XYLineAndShapeRenderer) {
//            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
//            renderer.setBaseShapesVisible(true);
//            renderer.setBaseShapesFilled(true);
//            renderer.setDrawSeriesLineAsPath(true);
//        }
//
//        NumberAxis axis = (NumberAxis) plot.getDomainAxis();
//        axis.setRange(xMin, xMax);
//        ValueAxis valueAxis = plot.getRangeAxis();
//        valueAxis.setRange(yMin, yMax);
//        // axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
//
//        return chart;
//
//    }
//
//    /**
//     * Creates an empty dataset.
//     *
//     * @return The dataset.
//     */
//    private XYDataset createEmptyDataset() {
//        XYSeries xyS = new XYSeries("", true, true); //$NON-NLS-1$
//        xyS.add(0, 0);
//        xMin = -1;
//        xMax = 1;
//        yMin = -1;
//        yMax = 1;
//
//        XYSeriesCollection xySC = new XYSeriesCollection();
//        xySC.addSeries(xyS);
//        return xySC;
//    }
//
//    /**
//     * Default setting for buffer usage.  The default has been changed to
//     * <code>true</code> from version 1.0.13 onwards, because of a severe
//     * performance problem with drawing the zoom RectShape using XOR (which
//     * now happens only when the buffer is NOT used).
//     */
//    public static final boolean DEFAULT_BUFFER_USED = true;
//
//    /** The default panel width. */
//    public static final int DEFAULT_WIDTH = 680;
//
//    /** The default panel height. */
//    public static final int DEFAULT_HEIGHT = 420;
//
//    /** The default limit below which chart scaling kicks in. */
//    public static final int DEFAULT_MINIMUM_DRAW_WIDTH = 10;
//
//    /** The default limit below which chart scaling kicks in. */
//    public static final int DEFAULT_MINIMUM_DRAW_HEIGHT = 10;
//
//    /** The default limit above which chart scaling kicks in. */
//    public static final int DEFAULT_MAXIMUM_DRAW_WIDTH = 1280;
//
//    /** The default limit above which chart scaling kicks in. */
//    public static final int DEFAULT_MAXIMUM_DRAW_HEIGHT = 1000;
//
//    /** The minimum size required to perform a zoom on a RectShape */
//    public static final int DEFAULT_ZOOM_TRIGGER_DISTANCE = 10;
//
//    /** The minimum size required to perform a move on a RectShape */
//    public static final int DEFAULT_MOVE_TRIGGER_DISTANCE = 10;
//
//    /** The chart that is displayed in the panel. */
//    private AFreeChart chart;
//
//    /** Storage for registered (chart) touch listeners. */
//    private transient CopyOnWriteArrayList<ChartTouchListener> chartMotionListeners;
//
//    /** The drawing info collected the last time the chart was drawn. */
//    private ChartRenderingInfo info;
//
//    /** The scale factor used to draw the chart. */
//    private double scaleX;
//
//    /** The scale factor used to draw the chart. */
//    private double scaleY;
//
//    /** The plot orientation. */
//    private PlotOrientation orientation = PlotOrientation.VERTICAL;
//
//    /**
//     * The zoom RectShape starting point (selected by the user with touch).
//     * This is a point on the screen, not the chart (which may have
//     * been scaled up or down to fit the panel).
//     */
//    private PointF zoomPoint = null;
//
//    /** Controls if the zoom RectShape is drawn as an outline or filled. */
//    // private boolean fillZoomRectShape = true;
//
//    private int moveTriggerDistance;
//
//    /** The last touch position during panning. */
//    // private Point panLast;
//
//    private RectangleInsets insets = null;
//
//    /**
//     * The minimum width for drawing a chart (uses scaling for smaller widths).
//     */
//    private int minimumDrawWidth;
//
//    /**
//     * The minimum height for drawing a chart (uses scaling for smaller
//     * heights).
//     */
//    private int minimumDrawHeight;
//
//    /**
//     * The maximum width for drawing a chart (uses scaling for bigger
//     * widths).
//     */
//    private int maximumDrawWidth;
//
//    /**
//     * The maximum height for drawing a chart (uses scaling for bigger
//     * heights).
//     */
//    private int maximumDrawHeight;
//
//    private Dimension size = null;
//
//    /** The chart anchor point. */
//    private PointF anchor;
//
//    /** A flag that controls whether or not domain moving is enabled. */
//    private boolean domainMovable = false;
//
//    /** A flag that controls whether or not range moving is enabled. */
//    private boolean rangeMovable = false;
//
//    private double accelX, accelY;
//    private double friction = 0.8;
//    private boolean inertialMovedFlag = false;
//    private PointF lastTouch;
//
//    private float mScale = 1.0f;
//
//    private long mPrevTimeMillis = 0;
//    private long mNowTimeMillis = System.currentTimeMillis();
//
//    /**
//     * touch event
//     */
//    public boolean onTouchEvent( MotionEvent ev ) {
//
//        super.onTouchEvent(ev);
//        int action = ev.getAction();
//        int count = ev.getPointerCount();
//
//        this.anchor = new PointF(ev.getX(), ev.getY());
//
//        if (this.info != null) {
//            EntityCollection entities = this.info.getEntityCollection();
//            if (entities != null) {
//            }
//        }
//
//        switch( action & MotionEvent.ACTION_MASK ) {
//        case MotionEvent.ACTION_DOWN:
//        case MotionEvent.ACTION_POINTER_DOWN:
//            if (Debug.D)
//                Logger.i(this, "ACTION_DOWN"); //$NON-NLS-1$
//            if (count == 2 && this.multiTouchStartInfo == null) {
//                setMultiTouchStartInfo(ev);
//            } else if (count == 1 && this.singleTouchStartInfo == null) {
//                setSingleTouchStartInfo(ev);
//            }
//
//            touched(ev);
//
//            break;
//        case MotionEvent.ACTION_MOVE:
//            if (Debug.D)
//                Logger.i(this, "ACTION_MOVE"); //$NON-NLS-1$
//            if (count == 1 && this.singleTouchStartInfo != null) {
//                moveAdjustment(ev);
//            } else if (count == 2 && this.multiTouchStartInfo != null) {
//                // scaleAdjustment(ev);
//                zoomAdjustment(ev);
//            }
//
//            inertialMovedFlag = false;
//
//            break;
//        case MotionEvent.ACTION_UP:
//        case MotionEvent.ACTION_POINTER_UP:
//            if (Debug.D)
//                Logger.i(this, "ACTION_UP"); //$NON-NLS-1$
//            if (count <= 2) {
//                this.multiTouchStartInfo = null;
//                this.singleTouchStartInfo = null;
//            }
//            if (count <= 1) {
//                this.singleTouchStartInfo = null;
//            }
//
//            // double click check
//            if (count == 1) {
//                mNowTimeMillis = System.currentTimeMillis();
//                if (mNowTimeMillis - mPrevTimeMillis < 400) {
//                    if (chart.getPlot() instanceof Movable) {
//                        // restoreAutoBounds();
//                        zoomToSelection(xMinAll, xMaxAll, yMinAll, yMaxAll);
//
//                        mScale = 1.0f;
//                        inertialMovedFlag = false;
//                    }
//                } else {
//                    inertialMovedFlag = true;
//                }
//                mPrevTimeMillis = mNowTimeMillis;
//            }
//            break;
//        default:
//            break;
//        }
//
//        return true;
//    }
//
//    /**
//     * MultiTouchStartInfo setting
//     * @param ev
//     */
//    private void setMultiTouchStartInfo( MotionEvent ev ) {
//
//        if (this.multiTouchStartInfo == null) {
//            this.multiTouchStartInfo = new MultiTouchStartInfo();
//        }
//
//        // distance
//        double distance = Math.sqrt(Math.pow(ev.getX(0) - ev.getX(1), 2) + Math.pow(ev.getY(0) - ev.getY(1), 2));
//        this.multiTouchStartInfo.setDistance(distance);
//    }
//
//    /**
//     * SingleTouchStartInfo setting
//     * @param ev
//     */
//    private void setSingleTouchStartInfo( MotionEvent ev ) {
//
//        if (this.singleTouchStartInfo == null) {
//            this.singleTouchStartInfo = new SingleTouchStartInfo();
//        }
//
//        // start point
//        this.singleTouchStartInfo.setX(ev.getX(0));
//        this.singleTouchStartInfo.setY(ev.getY(0));
//    }
//
//    /**
//     * Translate MotionEvent as TouchEvent
//     * @param ev
//     */
//    private void moveAdjustment( MotionEvent ev ) {
//
//        boolean hMove = false;
//        boolean vMove = false;
//        if (this.orientation == PlotOrientation.HORIZONTAL) {
//            hMove = this.rangeMovable;
//            vMove = this.domainMovable;
//        } else {
//            hMove = this.domainMovable;
//            vMove = this.rangeMovable;
//        }
//
//        boolean moveTrigger1 = hMove && Math.abs(ev.getX(0) - this.singleTouchStartInfo.getX()) >= this.moveTriggerDistance;
//        boolean moveTrigger2 = vMove && Math.abs(ev.getY(0) - this.singleTouchStartInfo.getY()) >= this.moveTriggerDistance;
//        if (moveTrigger1 || moveTrigger2) {
//
//            RectShape dataArea = this.info.getPlotInfo().getDataArea();
//
//            double moveBoundX;
//            double moveBoundY;
//            double dataAreaWidth = dataArea.getWidth();
//            double dataAreaHeight = dataArea.getHeight();
//
//            // for touchReleased event, (horizontalZoom || verticalZoom)
//            // will be true, so we can just test for either being false;
//            // otherwise both are true
//
//            if (!vMove) {
//                moveBoundX = this.singleTouchStartInfo.getX() - ev.getX(0);
//                moveBoundY = 0;
//            } else if (!hMove) {
//                moveBoundX = 0;
//                moveBoundY = this.singleTouchStartInfo.getY() - ev.getY(0);
//            } else {
//                moveBoundX = this.singleTouchStartInfo.getX() - ev.getX(0);
//                moveBoundY = this.singleTouchStartInfo.getY() - ev.getY(0);
//            }
//            accelX = moveBoundX;
//            accelY = moveBoundY;
//
//            lastTouch = new PointF(ev.getX(0), ev.getY(0));
//            move(lastTouch, moveBoundX, moveBoundY, dataAreaWidth, dataAreaHeight);
//
//        }
//
//        setSingleTouchStartInfo(ev);
//    }
//
//    /**
//     * 
//     * @param moveBoundX
//     * @param moveBoundY
//     * @param dataAreaWidth
//     * @param dataAreaHeight
//     */
//    private void move( PointF source, double moveBoundX, double moveBoundY, double dataAreaWidth, double dataAreaHeight ) {
//
//        if (source == null) {
//            throw new IllegalArgumentException("Null 'source' argument"); //$NON-NLS-1$
//        }
//
//        double hMovePercent = moveBoundX / dataAreaWidth;
//        double vMovePercent = -moveBoundY / dataAreaHeight;
//
//        Plot p = this.chart.getPlot();
//        if (p instanceof Movable) {
//            PlotRenderingInfo info = this.info.getPlotInfo();
//            // here we tweak the notify flag on the plot so that only
//            // one notification happens even though we update multiple
//            // axes...
//            // boolean savedNotify = p.isNotify();
//            // p.setNotify(false);
//            Movable z = (Movable) p;
//            if (z.getOrientation() == PlotOrientation.HORIZONTAL) {
//                z.moveDomainAxes(vMovePercent, info, source);
//                z.moveRangeAxes(hMovePercent, info, source);
//            } else {
//                z.moveDomainAxes(hMovePercent, info, source);
//                z.moveRangeAxes(vMovePercent, info, source);
//            }
//            // p.setNotify(savedNotify);
//
//            // repaint
//            invalidate();
//        }
//
//    }
//
//    /**
//     * Restores the auto-range calculation on both axes.
//     */
//    public void restoreAutoBounds() {
//        Plot plot = this.chart.getPlot();
//        if (plot == null) {
//            return;
//        }
//        // here we tweak the notify flag on the plot so that only
//        // one notification happens even though we update multiple
//        // axes...
//        // boolean savedNotify = plot.isNotify();
//        // plot.setNotify(false);
//        restoreAutoDomainBounds();
//        restoreAutoRangeBounds();
//        // plot.setNotify(savedNotify);
//    }
//
//    /**
//     * Restores the auto-range calculation on the domain axis.
//     */
//    public void restoreAutoDomainBounds() {
//        Plot plot = this.chart.getPlot();
//        if (plot instanceof Zoomable) {
//            Zoomable z = (Zoomable) plot;
//            // here we tweak the notify flag on the plot so that only
//            // one notification happens even though we update multiple
//            // axes...
//            // boolean savedNotify = plot.isNotify();
//            // plot.setNotify(false);
//            // we need to guard against this.zoomPoint being null
//            PointF zp = (this.zoomPoint != null ? this.zoomPoint : new PointF());
//            z.zoomDomainAxes(0.0, this.info.getPlotInfo(), zp);
//            // plot.setNotify(savedNotify);
//        }
//    }
//
//    /**
//     * Restores the auto-range calculation on the range axis.
//     */
//    public void restoreAutoRangeBounds() {
//        Plot plot = this.chart.getPlot();
//        if (plot instanceof Zoomable) {
//            Zoomable z = (Zoomable) plot;
//            // here we tweak the notify flag on the plot so that only
//            // one notification happens even though we update multiple
//            // axes...
//            // boolean savedNotify = plot.isNotify();
//            // plot.setNotify(false);
//            // we need to guard against this.zoomPoint being null
//            PointF zp = (this.zoomPoint != null ? this.zoomPoint : new PointF());
//            z.zoomRangeAxes(0.0, this.info.getPlotInfo(), zp);
//            // plot.setNotify(savedNotify);
//        }
//    }
//
//    protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
//        this.insets = new RectangleInsets(0, 0, 0, 0);
//        this.size = new Dimension(w, h);
//    }
//
//    private RectangleInsets getInsets() {
//        return this.insets;
//    }
//
//    /**
//     * Returns the X scale factor for the chart.  This will be 1.0 if no
//     * scaling has been used.
//     *
//     * @return The scale factor.
//     */
//    public double getChartScaleX() {
//        return this.scaleX;
//    }
//
//    /**
//     * Returns the Y scale factory for the chart.  This will be 1.0 if no
//     * scaling has been used.
//     *
//     * @return The scale factor.
//     */
//    public double getChartScaleY() {
//        return this.scaleY;
//    }
//
//    /**
//     * Sets the chart that is displayed in the panel.
//     *
//     * @param chart  the chart (<code>null</code> permitted).
//     */
//    public void setChart( AFreeChart chart ) {
//
//        // stop listening for changes to the existing chart
//        if (this.chart != null) {
//            this.chart.removeChangeListener(this);
//            this.chart.removeProgressListener(this);
//        }
//
//        // add the new chart
//        this.chart = chart;
//        if (chart != null) {
//            this.chart.addChangeListener(this);
//            this.chart.addProgressListener(this);
//            Plot plot = chart.getPlot();
//            if (plot instanceof Zoomable) {
//                Zoomable z = (Zoomable) plot;
//                z.isRangeZoomable();
//                this.orientation = z.getOrientation();
//            }
//
//            this.domainMovable = false;
//            this.rangeMovable = false;
//            if (plot instanceof Movable) {
//                Movable z = (Movable) plot;
//                this.domainMovable = z.isDomainMovable();
//                this.rangeMovable = z.isRangeMovable();
//                this.orientation = z.getOrientation();
//            }
//        } else {
//            this.domainMovable = false;
//            this.rangeMovable = false;
//        }
//        // if (this.useBuffer) {
//        // this.refreshBuffer = true;
//        // }
//        repaint();
//
//    }
//
//    /**
//     * Returns the minimum drawing width for charts.
//     * <P>
//     * If the width available on the panel is less than this, then the chart is
//     * drawn at the minimum width then scaled down to fit.
//     *
//     * @return The minimum drawing width.
//     */
//    public int getMinimumDrawWidth() {
//        return this.minimumDrawWidth;
//    }
//
//    /**
//     * Sets the minimum drawing width for the chart on this panel.
//     * <P>
//     * At the time the chart is drawn on the panel, if the available width is
//     * less than this amount, the chart will be drawn using the minimum width
//     * then scaled down to fit the available space.
//     *
//     * @param width  The width.
//     */
//    public void setMinimumDrawWidth( int width ) {
//        this.minimumDrawWidth = width;
//    }
//
//    /**
//     * Returns the maximum drawing width for charts.
//     * <P>
//     * If the width available on the panel is greater than this, then the chart
//     * is drawn at the maximum width then scaled up to fit.
//     *
//     * @return The maximum drawing width.
//     */
//    public int getMaximumDrawWidth() {
//        return this.maximumDrawWidth;
//    }
//
//    /**
//     * Sets the maximum drawing width for the chart on this panel.
//     * <P>
//     * At the time the chart is drawn on the panel, if the available width is
//     * greater than this amount, the chart will be drawn using the maximum
//     * width then scaled up to fit the available space.
//     *
//     * @param width  The width.
//     */
//    public void setMaximumDrawWidth( int width ) {
//        this.maximumDrawWidth = width;
//    }
//
//    /**
//     * Returns the minimum drawing height for charts.
//     * <P>
//     * If the height available on the panel is less than this, then the chart
//     * is drawn at the minimum height then scaled down to fit.
//     *
//     * @return The minimum drawing height.
//     */
//    public int getMinimumDrawHeight() {
//        return this.minimumDrawHeight;
//    }
//
//    /**
//     * Sets the minimum drawing height for the chart on this panel.
//     * <P>
//     * At the time the chart is drawn on the panel, if the available height is
//     * less than this amount, the chart will be drawn using the minimum height
//     * then scaled down to fit the available space.
//     *
//     * @param height  The height.
//     */
//    public void setMinimumDrawHeight( int height ) {
//        this.minimumDrawHeight = height;
//    }
//
//    /**
//     * Returns the maximum drawing height for charts.
//     * <P>
//     * If the height available on the panel is greater than this, then the
//     * chart is drawn at the maximum height then scaled up to fit.
//     *
//     * @return The maximum drawing height.
//     */
//    public int getMaximumDrawHeight() {
//        return this.maximumDrawHeight;
//    }
//
//    /**
//     * Sets the maximum drawing height for the chart on this panel.
//     * <P>
//     * At the time the chart is drawn on the panel, if the available height is
//     * greater than this amount, the chart will be drawn using the maximum
//     * height then scaled up to fit the available space.
//     *
//     * @param height  The height.
//     */
//    public void setMaximumDrawHeight( int height ) {
//        this.maximumDrawHeight = height;
//    }
//
//    /**
//     * Returns the chart rendering info from the most recent chart redraw.
//     *
//     * @return The chart rendering info.
//     */
//    public ChartRenderingInfo getChartRenderingInfo() {
//        return this.info;
//    }
//
//    protected void onDraw( Canvas canvas ) {
//        super.onDraw(canvas);
//
//        // inertialMove();
//
//        paintComponent(canvas);
//    }
//
//    @Override
//    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    }
//
//    /**
//     * Paints the component by drawing the chart to fill the entire component,
//     * but allowing for the insets (which will be non-zero if a border has been
//     * set for this component).  To increase performance (at the expense of
//     * memory), an off-screen buffer image can be used.
//     *
//     * @param canvas  the graphics device for drawing on.
//     */
//    public void paintComponent( Canvas canvas ) {
//
//        // first determine the size of the chart rendering area...
//        Dimension size = getSize();
//        RectangleInsets insets = getInsets();
//        RectShape available = new RectShape(insets.getLeft(), insets.getTop(), size.getWidth() - insets.getLeft()
//                - insets.getRight(), size.getHeight() - insets.getTop() - insets.getBottom());
//
//        double drawWidth = available.getWidth();
//        double drawHeight = available.getHeight();
//        this.scaleX = 1.0;
//        this.scaleY = 1.0;
//
//        if (drawWidth < this.minimumDrawWidth) {
//            this.scaleX = drawWidth / this.minimumDrawWidth;
//            drawWidth = this.minimumDrawWidth;
//        } else if (drawWidth > this.maximumDrawWidth) {
//            this.scaleX = drawWidth / this.maximumDrawWidth;
//            drawWidth = this.maximumDrawWidth;
//        }
//
//        if (drawHeight < this.minimumDrawHeight) {
//            this.scaleY = drawHeight / this.minimumDrawHeight;
//            drawHeight = this.minimumDrawHeight;
//        } else if (drawHeight > this.maximumDrawHeight) {
//            this.scaleY = drawHeight / this.maximumDrawHeight;
//            drawHeight = this.maximumDrawHeight;
//        }
//
//        RectShape chartArea = new RectShape(0.0, 0.0, drawWidth, drawHeight);
//
//        // are we using the chart buffer?
//        // if (this.useBuffer) {
//        //
//        // // do we need to resize the buffer?
//        // if ((this.chartBuffer == null)
//        // || (this.chartBufferWidth != available.getWidth())
//        // || (this.chartBufferHeight != available.getHeight())) {
//        // this.chartBufferWidth = (int) available.getWidth();
//        // this.chartBufferHeight = (int) available.getHeight();
//        // GraphicsConfiguration gc = canvas.getDeviceConfiguration();
//        // this.chartBuffer = gc.createCompatibleImage(
//        // this.chartBufferWidth, this.chartBufferHeight,
//        // Transparency.TRANSLUCENT);
//        // this.refreshBuffer = true;
//        // }
//        //
//        // // do we need to redraw the buffer?
//        // if (this.refreshBuffer) {
//        //
//        // this.refreshBuffer = false; // clear the flag
//        //
//        // RectShape bufferArea = new RectShape(
//        // 0, 0, this.chartBufferWidth, this.chartBufferHeight);
//        //
//        // Graphics2D bufferG2 = (Graphics2D)
//        // this.chartBuffer.getGraphics();
//        // RectShape r = new RectShape(0, 0, this.chartBufferWidth,
//        // this.chartBufferHeight);
//        // bufferG2.setPaint(getBackground());
//        // bufferG2.fill(r);
//        // if (scale) {
//        // AffineTransform saved = bufferG2.getTransform();
//        // AffineTransform st = AffineTransform.getScaleInstance(
//        // this.scaleX, this.scaleY);
//        // bufferG2.transform(st);
//        // this.chart.draw(bufferG2, chartArea, this.anchor,
//        // this.info);
//        // bufferG2.setTransform(saved);
//        // }
//        // else {
//        // this.chart.draw(bufferG2, bufferArea, this.anchor,
//        // this.info);
//        // }
//        //
//        // }
//        //
//        // // zap the buffer onto the panel...
//        // canvas.drawImage(this.chartBuffer, insets.left, insets.top, this);
//        //
//        // }
//
//        // TODO:AffineTransform
//        // or redrawing the chart every time...
//        // else {
//
//        // AffineTransform saved = canvas.getTransform();
//        // canvas.translate(insets.left, insets.top);
//        // if (scale) {
//        // AffineTransform st = AffineTransform.getScaleInstance(
//        // this.scaleX, this.scaleY);
//        // canvas.transform(st);
//        // }
//        // this.chart.draw(canvas, chartArea, this.anchor, this.info);
//        // canvas.setTransform(saved);
//
//        // }
//        this.chart.draw(canvas, chartArea, this.anchor, this.info);
//
//        // Iterator iterator = this.overlays.iterator();
//        // while (iterator.hasNext()) {
//        // Overlay overlay = (Overlay) iterator.next();
//        // overlay.paintOverlay(canvas, this);
//        // }
//
//        // redraw the zoom RectShape (if present) - if useBuffer is false,
//        // we use XOR so we can XOR the RectShape away again without redrawing
//        // the chart
//        // drawZoomRectShape(canvas, !this.useBuffer);
//
//        // canvas.dispose();
//
//        this.anchor = null;
//        // this.verticalTraceLine = null;
//        // this.horizontalTraceLine = null;
//
//    }
//
//    public Dimension getSize() {
//        return this.size;
//    }
//
//    /**
//     * Returns the anchor point.
//     *
//     * @return The anchor point (possibly <code>null</code>).
//     */
//    public PointF getAnchor() {
//        return this.anchor;
//    }
//
//    public ChartRenderingInfo getInfo() {
//        return info;
//    }
//
//    /**
//     * Sets the anchor point.  This method is provided for the use of
//     * subclasses, not end users.
//     *
//     * @param anchor  the anchor point (<code>null</code> permitted).
//     */
//    protected void setAnchor( PointF anchor ) {
//        this.anchor = anchor;
//    }
//
//    /**
//     * Information for multi touch start
//     * @author ikeda
//     *
//     */
//    private class MultiTouchStartInfo {
//        private double distance = 0;
//
//        public double getDistance() {
//            return distance;
//        }
//        public void setDistance( double distance ) {
//            this.distance = distance;
//        }
//    }
//
//    private MultiTouchStartInfo multiTouchStartInfo = null;
//
//    /**
//     * Information for Single touch start
//     * @author ikeda
//     *
//     */
//    private class SingleTouchStartInfo {
//        private double x = 0;
//        private double y = 0;
//
//        public double getX() {
//            return x;
//        }
//        public void setX( double x ) {
//            this.x = x;
//        }
//        public double getY() {
//            return y;
//        }
//        public void setY( double y ) {
//            this.y = y;
//        }
//    }
//
//    private SingleTouchStartInfo singleTouchStartInfo = null;
//
//    private double xMin;
//    private double xMax;
//    private double yMin;
//    private double yMax;
//    private double xMinAll;
//    private double xMaxAll;
//    private double yMinAll;
//    private double yMaxAll;
//
//    /**
//     * Zoom 
//     * @param ev
//     */
//    private void zoomAdjustment( MotionEvent ev ) {
//        PointF point = new PointF((ev.getX(0) + ev.getX(1)) / 2, (ev.getY(0) + ev.getY(1)) / 2);
//        // end distance
//        double endDistance = Math.sqrt(Math.pow(ev.getX(0) - ev.getX(1), 2) + Math.pow(ev.getY(0) - ev.getY(1), 2));
//
//        // zoom process
//        zoom(point, this.multiTouchStartInfo.getDistance(), endDistance);
//
//        // reset start point
//        setMultiTouchStartInfo(ev);
//    }
//
//    /**
//     * zoom
//     * @param startDistance
//     * @param endDistance
//     */
//    private void zoom( PointF source, double startDistance, double endDistance ) {
//
//        Plot plot = this.chart.getPlot();
//        PlotRenderingInfo info = this.info.getPlotInfo();
//
//        if (plot instanceof Zoomable) {
//            float scaleDistance = (float) (startDistance / endDistance);
//
//            if (this.mScale * scaleDistance < 10.0f && this.mScale * scaleDistance > 0.1f) {
//                this.mScale *= scaleDistance;
//                Zoomable z = (Zoomable) plot;
//                z.zoomDomainAxes(scaleDistance, info, source, false);
//                z.zoomRangeAxes(scaleDistance, info, source, false);
//            }
//        }
//
//        // repaint
//        invalidate();
//    }
//
//    // inertialmove has been disabled to avoid infinite panning
//    @SuppressWarnings("unused")
//    private void inertialMove() {
//        if (inertialMovedFlag == true) {
//            RectShape dataArea = this.info.getPlotInfo().getDataArea();
//
//            accelX *= friction;
//            accelY *= friction;
//
//            double dataAreaWidth = dataArea.getWidth();
//            double dataAreaHeight = dataArea.getHeight();
//
//            if (lastTouch != null) {
//                move(lastTouch, accelX, accelY, dataAreaWidth, dataAreaHeight);
//            }
//
//            if (accelX < 0.1 && accelX > -0.1) {
//                accelX = 0;
//            }
//
//            if (accelY < 0.1 && accelY > -0.1) {
//                accelY = 0;
//            }
//
//            if (accelX == 0 && accelY == 0) {
//                inertialMovedFlag = false;
//            }
//        }
//    }
//    /**
//    * Receives notification of touch on the panel. These are
//    * translated and passed on to any registered {@link ChartTouchListener}s.
//    *
//    * @param event  Information about the touch event.
//    */
//    public void touched( MotionEvent event ) {
//
//        int x = (int) (event.getX() / this.scaleX);
//        int y = (int) (event.getY() / this.scaleY);
//
//        this.anchor = new PointF(x, y);
//        if (this.chart == null) {
//            return;
//        }
//        this.chart.setNotify(true); // force a redraw
//
//        chart.handleClick((int) event.getX(), (int) event.getY(), info);
//        inertialMovedFlag = false;
//
//        // new entity code...
//        if (this.chartMotionListeners.size() == 0) {
//            return;
//        }
//
//        ChartEntity entity = null;
//        if (this.info != null) {
//            EntityCollection entities = this.info.getEntityCollection();
//            if (entities != null) {
//                entity = entities.getEntity(x, y);
//            }
//        }
//        ChartTouchEvent chartEvent = new ChartTouchEvent(getChart(), event, entity);
//        for( int i = chartMotionListeners.size() - 1; i >= 0; i-- ) {
//            this.chartMotionListeners.get(i).chartTouched(chartEvent);
//        }
//
//    }
//
//    /**
//     * Returns the chart contained in the panel.
//     *
//     * @return The chart (possibly <code>null</code>).
//     */
//    public AFreeChart getChart() {
//        return this.chart;
//    }
//
//    /**
//     * Adds a listener to the list of objects listening for chart touch events.
//     *
//     * @param listener  the listener (<code>null</code> not permitted).
//     */
//    public void addChartTouchListener( ChartTouchListener listener ) {
//        if (listener == null) {
//            throw new IllegalArgumentException("Null 'listener' argument."); //$NON-NLS-1$
//        }
//        this.chartMotionListeners.add(listener);
//    }
//
//    /**
//     * Removes a listener from the list of objects listening for chart touch
//     * events.
//     *
//     * @param listener  the listener.
//     */
//    public void removeChartTouchListener( ChartTouchListener listener ) {
//        this.chartMotionListeners.remove(listener);
//    }
//
//    /**
//     * Returns an array of the listeners of the given type registered with the
//     * panel.
//     *
//     * @param listenerType  the listener type.
//     *
//     * @return An array of listeners.
//     */
//    public EventListener[] getListeners() {
//        return this.chartMotionListeners.toArray(new ChartTouchListener[0]);
//    }
//
//    /**
//     * Schedule a user interface repaint.
//     */
//    public void repaint() {
//        mHandler.post(new Runnable(){
//            public void run() {
//                invalidate();
//            }
//        });
//    }
//
//    /**
//     * Receives notification of changes to the chart, and redraws the chart.
//     *
//     * @param event  details of the chart change event.
//     */
//    public void chartChanged( ChartChangeEvent event ) {
//        // this.refreshBuffer = true;
//        Plot plot = this.chart.getPlot();
//        if (plot instanceof Zoomable) {
//            Zoomable z = (Zoomable) plot;
//            this.orientation = z.getOrientation();
//        }
//        repaint();
//    }
//
//    /**
//     * Receives notification of a chart progress event.
//     *
//     * @param event  the event.
//     */
//    public void chartProgress( ChartProgressEvent event ) {
//        // does nothing - override if necessary
//    }
//
//    public void clearMarkers() {
//        Plot plot = chart.getPlot();
//        if (plot instanceof XYPlot) {
//            XYPlot xyPlot = (XYPlot) plot;
//            Collection< ? > domainMarkers = xyPlot.getDomainMarkers(Layer.BACKGROUND);
//            if (domainMarkers != null) {
//                Iterator< ? > iterator = domainMarkers.iterator();
//                // store the keys in a list first to escape a ConcurrentModificationException
//                List<Marker> tmpMarkers = new ArrayList<Marker>();
//                while( iterator.hasNext() ) {
//                    Marker marker = (Marker) iterator.next();
//                    tmpMarkers.add(marker);
//                }
//                // now remove them
//                for( Marker marker : tmpMarkers ) {
//                    xyPlot.removeDomainMarker(marker, Layer.BACKGROUND);
//                }
//
//                invalidate();
//            }
//        }
//    }
//
//    public void zoomToSelection( double xMin2, double xMax2, double yMin2, double yMax2 ) {
//        XYPlot plot = (XYPlot) chart.getPlot();
//
//        xMin = xMin2;
//        xMax = xMax2;
//
//        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
//        domainAxis.setRange(xMin, xMax);
//
//        if (!Double.isNaN(yMin2) && !Double.isNaN(yMax2)) {
//            yMin = yMin2;
//            yMax = yMax2;
//            ValueAxis valueAxis = plot.getRangeAxis();
//            valueAxis.setRange(yMin, yMax);
//        }
//        invalidate();
//    }
//
//    // public boolean onTouchEvent( MotionEvent ev ) {
//    // super.onTouchEvent(ev);
//    // int action = ev.getAction();
//    // switch( action & MotionEvent.ACTION_MASK ) {
//    // case MotionEvent.ACTION_DOWN:
//    // case MotionEvent.ACTION_POINTER_DOWN:
//    // case MotionEvent.ACTION_MOVE:
//    // break;
//    // case MotionEvent.ACTION_UP:
//    // case MotionEvent.ACTION_POINTER_UP:
//    // Plot plot = chart.getPlot();
//    // if (plot instanceof XYPlot) {
//    // double xValue = -1;
//    // double yValue = -1;
//    // XYPlot xyPlot = (XYPlot) plot;
//    // int x = (int) (ev.getX() / this.scaleX);
//    // int y = (int) (ev.getY() / this.scaleY);
//    // PlotRenderingInfo plotInfo = info.getPlotInfo();
//    // RectShape dataArea = plotInfo.getDataArea();
//    // if (dataArea.contains(x, y)) {
//    // ValueAxis xaxis = xyPlot.getDomainAxis();
//    // if (xaxis != null) {
//    // xValue = xaxis.java2DToValue(x, plotInfo.getDataArea(), xyPlot.getDomainAxisEdge());
//    // }
//    // ValueAxis yaxis = xyPlot.getRangeAxis();
//    // if (yaxis != null) {
//    // yValue = yaxis.java2DToValue(y, plotInfo.getDataArea(), xyPlot.getRangeAxisEdge());
//    // }
//    // }
//    // final double fxValue = xValue;
//    // final double fyValue = yValue;
//    // new AsyncTask<String, Void, String>(){
//    // protected String doInBackground( String... params ) {
//    // return "";
//    // }
//    // @SuppressWarnings("nls")
//    // protected void onPostExecute( String response ) { // on UI thread!
//    // Log.i("TIMESERIDEMO", fxValue + "/" + fyValue);
//    // Toast.makeText(getContext(), fxValue + "/" + fyValue, Toast.LENGTH_LONG);
//    // }
//    // }.execute((String) null);
//    // }
//    // break;
//    // default:
//    // break;
//    // }
//    // return true;
//    // }
//}
