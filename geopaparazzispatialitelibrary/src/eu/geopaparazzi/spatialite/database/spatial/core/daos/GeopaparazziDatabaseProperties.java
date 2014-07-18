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

package eu.geopaparazzi.spatialite.database.spatial.core.daos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.util.Style;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

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
        PROPERTIESTABLE_FIELDS_LIST = Collections.unmodifiableList(fieldsList);
    }

    /**
     * Create the properties table.
     *
     * @param database the db to use.
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static void createPropertiesTable(Database database) throws jsqlite.Exception {
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
        sb.append(DECIMATION).append(" REAL");
        sb.append(" );");
        String query = sb.toString();
        database.exec(query, null);
    }

    /**
     * Create a default properties table for a spatial table.
     *
     * @param database               the db to use.
     * @param spatialTableUniqueName the spatial table's unique name to create the property record for.
     * @return the created style object.
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static Style createDefaultPropertiesForTable(Database database, String spatialTableUniqueName,
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
        database.exec(insertQuery, null);

        return style;
    }

    /**
     * Deletes the style properties table.
     *
     * @param database the db to use.
     * @throws Exception if something goes wrong.
     */
    public static void deleteStyleTable(Database database) throws Exception {
        GPLog.addLogEntry("Resetting style table for: " + database.getFilename());
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("drop table if exists " + PROPERTIESTABLE + ";");

        String selectQuery = sbSel.toString();
        Stmt stmt = database.prepare(selectQuery);
        try {
            stmt.step();
        } finally {
            stmt.close();
        }
    }


    /**
     * Update the style name in the properties table.
     *
     * @param database the db to use.
     * @param name     the new name.
     * @param id       the record id of the style.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyleName(Database database, String name, long id) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        sbIn.append(NAME).append("='").append(name).append("'");
        sbIn.append(" where ");
        sbIn.append(ID);
        sbIn.append("=");
        sbIn.append(id);

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Update a style definition.
     *
     * @param database the db to use.
     * @param style    the {@link Style} to set.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyle(Database database, Style style) throws Exception {
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

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param database               the db to use.
     * @param spatialTableUniqueName the table name.
     * @return the style.
     * @throws Exception if something goes wrong.
     */
    public static Style getStyle4Table(Database database, String spatialTableUniqueName, String spatialTableLabelField)
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
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(spatialTableUniqueName).append("';");

        String selectQuery = sbSel.toString();
        Stmt stmt = database.prepare(selectQuery);
        Style style = null;
        try {
            if (stmt.step()) {
                style = new Style();
                style.name = spatialTableUniqueName;
                style.id = stmt.column_long(0);
                style.size = (float) stmt.column_double(1);
                style.fillcolor = stmt.column_string(2);
                style.strokecolor = stmt.column_string(3);
                style.fillalpha = (float) stmt.column_double(4);
                style.strokealpha = (float) stmt.column_double(5);
                style.shape = stmt.column_string(6);
                style.width = (float) stmt.column_double(7);
                style.labelsize = (float) stmt.column_double(8);
                style.labelfield = stmt.column_string(9);
                style.labelvisible = stmt.column_int(10);
                style.enabled = stmt.column_int(11);
                style.order = stmt.column_int(12);
                style.dashPattern = stmt.column_string(13);
                style.minZoom = stmt.column_int(14);
                style.maxZoom = stmt.column_int(15);
                style.decimationFactor = (float) stmt.column_double(16);
            }
        } finally {
            stmt.close();
        }

        if (style == null) {
            style = createDefaultPropertiesForTable(database, spatialTableUniqueName, spatialTableLabelField);
        }

        return style;
    }

    /**
     * Retrieve the {@link Style} for all tables of a db.
     *
     * @param database the db to use.
     * @return the list of styles or <code>null</code> if something went wrong.
     */
    public static List<Style> getAllStyles(Database database) {
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
        Stmt stmt = null;
        try {
            stmt = database.prepare(selectQuery);
            List<Style> stylesList = new ArrayList<Style>();
            while (stmt.step()) {
                Style style = new Style();
                style.id = stmt.column_long(0);
                style.name = stmt.column_string(1);
                style.size = (float) stmt.column_double(2);
                style.fillcolor = stmt.column_string(3);
                style.strokecolor = stmt.column_string(4);
                style.fillalpha = (float) stmt.column_double(5);
                style.strokealpha = (float) stmt.column_double(6);
                style.shape = stmt.column_string(7);
                style.width = (float) stmt.column_double(8);
                style.labelsize = (float) stmt.column_double(9);
                style.labelfield = stmt.column_string(10);
                style.labelvisible = stmt.column_int(11);
                style.enabled = stmt.column_int(12);
                style.order = stmt.column_int(13);
                style.dashPattern = stmt.column_string(14);
                style.minZoom = stmt.column_int(15);
                style.maxZoom = stmt.column_int(16);
                style.decimationFactor = (float) stmt.column_double(17);
                stylesList.add(style);
            }
            return stylesList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }




}
