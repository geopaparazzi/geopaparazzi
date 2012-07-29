package eu.geopaparazzi.library.routing.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

public class GoogleJsonRouter {

    private String jsonOutput;

    public GoogleJsonRouter( double fromLat, double fromLon, double toLat, double toLon ) {

        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.google.com/maps?f=d&hl=en");
        urlString.append("&saddr=");// from
        urlString.append(fromLat);
        urlString.append(",");
        urlString.append(fromLon);
        urlString.append("&daddr=");// to
        urlString.append(toLat);
        urlString.append(",");
        urlString.append(toLon);
        urlString.append("&dirflg=w");
        urlString.append("&ie=UTF8&0&om=0&output=kml");

        try {
            URL url = new URL(urlString.toString());
            StringBuilder response = new StringBuilder();
            HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
            if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()), 8192);
                String strLine = null;
                while( (strLine = input.readLine()) != null ) {
                    response.append(strLine);
                }
                input.close();
            }
            jsonOutput = response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            if (Debug.D)
                Logger.d(this, "Exception parsing kml.");
        }

    }

    public String getRouteString() {
        return jsonOutput;
    }
}
