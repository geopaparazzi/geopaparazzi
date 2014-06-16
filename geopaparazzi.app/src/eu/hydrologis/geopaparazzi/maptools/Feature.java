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

import java.util.ArrayList;
import java.util.List;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

import com.vividsolutions.jts.geom.Geometry;

import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;

/**
 * A spatial feature container.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("rawtypes")
public class Feature {

    private String id;

    private String tableName;

    private Geometry defaultGeometry;

    private List<String> attributeNames = new ArrayList<String>();
    private List<String> attributeValuesStrings = new ArrayList<String>();
    private List<Class> attributeClasses = new ArrayList<Class>();

    private Feature() {
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get an attribute's string representation by its name.
     * 
     * @param name the name.
     * @return the attribute.
     */
    public String getAttributeAsString( String name ) {
        int indexOf = attributeNames.indexOf(name);
        if (indexOf != -1) {
            return attributeValuesStrings.get(indexOf);
        }
        return null;
    }

    /**
     * @return the default geometry.
     */
    public Geometry getDefaultGeometry() {
        return defaultGeometry;
    }

    /**
     * @return the list of attributes names.
     */
    public List<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * @return the list of attribute values in string representation.
     */
    public List<String> getAttributeValuesStrings() {
        return attributeValuesStrings;
    }

    /**
     * @return the list of attributes classes.
     */
    public List<Class> getAttributeClasses() {
        return attributeClasses;
    }

    /**
     * @return the name of the table the feature is part of.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Build the features given by a query.
     * 
     * <b>Note that it is mandatory that the first item of the 
     * query is the id of the feature, which can be used at any time
     * to update the feature in the db. 
     * 
     * @param database the database to use.
     * @param query the query to run.
     * @param spatialTable the parent Spatialtable.
     * @return the list of feature from the query. 
     * @throws Exception is something goes wrong.
     */
    public static List<Feature> build( Database database, String query, SpatialVectorTable spatialTable ) throws Exception {
        List<Feature> featuresList = new ArrayList<Feature>();

        String tableName = spatialTable.getTableName();

        Stmt stmt = database.prepare(query);
        try {
            while( stmt.step() ) {
                Feature feature = new Feature();
                feature.tableName = tableName;
                int column_count = stmt.column_count();
                String id = stmt.column_string(0);
                feature.id = id;
                for( int i = 1; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    String value = stmt.column_string(i);
                    int columnType = stmt.column_type(i);
                    System.out.println(columnType);
                    feature.attributeNames.add(cName);
                    feature.attributeValuesStrings.add(value);
                    feature.attributeClasses.add(Float.class);
                }
                featuresList.add(feature);
            }
        } finally {
            stmt.close();
        }
        return featuresList;
    }
}
