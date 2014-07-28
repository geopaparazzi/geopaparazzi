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
package eu.hydrologis.geopaparazzi.maptools;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;

import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.noding.snapround.GeometryNoder;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.util.DataType;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * A spatial feature container.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FeatureUtilities {

    /**
     * Key to pass featuresLists through activities.
     */
    public static final String KEY_FEATURESLIST = "KEY_FEATURESLIST";

    /**
     * Key to pass a readonly flag through activities.
     */
    public static final String KEY_READONLY = "KEY_READONLY";

    /**
     * Key to pass a geometry type through activities.
     */
    public static final String KEY_GEOMETRYTYPE = "KEY_GEOMETRYTYPE";

    /**
     * A well known binary reader to use for geometry deserialization.
     */
    public static WKBReader WKBREADER = new WKBReader();

    /**
     * A well known binary writer to use for geometry serialization.
     */
    public static WKBWriter WKBWRITER = new WKBWriter();

    /**
     * Build the features given by a query.
     * <p/>
     * <b>Note that it is mandatory that the first item of the
     * query is the id of the feature, which can be used at any time
     * to update the feature in the db.
     *
     * @param query        the query to run.
     * @param spatialTable the parent Spatialtable.
     * @return the list of feature from the query.
     * @throws Exception is something goes wrong.
     */
    public static List<Feature> buildWithoutGeometry(String query, SpatialVectorTable spatialTable) throws Exception {
        List<Feature> featuresList = new ArrayList<Feature>();
        AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
            Database database = spatialiteDbHandler.getDatabase();

            String tableName = spatialTable.getTableName();
            String uniqueNameBasedOnDbFilePath = spatialTable.getUniqueNameBasedOnDbFilePath();

            Stmt stmt = database.prepare(query);
            try {
                while (stmt.step()) {
                    int column_count = stmt.column_count();
                    // the first is the id, transparent to the user
                    String id = stmt.column_string(0);
                    Feature feature = new Feature(tableName, uniqueNameBasedOnDbFilePath, id);
                    for (int i = 1; i < column_count; i++) {
                        String cName = stmt.column_name(i);
                        String value = stmt.column_string(i);
                        int columnType = stmt.column_type(i);
                        DataType type = DataType.getType4SqliteCode(columnType);
                        feature.addAttribute(cName, value, type.name());
                    }
                    featuresList.add(feature);
                }
            } finally {
                stmt.close();
            }

        }
        return featuresList;
    }

    /**
     * Build the features given by a query.
     * <p/>
     * <p><b>Note that this query needs to have at least 2 arguments, the first
     * being the ROWID and the last the geometry. Else if will fail.</b>
     *
     * @param query        the query to run.
     * @param spatialTable the parent Spatialtable.
     * @return the list of feature from the query.
     * @throws Exception is something goes wrong.
     */
    public static List<Feature> buildFeatures(String query, SpatialVectorTable spatialTable) throws Exception {
        List<Feature> featuresList = new ArrayList<Feature>();
        AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
            Database database = spatialiteDbHandler.getDatabase();
            String tableName = spatialTable.getTableName();
            String uniqueNameBasedOnDbFilePath = spatialTable.getUniqueNameBasedOnDbFilePath();

            Stmt stmt = database.prepare(query);
            try {
                while (stmt.step()) {
                    int count = stmt.column_count();
                    String id = stmt.column_string(0);
                    byte[] geometryBytes = stmt.column_bytes(count - 1);
                    Feature feature = new Feature(tableName, uniqueNameBasedOnDbFilePath, id, geometryBytes);
                    for (int i = 1; i < count - 1; i++) {
                        String cName = stmt.column_name(i);
                        String value = stmt.column_string(i);
                        int columnType = stmt.column_type(i);
                        DataType type = DataType.getType4SqliteCode(columnType);
                        if (type == null) {
                            GPLog.addLogEntry("Featureutilities#buildFeatures", "Unexpected type " + columnType + " for column "
                                    + cName);
                            continue;
                        }
                        feature.addAttribute(cName, value, type.name());
                    }
                    featuresList.add(feature);
                }
            } finally {
                stmt.close();
            }
            for (Feature feature : featuresList) {
                String id = feature.getId();
                double[] areaLength = DaoSpatialite.getAreaAndLengthById(id, spatialTable);
                feature.setOriginalArea(areaLength[0]);
                feature.setOriginalLength(areaLength[1]);
            }
        }

        return featuresList;
    }

//    /**
//     * Build the features given by a query.
//     *
//     * <p><b>Note that this query needs to have at least 2 arguments, the first
//     * being the ROWID and the second the geometry. Else if will fail.</b>
//     *
//     * @param query the query to run.
//     * @param spatialTable the parent Spatialtable.
//     * @return the list of feature from the query.
//     * @throws Exception is something goes wrong.
//     */
//    public static List<Feature> buildRowidGeometryFeatures( String query, SpatialVectorTable spatialTable ) throws Exception {
//
//        List<Feature> featuresList = new ArrayList<Feature>();
//        AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
//        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
//            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
//            Database database = spatialiteDbHandler.getDatabase();
//            String tableName = spatialTable.getTableName();
//            String uniqueNameBasedOnDbFilePath = spatialTable.getUniqueNameBasedOnDbFilePath();
//
//            Stmt stmt = database.prepare(query);
//            try {
//                while( stmt.step() ) {
//                    String id = stmt.column_string(0);
//                    byte[] geometryBytes = stmt.column_bytes(1);
//                    Feature feature = new Feature(tableName, uniqueNameBasedOnDbFilePath, id, geometryBytes);
//                    featuresList.add(feature);
//                }
//            } finally {
//                stmt.close();
//            }
//        }
//        return featuresList;
//    }

    /**
     * Draw a geometry on a canvas.
     *
     * @param geom                the {@link Geometry} to draw.
     * @param canvas              the {@link Canvas}.
     * @param shapeWriter         the shape writer.
     * @param geometryPaintFill   the fill.
     * @param geometryPaintStroke the stroke.
     */
    public static void drawGeometry(Geometry geom, Canvas canvas, ShapeWriter shapeWriter, Paint geometryPaintFill,
                                    Paint geometryPaintStroke) {
        String geometryTypeStr = geom.getGeometryType();
        int geometryTypeInt = GeometryType.forValue(geometryTypeStr);
        GeometryType geometryType = GeometryType.forValue(geometryTypeInt);
        DrawableShape shape = shapeWriter.toShape(geom);
        switch (geometryType) {
            // case POINT_XY:
            // case POINT_XYM:
            // case POINT_XYZ:
            // case POINT_XYZM:
            // case MULTIPOINT_XY:
            // case MULTIPOINT_XYM:
            // case MULTIPOINT_XYZ:
            // case MULTIPOINT_XYZM: {
            // if (selectedGeometryPaintFill != null)
            // shape.fill(canvas, selectedGeometryPaintFill);
            // if (selectedGeometryPaintStroke != null)
            // shape.draw(canvas, selectedGeometryPaintStroke);
            // //
            // GPLog.androidLog(-1,"GeopaparazziOverlay.drawGeometry geometry_type["+s_geometry_type+"]: ["+i_geometry_type+"]");
            // }
            // break;
            // case LINESTRING_XY:
            // case LINESTRING_XYM:
            // case LINESTRING_XYZ:
            // case LINESTRING_XYZM:
            // case MULTILINESTRING_XY:
            // case MULTILINESTRING_XYM:
            // case MULTILINESTRING_XYZ:
            // case MULTILINESTRING_XYZM: {
            // if (selectedGeometryPaintStroke != null)
            // shape.draw(canvas, selectedGeometryPaintStroke);
            // }
            // break;
            case POLYGON_XY:
            case POLYGON_XYM:
            case POLYGON_XYZ:
            case POLYGON_XYZM:
            case MULTIPOLYGON_XY:
            case MULTIPOLYGON_XYM:
            case MULTIPOLYGON_XYZ:
            case MULTIPOLYGON_XYZM: {
                if (geometryPaintFill != null)
                    shape.fill(canvas, geometryPaintFill);
                if (geometryPaintStroke != null)
                    shape.draw(canvas, geometryPaintStroke);
            }
            break;
            default:
                break;
        }
    }

    /**
     * Get geometry from feature.
     *
     * @param feature the feature.
     * @return the {@link Geometry} or <code>null</code>.
     * @throws java.lang.Exception if something goes wrong.
     */
    public static Geometry getGeometry(Feature feature) throws java.lang.Exception {
        byte[] defaultGeometry = feature.getDefaultGeometry();
        if (defaultGeometry == null) {
            return null;
        }
        return WKBREADER.read(defaultGeometry);
    }

    /**
     * Tries to split an invalid polygon in its {@link GeometryCollection}.
     * <p/>
     * <p>Based on JTSBuilder code.
     *
     * @param invalidPolygon the invalid polygon.
     * @return the geometries.
     */
    @SuppressWarnings("rawtypes")
    public static Geometry invalidPolygonSplit(Geometry invalidPolygon) {
        PrecisionModel pm = new PrecisionModel(10000000);
        GeometryFactory geomFact = invalidPolygon.getFactory();
        List lines = LinearComponentExtracter.getLines(invalidPolygon);
        List nodedLinework = new GeometryNoder(pm).node(lines);
        // union the noded linework to remove duplicates
        Geometry nodedDedupedLinework = geomFact.buildGeometry(nodedLinework).union();
        // polygonize the result
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(nodedDedupedLinework);
        Collection polys = polygonizer.getPolygons();
        // convert to collection for return
        Polygon[] polyArray = GeometryFactory.toPolygonArray(polys);
        return geomFact.createGeometryCollection(polyArray);
    }

    /**
     * Get the SpatialVectorTable from the Feature.
     *
     * @param feature teh feature to get the table from.
     * @return the table or <code>null</code>.
     * @throws Exception
     */
    public static SpatialVectorTable getTableFromFeature(Feature feature) throws Exception {
        String tableName = feature.getUniqueTableName();
        List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);

        for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
            String uniqueNameBasedOnDbFilePath = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
            if (tableName.equals(uniqueNameBasedOnDbFilePath)) {
                return spatialVectorTable;
            }
        }
        return null;
    }

    /**
     * Checks if the text is a vievable string (ex urls or files) and if yes, opens the intent.
     *
     * @param context the context to use.
     * @param text the text to check.
     * @return <code>true</code> if the text is viewable.
     */
    public static void viewIfApplicable(Context context, String text) {
        String textLC = text.toLowerCase();
        Intent intent=null;
        if (textLC.startsWith("http")) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
            context.startActivity(intent);
        } else if (textLC.endsWith("png")) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + text), "image/png");
        } else if (textLC.endsWith("jpg")) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + text), "image/jpg");
        }
        if (intent != null)
            context.startActivity(intent);
    }
}
