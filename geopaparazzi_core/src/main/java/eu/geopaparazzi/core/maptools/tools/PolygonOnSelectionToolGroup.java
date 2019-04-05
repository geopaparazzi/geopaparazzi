///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
//package eu.geopaparazzi.core.maptools.tools;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.PorterDuff.Mode;
//import android.os.Parcelable;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.View.OnTouchListener;
//import android.view.ViewGroup.LayoutParams;
//import android.widget.ImageButton;
//import android.widget.LinearLayout;
//
//import org.locationtech.jts.android.PointTransformation;
//import org.locationtech.jts.android.ShapeWriter;
//import org.locationtech.jts.geom.Geometry;
//import org.locationtech.jts.io.WKBReader;
//import org.mapsforge.core.graphics.Canvas;
//import org.mapsforge.core.graphics.Paint;
//import org.mapsforge.core.graphics.Style;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.android.view.MapView;
//import org.mapsforge.map.model.IMapViewPosition;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import eu.geopaparazzi.core.R;
//import eu.geopaparazzi.core.features.EditManager;
//import eu.geopaparazzi.core.features.EditingView;
//import eu.geopaparazzi.core.features.ILayer;
//import eu.geopaparazzi.core.features.Tool;
//import eu.geopaparazzi.core.features.ToolGroup;
//import eu.geopaparazzi.core.maptools.CopyToLayersListActivity;
//import eu.geopaparazzi.core.maptools.FeaturePagerActivity;
//import eu.geopaparazzi.core.maptools.FeatureUtilities;
//import eu.geopaparazzi.core.mapview.MapsSupportService;
//import eu.geopaparazzi.core.mapview.MapviewActivity;
//import eu.geopaparazzi.library.database.GPLog;
//import eu.geopaparazzi.library.features.Feature;
//import eu.geopaparazzi.library.style.ToolColors;
//import eu.geopaparazzi.library.util.Compat;
//import eu.geopaparazzi.mapsforge.core.proj.MapsforgePointTransformation;
//import eu.geopaparazzi.mapsforge.core.proj.OverlayViewProjection;
//import eu.geopaparazzi.mapsforge.utils.MapsforgeUtils;
//import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
//
///**
// * The group of tools active when a selection has been done.
// *
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class PolygonOnSelectionToolGroup implements ToolGroup, OnClickListener, OnTouchListener, OnSelectionToolGroup {
//
//    private final MapView mapView;
//
//    private int buttonSelectionColor;
//    private List<Feature> selectedFeatures = new ArrayList<>();
//    private ImageButton deleteFeatureButton;
//    private OverlayViewProjection editingViewProjection;
//
//    private Paint geometryPaintStroke;
//    private Paint geometryPaintFill;
//
//    private final Paint selectedGeometryPaintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Paint selectedGeometryPaintFill = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Paint selectedPreviewGeometryPaintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
//    private final Paint selectedPreviewGeometryPaintFill = AndroidGraphicFactory.INSTANCE.createPaint();
//
//    private WKBReader wkbReader = new WKBReader();
//
//    private ImageButton editAttributesButton;
//
//    private boolean isInDeletePreview;
//
//    private ImageButton commitButton;
//
//    private ImageButton undoButton;
//    private ImageButton copyFeatureButton;
//
//    /**
//     * Constructor.
//     *
//     * @param mapView          the map view.
//     * @param selectedFeatures the set of selected features.
//     */
//    public PolygonOnSelectionToolGroup(MapView mapView, List<Feature> selectedFeatures) {
//        this.mapView = mapView;
//        this.selectedFeatures.addAll(selectedFeatures);
//
//        EditingView editingView = EditManager.INSTANCE.getEditingView();
//        editingViewProjection = new OverlayViewProjection(mapView, editingView);
//        buttonSelectionColor = Compat.getColor(editingView.getContext(), R.color.main_selection);
//
//
//        int stroke = MapsforgeUtils.toColor(ToolColors.selection_stroke.getHex(), -1);
//        int fill = MapsforgeUtils.toColor(ToolColors.selection_fill.getHex(), 180);
////        selectedGeometryPaintFill.setAntiAlias(true);
//        selectedGeometryPaintFill.setColor(fill);
////        selectedGeometryPaintFill.setAlpha(180);
//        selectedGeometryPaintFill.setStyle(Style.FILL);
////        selectedGeometryPaintStroke.setAntiAlias(true);
//        selectedGeometryPaintStroke.setStrokeWidth(5f);
//        selectedGeometryPaintStroke.setColor(stroke);
//        selectedGeometryPaintStroke.setStyle(Style.STROKE);
//
//        stroke = MapsforgeUtils.toColor(ToolColors.preview_stroke.getHex(), -1);
//        fill = MapsforgeUtils.toColor(ToolColors.preview_fill.getHex(), 180);
////        selectedPreviewGeometryPaintFill.setAntiAlias(true);
//        selectedPreviewGeometryPaintFill.setColor(fill);
////        selectedPreviewGeometryPaintFill.setAlpha(180);
//        selectedPreviewGeometryPaintFill.setStyle(Style.FILL);
////        selectedPreviewGeometryPaintStroke.setAntiAlias(true);
//        selectedPreviewGeometryPaintStroke.setStrokeWidth(5f);
//        selectedPreviewGeometryPaintStroke.setColor(stroke);
//        selectedPreviewGeometryPaintStroke.setStyle(Style.STROKE);
//
//        geometryPaintFill = selectedGeometryPaintFill;
//        geometryPaintStroke = selectedGeometryPaintStroke;
//
//    }
//
//    public void activate() {
//        if (mapView != null)
//            mapView.setClickable(true);
//    }
//
//    public void initUI() {
//        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
//        parent.removeAllViews();
//
//        Context context = parent.getContext();
//        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
//        int padding = 2;
//
//        if (editLayer != null) {
//            deleteFeatureButton = new ImageButton(context);
//            deleteFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                    LayoutParams.WRAP_CONTENT));
//            deleteFeatureButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_delete_feature_24dp));
//            deleteFeatureButton.setPadding(0, padding, 0, padding);
//            deleteFeatureButton.setOnTouchListener(this);
//            deleteFeatureButton.setOnClickListener(this);
//            parent.addView(deleteFeatureButton);
//
//            copyFeatureButton = new ImageButton(context);
//            copyFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                    LayoutParams.WRAP_CONTENT));
//            copyFeatureButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_copy_geoms_24dp));
//            copyFeatureButton.setPadding(0, padding, 0, padding);
//            copyFeatureButton.setOnTouchListener(this);
//            copyFeatureButton.setOnClickListener(this);
//            parent.addView(copyFeatureButton);
//
//            editAttributesButton = new ImageButton(context);
//            editAttributesButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                    LayoutParams.WRAP_CONTENT));
//            editAttributesButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_view_attributes_24dp));
//            editAttributesButton.setPadding(0, padding, 0, padding);
//            editAttributesButton.setOnTouchListener(this);
//            editAttributesButton.setOnClickListener(this);
//            parent.addView(editAttributesButton);
//
//            undoButton = new ImageButton(context);
//            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//            undoButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_undo_24dp));
//            undoButton.setPadding(0, padding, 0, padding);
//            undoButton.setOnTouchListener(this);
//            undoButton.setOnClickListener(this);
//            parent.addView(undoButton);
//
//            commitButton = new ImageButton(context);
//            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//            commitButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_commit_24dp));
//            commitButton.setPadding(0, padding, 0, padding);
//            commitButton.setOnTouchListener(this);
//            commitButton.setOnClickListener(this);
//            parent.addView(commitButton);
//            commitButton.setVisibility(View.GONE);
//        }
//    }
//
//    public void disable() {
//        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
//        if (parent != null)
//            parent.removeAllViews();
//        parent = null;
//    }
//
//    public void onClick(View v) {
//        if (v == editAttributesButton) {
//            if (selectedFeatures.size() > 0) {
//                Context context = v.getContext();
//                Intent intent = new Intent(context, FeaturePagerActivity.class);
//                intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
//                        (ArrayList<? extends Parcelable>) selectedFeatures);
//                intent.putExtra(FeatureUtilities.KEY_READONLY, false);
//                if (context instanceof Activity) {
//                    Activity activity = (Activity) context;
//                    activity.startActivityForResult(intent, MapviewActivity.SELECTED_FEATURES_UPDATED_RETURN_CODE);
//                } else {
//                    context.startActivity(intent);
//                }
//            }
//        } else if (v == deleteFeatureButton) {
//            if (!isInDeletePreview) {
//                isInDeletePreview = true;
//                geometryPaintFill = selectedPreviewGeometryPaintFill;
//                geometryPaintStroke = selectedPreviewGeometryPaintStroke;
//                commitButton.setVisibility(View.VISIBLE);
//                EditManager.INSTANCE.invalidateEditingView();
//            }
//        } else if (v == copyFeatureButton) {
//            if (selectedFeatures.size() > 0) {
//                List<Feature> copySelectedFeatures = new ArrayList<>(selectedFeatures);
//                Context context = v.getContext();
//                Intent intent = new Intent(context, CopyToLayersListActivity.class);
//                intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
//                        (ArrayList<? extends Parcelable>) copySelectedFeatures);
//                context.startActivity(intent);
//
//                selectedFeatures.clear();
//                EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
//                EditManager.INSTANCE.setActiveTool(null);
//
//            }
//        } else if (v == undoButton) {
//            if (isInDeletePreview) {
//                /*
//                 * if in delete preview, disable it
//                 */
//                isInDeletePreview = false;
//                geometryPaintFill = selectedGeometryPaintFill;
//                geometryPaintStroke = selectedGeometryPaintStroke;
//                commitButton.setVisibility(View.GONE);
//                EditManager.INSTANCE.invalidateEditingView();
//            } else if (selectedFeatures.size() > 0) {
//                /*
//                 * if in selection mode, clear the selection
//                 */
//                selectedFeatures.clear();
//                EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
//                EditManager.INSTANCE.setActiveTool(null);
//                commitButton.setVisibility(View.GONE);
//            }
//        } else if (v == commitButton) {
//            if (isInDeletePreview) {
//                isInDeletePreview = false;
//
//                try {
//                    // delete features
//                    DaoSpatialite.deleteFeatures(selectedFeatures);
//                    selectedFeatures.clear();
//
//                    // reset mapview
//                    Context context = v.getContext();
//                    Intent intent = new Intent(context, MapsSupportService.class);
//                    intent.putExtra(MapsSupportService.REREAD_MAP_REQUEST, true);
//                    context.startService(intent);
//
//                    // reset drawview
//                    EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
//                    EditManager.INSTANCE.setActiveTool(null);
//
//                } catch (jsqlite.Exception e) {
//                    GPLog.error(this, null, e);
//                }
//
//                geometryPaintFill = selectedGeometryPaintFill;
//                geometryPaintStroke = selectedGeometryPaintStroke;
//            }
//        }
//    }
//
//    public boolean onTouch(View v, MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN: {
//                v.getBackground().setColorFilter(buttonSelectionColor, Mode.SRC_ATOP);
//                v.invalidate();
//                break;
//            }
//            case MotionEvent.ACTION_UP: {
//                v.getBackground().clearColorFilter();
//                v.invalidate();
//                break;
//            }
//        }
//        return false;
//    }
//
//    public void onToolFinished(Tool tool) {
//        // nothing
//    }
//
//
//    public void onToolDraw(Canvas canvas) {
//        try {
//            if (selectedFeatures.size() > 0) {
//                OverlayViewProjection projection = editingViewProjection;
//                IMapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;
//
//                byte zoomLevelBeforeDraw = mapPosition.getZoomLevel();
//                Point positionBeforeDraw = projection.toPoint(mapPosition.getCenter(),
//                        zoomLevelBeforeDraw);
//                // calculate the top-left point of the visible rectangle
//                double x = positionBeforeDraw.x - (canvas.getWidth() >> 1);
//                double y = positionBeforeDraw.y - (canvas.getHeight() >> 1);
//                Point point = new Point(x, y);
//
//                byte zoomLevel = mapPosition.getZoomLevel();
//
//                PointTransformation pointTransformer = new MapsforgePointTransformation(projection, point, zoomLevel, mapView.getModel().displayModel.getTileSize());
//                ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
//                shapeWriter.setRemoveDuplicatePoints(true);
//                // shapeWriter.setDecimation(spatialTable.getStyle().decimationFactor);
//
//                // draw features
//                for (Feature feature : selectedFeatures) {
//                    byte[] defaultGeometry = feature.getDefaultGeometry();
//                    if (defaultGeometry != null) {
//                        try {
//                            Geometry geometry = wkbReader.read(defaultGeometry);
//                            FeatureUtilities.drawGeometry(geometry, canvas, shapeWriter, geometryPaintFill, geometryPaintStroke);
//                        } catch (Exception e) {
//                            GPLog.error(this, null, e); //$NON-NLS-1$
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            GPLog.error(this, null, e);
//        }
//    }
//
//    public boolean onToolTouchEvent(MotionEvent event) {
//        return false;
//    }
//
//    public void onGpsUpdate(double lon, double lat) {
//        // ignore
//    }
//
//    /**
//     * Forces a feature selection.
//     * <p/>
//     * <p>Previous selections are cleared and a redrawing is triggered.
//     *
//     * @param features the new features to select.
//     */
//    public void setSelectedFeatures(List<Feature> features) {
//        selectedFeatures.clear();
//        selectedFeatures.addAll(features);
//        EditManager.INSTANCE.invalidateEditingView();
//    }
//}
