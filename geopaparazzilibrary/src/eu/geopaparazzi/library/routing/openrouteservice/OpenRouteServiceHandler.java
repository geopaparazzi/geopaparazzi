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
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

@SuppressWarnings("nls")
public class OpenRouteServiceHandler {

    public static enum Preference {
        Fastest, Shortest, Pedestrain, Bicycle
    }
    public static enum Language {
        en, it, de, fr, es
    }

    private float[] routePoints = null;
    private String distance = "";
    private String uom = "";

    public OpenRouteServiceHandler( double fromLat, double fromLon, double toLat, double toLon, Preference pref, Language lang )
            throws Exception {
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://openls.geog.uni-heidelberg.de/osm/eu/routing?");
        urlString.append("start=");// from
        urlString.append(fromLon);
        urlString.append(",");
        urlString.append(fromLat);
        urlString.append("&end=");// to
        urlString.append(toLon);
        urlString.append(",");
        urlString.append(toLat);
        urlString.append("&preference=");
        urlString.append(pref.toString());
        urlString.append("&language=");
        urlString.append(lang.toString());
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

        URL url = new URL(urlString.toString());
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
        for( int i = 0; i < routeGeometryList.getLength(); i++ ) {
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
                }
            }
        }
    }

    public float[] getRoutePoints() {
        return routePoints;
    }

    public String getDistance() {
        return distance;
    }

    public String getUom() {
        return uom;
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
