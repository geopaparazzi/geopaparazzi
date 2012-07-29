package eu.geopaparazzi.library.routing.openrouteservice;

import java.io.IOException;
import java.sql.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

public class OpenRouteServiceHandler {

    public static enum Preference {
        Fastest, Shortest, Pedestrain, Bicycle
    }
    public static enum Language {
        en, it, de, fr, es
    }

    @SuppressWarnings("nls")
    public OpenRouteServiceHandler( double fromLat, double fromLon, double toLat, double toLon, Preference pref, Language lang,
            Boolean noTollways, Boolean noMotorWays ) {

        // start=10.84959,45.88943&end=10.66265,45.68752&preference=Fastest

        StringBuilder urlString = new StringBuilder();
        urlString.append("http://openls.geog.uni-heidelberg.de/osm/eu/routing?");
        urlString.append("&start=");// from
        urlString.append(fromLon);
        urlString.append(",");
        urlString.append(fromLat);
        urlString.append("&end=");// to
        urlString.append(toLon);
        urlString.append(",");
        urlString.append(toLat);
        urlString.append("&pref=");
        urlString.append(pref.toString());
        urlString.append("&lang=");
        urlString.append(lang.toString());
        if (noMotorWays != null) {
            urlString.append("&noMotorways==");
            urlString.append(noMotorWays.toString());
        }
        if (noTollways != null) {
            urlString.append("&noTollways=");
            urlString.append(noTollways.toString());
        }

    }

    public String getRouteString() {
        return "";
    }

    public void dumpInDatabase( String name, Context context, IGpsLogDbHelper logDumper ) throws Exception {
        SQLiteDatabase sqliteDatabase = logDumper.getDatabase(context);
        Date now = new Date(new java.util.Date().getTime());
        long newLogId = logDumper.addGpsLog(context, now, now, name, 1, "blue", true); //$NON-NLS-1$

        sqliteDatabase.beginTransaction();
        try {
            Date nowPlus10Secs = now;
            String path = "";
            if (path != null && path.trim().length() > 0) {
                String[] pairs = path.trim().split(" ");

                try {
                    for( int i = 1; i < pairs.length; i++ ) // the last one would be crash
                    {
                        String[] lngLat = pairs[i].split(",");
                        double lon = Double.parseDouble(lngLat[0]);
                        double lat = Double.parseDouble(lngLat[1]);
                        double altim = 0;
                        if (lngLat.length > 2) {
                            altim = Double.parseDouble(lngLat[2]);
                        }

                        // dummy time increment
                        nowPlus10Secs = new Date(nowPlus10Secs.getTime() + 10000);
                        logDumper.addGpsLogDataPoint(sqliteDatabase, newLogId, lon, lat, altim, nowPlus10Secs);
                    }
                } catch (NumberFormatException e) {
                    if (Debug.D)
                        Logger.e(this, "Cannot draw route.", e);
                }
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }

    }

}
