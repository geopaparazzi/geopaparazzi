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
package eu.hydrologis.geopaparazzi.maps;

import org.achartengine.model.XYSeries;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.util.Log;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.util.Line;
//import android.app.Activity;

/**
 * Excerpted from profile chart class.
 * 
 * @author 4ViewSoft, modifications by Tim Howard
 */
// public class GpsLogItemSummary extends Activity {
public class GpsLogItemSummary {
    /**
     * 
     */
    //public static final String TYPE = "type"; //$NON-NLS-1$
    // private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    // private XYSeries mCurrentSeries;
    // private String mDateFormat;
    private Line line;
    private double yMin;
    private double yMax;
    /**
     * the total length of the log (track)
     */
    public double trackLength = 8888;

    /**
     * gpslog table name.
     */
    public static final String TABLE_GPSLOGS = "gpslogs";

    public double updateLengthm( long logid ) {
        double totLen = 7777;
        try {
            line = DaoGpsLog.getGpslogAsLine(logid, -1);
            DynamicDoubleArray altimArray = line.getAltimList();
            DynamicDoubleArray latArray = line.getLatList();
            DynamicDoubleArray lonArray = line.getLonList();
            XYSeries dataset = createDatasetFromProfile(lonArray, latArray, altimArray, "temp");
            totLen = dataset.getMaxX();
            trackLength = totLen;

            SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
            sqliteDatabase.beginTransaction();

            String query = "update " + TABLE_GPSLOGS + " set lengthm = " + trackLength + " where id = " + logid;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e("error", "can't calculate total length", e);
            e.printStackTrace();
        }
        return totLen;
    }

    // a toString statement for the class, may not be needed
    @Override
    public String toString() {
        return Double.toString(trackLength);
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
