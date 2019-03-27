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

package eu.geopaparazzi.spatialite;


import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.graphics.Cap;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.Style;

/**
 * geopaparazzi related database utilities.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziDatabaseProperties implements ISpatialiteTableAndFieldsNames {

    /**
     * The complete list of fields in the properties table.
     */
    public static List<String> PROPERTIESTABLE_FIELDS_LIST;

    static {
        List<String> fieldsList = new ArrayList<String>();
        fieldsList.add(ID);
        fieldsList.add(NAME);
        fieldsList.add(SIZE);
        fieldsList.add(FILLCOLOR);
        fieldsList.add(STROKECOLOR);
        fieldsList.add(FILLALPHA);
        fieldsList.add(STROKEALPHA);
        fieldsList.add(SHAPE);
        fieldsList.add(WIDTH);
        fieldsList.add(LABELSIZE);
        fieldsList.add(LABELFIELD);
        fieldsList.add(LABELVISIBLE);
        fieldsList.add(ENABLED);
        fieldsList.add(ORDER);
        fieldsList.add(DASH);
        fieldsList.add(MINZOOM);
        fieldsList.add(MAXZOOM);
        fieldsList.add(DECIMATION);
        fieldsList.add(THEME);
        PROPERTIESTABLE_FIELDS_LIST = Collections.unmodifiableList(fieldsList);
    }

    /**
     * Create the properties table.
     *
     * @param db the db to use.
     * @throws Exception if something goes wrong.
     */
    public static void createPropertiesTable(ASpatialDb db) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(PROPERTIESTABLE);
        sb.append(" (");
        sb.append(ID);
        sb.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(NAME).append(" TEXT, ");
        sb.append(SIZE).append(" REAL, ");
        sb.append(FILLCOLOR).append(" TEXT, ");
        sb.append(STROKECOLOR).append(" TEXT, ");
        sb.append(FILLALPHA).append(" REAL, ");
        sb.append(STROKEALPHA).append(" REAL, ");
        sb.append(SHAPE).append(" TEXT, ");
        sb.append(WIDTH).append(" REAL, ");
        sb.append(LABELSIZE).append(" REAL, ");
        sb.append(LABELFIELD).append(" TEXT, ");
        sb.append(LABELVISIBLE).append(" INTEGER, ");
        sb.append(ENABLED).append(" INTEGER, ");
        sb.append(ORDER).append(" INTEGER,");
        sb.append(DASH).append(" TEXT,");
        sb.append(MINZOOM).append(" INTEGER,");
        sb.append(MAXZOOM).append(" INTEGER,");
        sb.append(DECIMATION).append(" REAL,");
        sb.append(THEME).append(" TEXT");
        sb.append(" );");
        String query = sb.toString();

        db.executeInsertUpdateDeleteSql(query);
    }

    /**
     * Create a default properties table for a spatial table.
     *
     * @param db                     the db to use.
     * @param spatialTableUniqueName the spatial table's unique name to create the property record for.
     * @return the created style object.
     * @throws Exception if something goes wrong.
     */
    public static Style createDefaultPropertiesForTable(ASpatialDb db, String spatialTableUniqueName,
                                                        String spatialTableLabelField) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into ").append(PROPERTIESTABLE);
        sbIn.append(" ( ");
        sbIn.append(NAME).append(" , ");
        sbIn.append(SIZE).append(" , ");
        sbIn.append(FILLCOLOR).append(" , ");
        sbIn.append(STROKECOLOR).append(" , ");
        sbIn.append(FILLALPHA).append(" , ");
        sbIn.append(STROKEALPHA).append(" , ");
        sbIn.append(SHAPE).append(" , ");
        sbIn.append(WIDTH).append(" , ");
        sbIn.append(LABELSIZE).append(" , ");
        sbIn.append(LABELFIELD).append(" , ");
        sbIn.append(LABELVISIBLE).append(" , ");
        sbIn.append(ENABLED).append(" , ");
        sbIn.append(ORDER).append(" , ");
        sbIn.append(DASH).append(" ,");
        sbIn.append(MINZOOM).append(" ,");
        sbIn.append(MAXZOOM).append(" ,");
        sbIn.append(DECIMATION);
        sbIn.append(" ) ");
        sbIn.append(" values ");
        sbIn.append(" ( ");
        Style style = new Style();
        style.name = spatialTableUniqueName;
        style.labelfield = spatialTableLabelField;
        sbIn.append(style.insertValuesString());
        sbIn.append(" );");

        String insertQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(insertQuery);

        return style;
    }

    /**
     * Deletes the style properties table.
     *
     * @param db the db to use.
     * @throws Exception if something goes wrong.
     */
    public static void deleteStyleTable(ASpatialDb db) throws Exception {
        GPLog.addLogEntry("Resetting style table for: " + db.getDatabasePath());
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("drop table if exists " + PROPERTIESTABLE + ";");

        String selectQuery = sbSel.toString();
        db.executeInsertUpdateDeleteSql(selectQuery);
    }


    /**
     * Update the style name in the properties table.
     *
     * @param db   the db to use.
     * @param name the new name.
     * @param id   the record id of the style.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyleName(ASpatialDb db, String name, long id) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        sbIn.append(NAME).append("='").append(name).append("'");
        sbIn.append(" where ");
        sbIn.append(ID);
        sbIn.append("=");
        sbIn.append(id);

        String updateQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(updateQuery);
    }

    /**
     * Update a style definition.
     *
     * @param db    the db to use.
     * @param style the {@link Style} to set.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyle(ASpatialDb db, Style style) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        // sbIn.append(NAME).append("='").append(style.name).append("' , ");
        sbIn.append(SIZE).append("=").append(style.size).append(" , ");
        sbIn.append(FILLCOLOR).append("='").append(style.fillcolor).append("' , ");
        sbIn.append(STROKECOLOR).append("='").append(style.strokecolor).append("' , ");
        sbIn.append(FILLALPHA).append("=").append(style.fillalpha).append(" , ");
        sbIn.append(STROKEALPHA).append("=").append(style.strokealpha).append(" , ");
        sbIn.append(SHAPE).append("='").append(style.shape).append("' , ");
        sbIn.append(WIDTH).append("=").append(style.width).append(" , ");
        sbIn.append(LABELSIZE).append("=").append(style.labelsize).append(" , ");
        sbIn.append(LABELFIELD).append("='").append(style.labelfield).append("' , ");
        sbIn.append(LABELVISIBLE).append("=").append(style.labelvisible).append(" , ");
        sbIn.append(ENABLED).append("=").append(style.enabled).append(" , ");
        sbIn.append(ORDER).append("=").append(style.order).append(" , ");
        sbIn.append(DASH).append("='").append(style.dashPattern).append("' , ");
        sbIn.append(MINZOOM).append("=").append(style.minZoom).append(" , ");
        sbIn.append(MAXZOOM).append("=").append(style.maxZoom).append(" , ");
        sbIn.append(DECIMATION).append("=").append(style.decimationFactor);
        sbIn.append(" where ");
        sbIn.append(NAME);
        sbIn.append("='");
        sbIn.append(style.name);
        sbIn.append("';");

        // NOTE THAT THE THEME STYLE IS READONLY RIGHT NOW AND NOT UPDATED

        String updateQuery = sbIn.toString();
        db.executeInsertUpdateDeleteSql(updateQuery);
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param db                     the db to use.
     * @param spatialTableUniqueName the table name.
     * @return the style.
     * @throws Exception if something goes wrong.
     */
    public static Style getStyle4Table(ASpatialDb db, String spatialTableUniqueName, String spatialTableLabelField)
            throws Exception {
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(ID).append(" , ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(LABELSIZE).append(" , ");
        sbSel.append(LABELFIELD).append(" , ");
        sbSel.append(LABELVISIBLE).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION).append(" , ");
        sbSel.append(THEME);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(spatialTableUniqueName).append("';");


        String selectQuery = sbSel.toString();
        Style tableStyle = db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(selectQuery)) {
                if (rs.next()) {
                    String tabelName = rs.getString(1);
                    int i = 1;
                    Style style = new Style();
                    style.name = spatialTableUniqueName;
                    style.id = rs.getLong(i++);
                    style.size = (float) rs.getDouble(i++);
                    style.fillcolor = rs.getString(i++);
                    style.strokecolor = rs.getString(i++);
                    style.fillalpha = (float) rs.getDouble(i++);
                    style.strokealpha = (float) rs.getDouble(i++);
                    style.shape = rs.getString(i++);
                    style.width = (float) rs.getDouble(i++);
                    style.labelsize = (float) rs.getDouble(i++);
                    style.labelfield = rs.getString(i++);
                    style.labelvisible = rs.getInt(i++);
                    style.enabled = rs.getInt(i++);
                    style.order = rs.getInt(i++);
                    style.dashPattern = rs.getString(i++);
                    style.minZoom = rs.getInt(i++);
                    style.maxZoom = rs.getInt(i++);
                    style.decimationFactor = (float) rs.getDouble(i++);

                    String theme = rs.getString(i++);
                    if (theme != null && theme.trim().length() > 0) {
                        try {
                            JSONObject root = new JSONObject(theme);
                            if (root.has(UNIQUEVALUES)) {
                                JSONObject sub = root.getJSONObject(UNIQUEVALUES);
                                String fieldName = sub.keys().next();
                                style.themeField = fieldName;
                                JSONObject fieldObject = sub.getJSONObject(fieldName);
                                style.themeMap = new HashMap<>();
                                Iterator<String> keys = fieldObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    JSONObject styleObject = fieldObject.getJSONObject(key);

                                    Style themeStyle = getStyle(styleObject);
                                    style.themeMap.put(key, themeStyle);
                                }
                            }
                        } catch (JSONException e) {
                            GPLog.error("GeopaparazziDatabaseProperties", null, e);
                        }
                    }

                }
                return null;
            }
        });

        if (tableStyle == null) {
            tableStyle = createDefaultPropertiesForTable(db, spatialTableUniqueName, spatialTableLabelField);
        }

        return tableStyle;
    }

    private static Style getStyle(JSONObject styleObject) throws JSONException {
        Style style = new Style();

        if (styleObject.has(ID))
            style.id = styleObject.getLong(ID);
        if (styleObject.has(NAME))
            style.name = styleObject.getString(NAME);
        if (styleObject.has(SIZE))
            style.size = styleObject.getInt(SIZE);
        if (styleObject.has(FILLCOLOR))
            style.fillcolor = styleObject.getString(FILLCOLOR);
        else
            style.fillcolor = null;
        if (styleObject.has(STROKECOLOR))
            style.strokecolor = styleObject.getString(STROKECOLOR);
        else
            style.strokecolor = null;
        if (styleObject.has(FILLALPHA))
            style.fillalpha = (float) styleObject.getDouble(FILLALPHA);
        if (styleObject.has(STROKEALPHA))
            style.strokealpha = (float) styleObject.getDouble(STROKEALPHA);
        if (styleObject.has(SHAPE))
            style.shape = styleObject.getString(SHAPE);
        if (styleObject.has(WIDTH))
            style.width = (float) styleObject.getDouble(WIDTH);
        if (styleObject.has(LABELSIZE))
            style.labelsize = (float) styleObject.getDouble(LABELSIZE);
        if (styleObject.has(LABELFIELD))
            style.labelfield = styleObject.getString(LABELFIELD);
        if (styleObject.has(LABELVISIBLE))
            style.labelvisible = styleObject.getInt(LABELVISIBLE);
        if (styleObject.has(ENABLED))
            style.enabled = styleObject.getInt(ENABLED);
        if (styleObject.has(ORDER))
            style.order = styleObject.getInt(ORDER);
        if (styleObject.has(DASH))
            style.dashPattern = styleObject.getString(DASH);
        if (styleObject.has(MINZOOM))
            style.minZoom = styleObject.getInt(MINZOOM);
        if (styleObject.has(MAXZOOM))
            style.maxZoom = styleObject.getInt(MAXZOOM);
        if (styleObject.has(DECIMATION))
            style.decimationFactor = (float) styleObject.getDouble(DECIMATION);
        return style;
    }

    /**
     * Retrieve the {@link Style} for all tables of a db.
     *
     * @param db the db to use.
     * @return the list of styles or <code>null</code> if something went wrong.
     */
    public static List<Style> getAllStyles(ASpatialDb db) throws Exception {
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(ID).append(" , ");
        sbSel.append(NAME).append(" , ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(LABELSIZE).append(" , ");
        sbSel.append(LABELFIELD).append(" , ");
        sbSel.append(LABELVISIBLE).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);

        String selectQuery = sbSel.toString();
        List<Style> stylesList = db.execOnConnection(connection -> {
            List<Style> list = new ArrayList<Style>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(selectQuery)) {
                while (rs.next()) {
                    String tabelName = rs.getString(1);
                    int i = 1;
                    Style style = new Style();
                    style.id = rs.getLong(i++);
                    style.name = rs.getString(i++);
                    style.size = (float) rs.getDouble(i++);
                    style.fillcolor = rs.getString(i++);
                    style.strokecolor = rs.getString(i++);
                    style.fillalpha = (float) rs.getDouble(i++);
                    style.strokealpha = (float) rs.getDouble(i++);
                    style.shape = rs.getString(i++);
                    style.width = (float) rs.getDouble(i++);
                    style.labelsize = (float) rs.getDouble(i++);
                    style.labelfield = rs.getString(i++);
                    style.labelvisible = rs.getInt(i++);
                    style.enabled = rs.getInt(i++);
                    style.order = rs.getInt(i++);
                    style.dashPattern = rs.getString(i++);
                    style.minZoom = rs.getInt(i++);
                    style.maxZoom = rs.getInt(i++);
                    style.decimationFactor = (float) rs.getDouble(i++);

                    String theme = rs.getString(i++);
                    if (theme != null && theme.trim().length() > 0) {
                        try {
                            JSONObject root = new JSONObject(theme);
                            if (root.has(UNIQUEVALUES)) {
                                JSONObject sub = root.getJSONObject(UNIQUEVALUES);
                                String fieldName = sub.keys().next();
                                style.themeField = fieldName;
                                JSONObject fieldObject = sub.getJSONObject(fieldName);
                                style.themeMap = new HashMap<>();
                                Iterator<String> keys = fieldObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    JSONObject styleObject = fieldObject.getJSONObject(key);

                                    Style themeStyle = getStyle(styleObject);
                                    style.themeMap.put(key, themeStyle);
                                }
                            }
                        } catch (JSONException e) {
                            GPLog.error("GeopaparazziDatabaseProperties", null, e);
                        }
                    }

                    list.add(style);

                }
                return list;
            }
        });

        return stylesList;
    }


    /**
     * Get the fill {@link Paint} for a given style.
     * <p/>
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public static Paint getFillPaint4Style(Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
//        paint.setAntiAlias(true);
        paint.setStyle(org.mapsforge.core.graphics.Style.FILL);

//        int color = AndroidGraphicFactory.INSTANCE.createColor();
        paint.setColor(ColorUtilities.toColor(style.fillcolor));
        float alpha = style.fillalpha * 255f;

        // TODPO add alpha
//        paint.setAlpha((int) alpha);
        return paint;
    }

    /**
     * Get the stroke {@link Paint} for a given style.
     * <p/>
     * <p>Paints are cached and reused.</p>
     *
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public static Paint getStrokePaint4Style(Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(org.mapsforge.core.graphics.Style.STROKE);
//        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
        paint.setStrokeJoin(Join.ROUND);
        paint.setColor(ColorUtilities.toColor(style.strokecolor));
        float alpha = style.strokealpha * 255f;
//        paint.setAlpha((int) alpha);
        paint.setStrokeWidth(style.width);

        try {
            float[] shiftAndDash = Style.dashFromString(style.dashPattern);
            if (shiftAndDash != null) {
                float[] dash = Style.getDashOnly(shiftAndDash);
                if (dash.length > 1)
                    paint.setDashPathEffect(dash);
            }
        } catch (java.lang.Exception e) {
            GPLog.error("GeopaparazziDatabaseProperties", "Error on dash creation: " + style.dashPattern, e);
        }

        return paint;
    }

    /**
     * Get the fill paint for a theme style. These are not editable and as such do not change.
     *
     * @param uniqueValue the theme unique value key.
     * @param style       the style to use for the paint.
     * @return the generated paint.
     */
    public static Paint getFillPaint4Theme(String uniqueValue, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
//        paint.setAntiAlias(true);
        paint.setStyle(org.mapsforge.core.graphics.Style.FILL);
        paint.setColor(ColorUtilities.toColor(style.fillcolor));
        float alpha = style.fillalpha * 255f;
//        paint.setAlpha((int) alpha);
        return paint;
    }

    /**
     * Get the stroke paint for a theme style. These are not editable and as such do not change.
     *
     * @param uniqueValue the theme unique value key.
     * @param style       the style to use for the paint.
     * @return the generated paint.
     */
    public static Paint getStrokePaint4Theme(String uniqueValue, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setStyle(org.mapsforge.core.graphics.Style.STROKE);
//        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
        paint.setStrokeJoin(Join.ROUND);
        paint.setColor(ColorUtilities.toColor(style.strokecolor));
        float alpha = style.strokealpha * 255f;
//        paint.setAlpha((int) alpha);
        paint.setStrokeWidth(style.width);
        try {
            float[] shiftAndDash = Style.dashFromString(style.dashPattern);
            if (shiftAndDash != null) {
                float[] dash = Style.getDashOnly(shiftAndDash);
                if (dash.length > 1)
                    paint.setDashPathEffect(dash);
            }
        } catch (java.lang.Exception e) {
            GPLog.error("GeopaparazziDatabaseProperties", "Error on dash creation: " + style.dashPattern, e);
        }
        return paint;
    }


}
