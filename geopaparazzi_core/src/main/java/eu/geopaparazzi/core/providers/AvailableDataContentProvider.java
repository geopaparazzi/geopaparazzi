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
package eu.geopaparazzi.core.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.json.JSONException;

import java.io.File;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;

/**
 * Content provider to query and modify current available data (spatialite, basemaps, tags)
 *
 * @author Andrea Antonello
 */
public class AvailableDataContentProvider extends ContentProvider {

    public static final String[] CONTENT_PROVIDER_FIELDS = new String[]{"id", "json"};
    public static final String CONTENTVALUES_ADD_PATH = "CONTENTVALUES_ADD_PATH";

    public static final String SPATIALITE = "SPATIALITE";
    public static final String BASEMAPS = "BASEMAPS";
    public static final String TAGS = "TAGS";
    public static final int SPATIALITE_ID = 1;
    public static final int BASEMAPS_ID = 2;
    public static final int TAGS_ID = 3;

    private static UriMatcher uriMatcher = null;

    public static String AUTHORITY = "";
    public static Uri BASE_CONTENT_URI = null;
    public static Uri CONTENT_URI = null;

    @Override
    public boolean onCreate() {
        try {
            String packageName = ResourcesManager.getInstance(getContext()).getPackageName();
            AUTHORITY = packageName + ".provider.availabledata";
            BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
            CONTENT_URI = BASE_CONTENT_URI.buildUpon().build();

            uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
            uriMatcher.addURI(AUTHORITY, SPATIALITE, SPATIALITE_ID);
            uriMatcher.addURI(AUTHORITY, BASEMAPS, BASEMAPS_ID);
            uriMatcher.addURI(AUTHORITY, TAGS, TAGS_ID);

        } catch (Exception e) {
            GPLog.error(this, null, e);
            return false;
        }
        return true;
    }


    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor mc = new MatrixCursor(CONTENT_PROVIDER_FIELDS);

        switch (uriMatcher.match(uri)) {
            case SPATIALITE_ID:
                List<SpatialiteMap> spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
                int id = 0;
                for (SpatialiteMap spatialiteMap : spatialiteMaps) {
                    try {
                        Object[] objs = new Object[]{id++, spatialiteMap.toJson()};
                        mc.addRow(objs);
                    } catch (JSONException e) {
                        GPLog.error(this, null, e);
                    }
                }
                break;
            case BASEMAPS_ID:
                List<BaseMap> baseMaps = BaseMapSourcesManager.INSTANCE.getBaseMaps();
                int bmId = 1;
                for (BaseMap baseMap : baseMaps) {
                    try {
                        Object[] objs = new Object[]{bmId++, baseMap.toJson()};
                        mc.addRow(objs);
                    } catch (JSONException e) {
                        GPLog.error(this, null, e);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid query uri: " + uri);
        }

        // configure to watch for content changes
        mc.setNotificationUri(getContext().getContentResolver(), uri);
        return mc;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String pathToAdd = values.getAsString(CONTENTVALUES_ADD_PATH);
        switch (uriMatcher.match(uri)) {
            case SPATIALITE_ID:
                SpatialiteSourcesManager.INSTANCE.addSpatialiteMapFromFile(new File(pathToAdd));
                break;
            case BASEMAPS_ID:
                BaseMapSourcesManager.INSTANCE.addBaseMapsFromFile(new File(pathToAdd));
            default:
                throw new UnsupportedOperationException("Invalid query uri: " + uri);
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            String idStr = uri.getLastPathSegment();
            int idToRemove = Integer.parseInt(idStr);
            switch (uriMatcher.match(uri)) {
                case SPATIALITE_ID:
                    List<SpatialiteMap> spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
                    SpatialiteMap spatialiteMapToRemove = spatialiteMaps.get(idToRemove);

                    SpatialiteSourcesManager.INSTANCE.removeSpatialiteMap(spatialiteMapToRemove);
                    break;
                case BASEMAPS_ID:
                    List<BaseMap> baseMaps = BaseMapSourcesManager.INSTANCE.getBaseMaps();
                    BaseMap baseMapToRemove = baseMaps.get(idToRemove);
                    BaseMapSourcesManager.INSTANCE.removeBaseMap(baseMapToRemove);
                default:
                    throw new UnsupportedOperationException("Invalid query uri: " + uri);
            }

            return 1;
        } catch (Exception e) {
            GPLog.error(this, null, e);
            return 0;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new RuntimeException("Not allowed");
    }
}
