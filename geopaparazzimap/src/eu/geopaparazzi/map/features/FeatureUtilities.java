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
package eu.geopaparazzi.map.features;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;

import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.LinearComponentExtracter;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.noding.snapround.GeometryNoder;
import org.locationtech.jts.operation.polygonize.Polygonizer;

import java.util.Collection;
import java.util.List;

import eu.geopaparazzi.map.jts.android.ShapeWriter;
import eu.geopaparazzi.map.jts.android.geom.DrawableShape;

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

//    /**
//     * Build the features given by a query.
//     * <p/>
//     * <p><b>Note that this query needs to have at least 2 arguments, the first
//     * being the ROWID and the last the geometry. Else if will fail.</b>
//     *
//     * @param query        the query to run.
//     * @param vectorLayer the parent Spatialtable.
//     * @return the list of feature from the query.
//     * @throws Exception is something goes wrong.
//     */
//    public static List<Feature> buildFeatures(String query, IVectorDbLayer vectorLayer) throws java.lang.Exception {
//        List<Feature> featuresList = vectorLayer.getFeatures(query);
//        return featuresList;
//    }

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
     * Checks if the text is a vievable string (ex urls or files) and if yes, opens the intent.
     *
     * @param context the context to use.
     * @param text    the text to check.
     * @return <code>true</code> if the text is viewable.
     */
    public static void viewIfApplicable(Context context, String text) {
        String textLC = text.toLowerCase();
        Intent intent = null;
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
        EGeometryType geometryType = EGeometryType.forTypeName(geometryTypeStr);
        DrawableShape shape = shapeWriter.toShape(geom);
        switch (geometryType) {
            case MULTIPOINT:
            case POINT: {
                if (geometryPaintFill != null)
                    shape.fill(canvas, geometryPaintFill);
                if (geometryPaintStroke != null)
                    shape.draw(canvas, geometryPaintStroke);
            }
            break;
            case MULTILINESTRING:
            case LINESTRING: {
                if (geometryPaintStroke != null)
                    shape.draw(canvas, geometryPaintStroke);
            }
            break;
            case MULTIPOLYGON:
            case POLYGON: {
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
}
