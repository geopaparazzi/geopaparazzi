package eu.geopaparazzi.library.routing.google;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

public class GRouter {

    private NavigationDataSet dataset;

    public GRouter( double fromLat, double fromLon, double toLat, double toLon ) {

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
            // setup the url
            URL url = new URL(urlString.toString());
            // create the factory
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // create a parser
            SAXParser parser = factory.newSAXParser();
            // create the reader (scanner)
            XMLReader xmlreader = parser.getXMLReader();
            // instantiate our handler
            NavigationSaxHandler navSaxHandler = new NavigationSaxHandler();
            // assign our handler
            xmlreader.setContentHandler(navSaxHandler);
            // get our data via the url class
            InputStream openStream = url.openStream();
            InputStreamReader bis = new InputStreamReader(openStream);
            BufferedReader br = new BufferedReader(bis);
            String line = null;
            while( (line = br.readLine()) != null ) {
                Logger.i(this, line);
            }
            br.close();

            InputSource is = new InputSource(openStream);
            // perform the synchronous parse
            xmlreader.parse(is);

            dataset = navSaxHandler.getParsedData();

        } catch (Exception e) {
            e.printStackTrace();
            if (Debug.D)
                Logger.d(this, "Exception parsing kml.");
        }

    }

    public String getRouteString() {
        String path = dataset.getRoutePlacemark().getCoordinates();
        return path;
    }

    public void dumpInDatabase( String name, Context context, IGpsLogDbHelper logDumper ) throws Exception {
        SQLiteDatabase sqliteDatabase = logDumper.getDatabase(context);
        Date now = new Date(new java.util.Date().getTime());
        long newLogId = logDumper.addGpsLog(context, now, now, name, 1, "blue", true); //$NON-NLS-1$

        sqliteDatabase.beginTransaction();
        try {
            Date nowPlus10Secs = now;
            String path = dataset.getRoutePlacemark().getCoordinates();
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
