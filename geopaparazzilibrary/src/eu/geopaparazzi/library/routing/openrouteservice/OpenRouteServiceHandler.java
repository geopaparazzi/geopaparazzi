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
package eu.geopaparazzi.library.routing.openrouteservice;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;

/**
 * Open route service class. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class OpenRouteServiceHandler {

    /**
     * preference option for routing.
     *
     */
    public static enum Preference {
        /**
         * 
         */
        Fastest,
        /**
         * 
         */
        Shortest,
        /**
          * 
          */
        Bicycle // shortest == pedestrian
    }
    /**
     *
     */
    public static enum Language {
        /**
         * 
         */
        en,
        /**
         * 
         */
        it,
        /**
         * 
         */
        de,
        /**
         * 
         */
        fr,
        /**
         * 
         */
        es
    }

    private float[] routePoints = null;
    private String distance = "";
    private String uom = "";

    private String errorMessage = null;
    private String urlString;

    /**
     * @param fromLat from lat
     * @param fromLon from lon
     * @param toLat to lat
     * @param toLon to lon
     * @param pref preference
     * @param lang language
     * @throws Exception  if something goes wrong.
     */
    public OpenRouteServiceHandler( double fromLat, double fromLon, double toLat, double toLon, Preference pref, Language lang )
            throws Exception {
        StringBuilder urlSB = new StringBuilder();
        urlSB.append("http://openls.geog.uni-heidelberg.de/osm/eu/routing?");
        urlSB.append("start=");// from
        urlSB.append(fromLon);
        urlSB.append(",");
        urlSB.append(fromLat);
        urlSB.append("&end=");// to
        urlSB.append(toLon);
        urlSB.append(",");
        urlSB.append(toLat);
        urlSB.append("&preference=");
        urlSB.append(pref.toString());
        urlSB.append("&language=");
        urlSB.append(lang.toString());
        /*
         * TODO check the openrouteservce docs, which seem to be wrong or outdated.
         */
        // if (noMotorWays != null) {
        // urlString.append("&noMotorways==");
        // urlString.append(noMotorWays.toString());
        // }
        // if (noTollways != null) {
        // urlString.append("&noTollways=");
        // urlString.append(noTollways.toString());
        // }

        urlString = urlSB.toString();
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();

        DocumentBuilder dom = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dom.parse(new InputSource(new InputStreamReader(connection.getInputStream())));

        /*
         * extract route length
         */
        NodeList routeSummaryList = doc.getElementsByTagName("xls:RouteSummary"); //$NON-NLS-1$
        for( int i = 0; i < routeSummaryList.getLength(); i++ ) {
            Node routeSummaryNode = routeSummaryList.item(i);
            NodeList totalDistance = ((Element) routeSummaryNode).getElementsByTagName("xls:TotalDistance"); //$NON-NLS-1$
            Node item = totalDistance.item(0);
            distance = ((Element) item).getAttribute("value");
            uom = ((Element) item).getAttribute("uom");
        }
        /*
         * extract route
         */
        NodeList routeGeometryList = doc.getElementsByTagName("xls:RouteGeometry"); //$NON-NLS-1$
        int routeGeometryListLength = routeGeometryList.getLength();
        for( int i = 0; i < routeGeometryListLength; i++ ) {
            Node gmlLinestring = routeGeometryList.item(i);
            NodeList gmlPoslist = ((Element) gmlLinestring).getElementsByTagName("gml:pos"); //$NON-NLS-1$
            int length = gmlPoslist.getLength();
            routePoints = new float[length * 2];
            int index = 0;
            for( int j = 0; j < length; j++ ) {
                String text = gmlPoslist.item(j).getFirstChild().getNodeValue();
                int s = text.indexOf(' ');
                try {
                    double lon = Double.parseDouble(text.substring(0, s));
                    double lat = Double.parseDouble(text.substring(s + 1));
                    routePoints[index++] = (float) lon;
                    routePoints[index++] = (float) lat;
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            }
        }

        if (routeGeometryListLength == 0) {
            NodeList errorList = doc.getElementsByTagName("xls:ErrorList"); //$NON-NLS-1$
            for( int i = 0; i < errorList.getLength(); i++ ) {
                Node errorNode = errorList.item(i);
                NodeList errors = ((Element) errorNode).getElementsByTagName("xls:Error"); //$NON-NLS-1$
                Node error = errors.item(0);
                errorMessage = ((Element) error).getAttribute("message");
            }
        }
    }

    /**
     * @return error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return used url string.
     */
    public String getUsedUrlString() {
        return urlString;
    }

    /**
     * @return route points.
     */
    public float[] getRoutePoints() {
        return routePoints;
    }

    /**
     * @return distance.
     */
    public String getDistance() {
        return distance;
    }

    /**
     * @return unit of measure.
     */
    public String getUom() {
        return uom;
    }

    /**
     * Dump route into teh db.
     * 
     * @param name name.
     * @param context  the context to use.
     * @param logDumper log db helper.
     * @throws Exception  if something goes wrong.
     */
    public void dumpInDatabase( String name, Context context, IGpsLogDbHelper logDumper ) throws Exception {
        SQLiteDatabase sqliteDatabase = logDumper.getDatabase();
        Date now = new Date(new java.util.Date().getTime());
        long newLogId = logDumper.addGpsLog(now, now, 0, name, 1, "blue", true); //$NON-NLS-1$

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
                    GPLog.error(this, "Cannot draw route.", e);
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
