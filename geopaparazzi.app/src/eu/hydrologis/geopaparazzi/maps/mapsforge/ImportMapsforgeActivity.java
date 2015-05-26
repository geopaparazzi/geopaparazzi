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
package eu.hydrologis.geopaparazzi.maps.mapsforge;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.reader.Way;
import org.mapsforge.map.reader.header.FileOpenResult;

import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import jsqlite.*;

/**
 * The mapsforge data extraction activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportMapsforgeActivity extends Activity {

    private float[] nswe;
    private int zoomLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extract_mapsforge_view);

        Bundle extras = getIntent().getExtras();
        nswe = extras.getFloatArray(LibraryConstants.NSWE);
        zoomLevel = extras.getInt(LibraryConstants.ZOOMLEVEL);
    }


    /**
     * Start data extraction.
     *
     * @param view parent.
     */
    public void startExtraction(View view) {

        Date date = new Date();
        final long dateLong = date.getTime();

        CheckBox poisCheckbox = (CheckBox) findViewById(R.id.poisCheckbox);
        final boolean doPois = poisCheckbox.isChecked();
        CheckBox waysCheckbox = (CheckBox) findViewById(R.id.waysCheckbox);
        final boolean doWays = waysCheckbox.isChecked();
        CheckBox waterCheckbox = (CheckBox) findViewById(R.id.waterCheckbox);
        final boolean doWater = waterCheckbox.isChecked();
        CheckBox contoursCheckbox = (CheckBox) findViewById(R.id.contoursCheckbox);
        final boolean doContours = contoursCheckbox.isChecked();

        if (!doPois && !doWays && !doContours && !doWater) {
            return;
        }


        EditText filterEditTExt = (EditText) findViewById(R.id.filterEditText);
        final String filter = filterEditTExt.getText().toString().toLowerCase();

        CheckBox excludeCheckBox = (CheckBox) findViewById(R.id.excludeFilterCheckbox);
        final boolean filterExcludes = excludeCheckBox.isChecked();

        AbstractSpatialTable selectedSpatialTable = MapsDirManager.getInstance().getSelectedSpatialTable();
        if (selectedSpatialTable instanceof MapTable) {
            MapTable mapTable = (MapTable) selectedSpatialTable;
            File databaseFile = mapTable.getDatabaseFile();
            final MapDatabase mapFile = new MapDatabase();

            FileOpenResult result = mapFile.openFile(databaseFile);
            final double w = nswe[2];
            final double s = nswe[1];
            final double e = nswe[3];
            final double n = nswe[0];
            final byte z = (byte) zoomLevel;

            // prepare count for various levels, we go a few into detail

            long count = 0;
            long singleZoomCount = 0;
            final int zoomLimit = 4;
            for (int i = 0; i <= zoomLimit; i++) {
                int zoom = z + i;
                if (zoom > 22) {
                    break;
                }
                long startXTile = MercatorProjection.longitudeToTileX(w, (byte) zoom);
                long endXTile = MercatorProjection.longitudeToTileX(e, (byte) zoom);
                long startYTile = MercatorProjection.latitudeToTileY(n, (byte) zoom);
                long endYTile = MercatorProjection.latitudeToTileY(s, (byte) zoom);
                count = count + (endXTile - startXTile) * (endYTile - startYTile);
                if (i == 0) {
                    singleZoomCount = (endXTile - startXTile) * (endYTile - startYTile);
                }
            }


            StringAsyncTask task = new StringAsyncTask(this) {
                @Override
                protected String doBackgroundWork() {
                    try {
                        TreeSet<String> pointsSet = new TreeSet<String>();
                        TreeSet<String> waysSet = new TreeSet<String>();
                        HashMap<String, String> fieldsMap = new HashMap<String, String>();
                        List<String> fieldNames = new ArrayList<String>();
                        StringBuilder fieldsStringBuilder = new StringBuilder();
                        for (int j = 1; j <= 20; j++) {
                            String name = "field" + j;
                            fieldNames.add(name);
                            if (j != 1) {
                                fieldsStringBuilder.append(",");
                            }
                            fieldsStringBuilder.append(name);
                        }
                        String fieldsString = fieldsStringBuilder.toString();

                        // get mapsforge db
                        Database database = MapsforgeExtractorUtilities.getDatabase();


                        boolean doFilter = filter.length() > 0;
                        int index = 0;
                        for (int i = 0; i <= zoomLimit; i++) {
                            int zoom = z + i;
                            if (zoom > 22) {
                                break;
                            }
                            long startXTile = MercatorProjection.longitudeToTileX(w, (byte) zoom);
                            long endXTile = MercatorProjection.longitudeToTileX(e, (byte) zoom);
                            long startYTile = MercatorProjection.latitudeToTileY(n, (byte) zoom);
                            long endYTile = MercatorProjection.latitudeToTileY(s, (byte) zoom);
                            for (long tileX = startXTile; tileX <= endXTile; tileX++) {
                                for (long tileY = startYTile; tileY <= endYTile; tileY++) {
                                    index++;
                                    Tile tile = new Tile(tileX, tileY, (byte) zoom);
                                    MapReadResult mapReadResult = mapFile.readMapData(tile);

                                    if (doPois) {
                                        for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
                                            GeoPoint p = pointOfInterest.position;
                                            double longitude = p.getLongitude();
                                            double latitude = p.getLatitude();

                                            if (longitude < w || longitude > e || latitude < s || latitude > n) {
                                                // ignore external points
                                                continue;
                                            }

                                            MapsforgeExtractedFormHelper mapsforgeHelper = new MapsforgeExtractedFormHelper();
                                            List<Tag> tags = pointOfInterest.tags;
                                            double elev = -1.0;
                                            for (Tag tag : tags) {
                                                String key = tag.key;
                                                String value = tag.value;
                                                if (key.equals(MapsforgeExtractorUtilities.tagPoiElevation)) {
                                                    try {
                                                        elev = Double.parseDouble(value);
                                                    } catch (Exception e1) {
                                                        // ignore
                                                    }
                                                }
                                                mapsforgeHelper.addTag(key, value);
                                            }

                                            String form = mapsforgeHelper.toForm();
                                            String labelValue = mapsforgeHelper.getLabelValue();

                                            // check if the complete text contains the thing
                                            if (doFilter) {
                                                String formLC = form.toLowerCase();
                                                if (filterExcludes && formLC.contains(filter)) {
                                                    continue;
                                                } else if (!filterExcludes && !formLC.contains(filter)) {
                                                    continue;
                                                }
                                            }
                                            try {
                                                // check if it is double
                                                String key = longitude + "_" + latitude + "_" + form;
                                                if (pointsSet.add(key)) {
                                                    DaoNotes.addNote(longitude, latitude, elev, dateLong, labelValue, "POI", form, null);
                                                }
                                            } catch (IOException ex) {
                                                GPLog.error(this, null, ex);
                                            }
                                        }
                                    }
                                    // TODO
                                    if (i == 0 && (doWays || doContours || doWater)) {
                                        List<Way> ways = mapReadResult.ways;

                                        List<String> fieldNamesTmp = new ArrayList<String>();
                                        fieldNamesTmp.addAll(fieldNames);

                                        for (Way way : ways) {

                                            List<Tag> tags = way.tags;

                                            HashMap<String, String> fieldValueMap = new HashMap<String, String>();

                                            boolean isRoad = false;
                                            boolean isWater = false;
                                            boolean isContour = false;
                                            for (Tag tag : tags) {
                                                String key = tag.key;
                                                if (MapsforgeExtractorUtilities.isWay(key) && doWays) {
                                                    isRoad = true;
                                                    break;
                                                } else if (MapsforgeExtractorUtilities.isContour(key) && doContours) {
                                                    isContour = true;
                                                    break;
                                                } else if (MapsforgeExtractorUtilities.isWaterline(key) && doWater) {
                                                    isWater = true;
                                                    break;
                                                }
                                            }

                                            if (!isRoad && !isContour && !isWater) {
                                                continue;
                                            }

                                            for (Tag tag : tags) {
                                                String key = tag.key;
                                                String value = tag.value;

                                                String field = fieldsMap.get(key);
                                                if (field == null) {
                                                    // get next available field
                                                    field = fieldNamesTmp.remove(0);
                                                    fieldsMap.put(key, field);
                                                }
                                                fieldValueMap.put(field, value);
                                            }

                                            float[][] wayNodes = way.wayNodes;
                                            String trackId = wayNodes[0][0] + "_" + wayNodes[0][1];
                                            if (!waysSet.add(trackId)) {
                                                continue;
                                            }

                                            String tableName = MapsforgeExtractorUtilities.TABLENAME_WAYS;
                                            if (isContour) {
                                                tableName = MapsforgeExtractorUtilities.TABLENAME_CONTOURS;
                                            }
                                            if (isWater) {
                                                tableName = MapsforgeExtractorUtilities.TABLENAME_WATERLINES;
                                            }

                                            StringBuilder queryBuilder = new StringBuilder();
                                            queryBuilder.append("insert into ").append(tableName).append(" (geometry,");
                                            queryBuilder.append(fieldsString);
                                            queryBuilder.append(") values (CastToSingle(CastToXY(CastToLineString(GeomFromText('LINESTRING (");

                                            for (float[] wayNode : wayNodes) {
                                                for (int j = 0; j < wayNode.length - 1; j = j + 2) {
                                                    double lon = wayNode[j] / 1000000.0;
                                                    double lat = wayNode[j + 1] / 1000000.0;

                                                    if (j != 0) {
                                                        queryBuilder.append(",");
                                                    }
                                                    queryBuilder.append(lon).append(" ").append(lat);
                                                }
                                            }

                                            queryBuilder.append(")' , 4326))))");

                                            // now alpha values
                                            for (String fieldName : fieldNames) {
                                                String value = fieldValueMap.get(fieldName);
                                                if (value == null) {
                                                    value = "";
                                                }
                                                value = value.replaceAll("'", "''");
                                                queryBuilder.append(",'").append(value).append("'");
                                            }
                                            queryBuilder.append(")");

                                            try {
                                                database.exec(queryBuilder.toString(), null);
                                            } catch (jsqlite.Exception e1) {
                                                // ignore only the one unable to import
                                            }
                                        }
                                    }
                                    publishProgress(index);
                                }
                            }
                            if (!doPois) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        GPLog.error(ImportMapsforgeActivity.this, null, e);
                        return "ERROR: " + e.getLocalizedMessage();
                    } finally {
                        mapFile.closeFile();
                    }
                    return "";
                }

                @Override
                protected void doUiPostWork(String response) {
                    dispose();
                    if (response.length() != 0) {
                        Utilities.warningDialog(ImportMapsforgeActivity.this, response, new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
                    }

                }
            };
            if (!doPois) {
                count = singleZoomCount;
            }
            task.startProgressDialog(null, getString(R.string.extract_mapsforge_data), false, (int) count);
            task.execute();


        } else {
            Utilities.warningDialog(this, getString(R.string.extract_mapsforge_only_when_loaded), null);
        }
    }


}
