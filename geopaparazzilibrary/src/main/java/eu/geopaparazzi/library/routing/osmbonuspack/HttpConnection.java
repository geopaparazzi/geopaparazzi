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

package eu.geopaparazzi.library.routing.osmbonuspack;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import eu.geopaparazzi.library.network.NetworkUtilities;

/**
 * A "very very simple to use" class for performing http get and post requests.
 * So many ways to do that, and potential subtle issues.
 * If complexity should be added to handle even more issues, complexity should be put here and only here.
 * <p/>
 * Typical usage:
 * <pre>HttpConnection connection = new HttpConnection();
 * connection.doGet("http://www.google.com");
 * InputStream stream = connection.getStream();
 * if (stream != null) {
 * 	//use this stream, for buffer reading, or XML SAX parsing, or whatever...
 * }
 * connection.close();</pre>
 */
public class HttpConnection {

    private InputStream stream;
    private String mUserAgent;

    private final static int TIMEOUT_CONNECTION = 3000; //ms
    private final static int TIMEOUT_SOCKET = 10000; //ms
    private HttpURLConnection httpURLConnection;

    public HttpConnection() {
        stream = null;
    }

    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    /**
     * @param sUrl url to get
     */
    public int doGet(String sUrl) {
        try {
            httpURLConnection = NetworkUtilities.makeNewConnection(sUrl);
            if (mUserAgent != null)
                httpURLConnection.setRequestProperty("User-Agent", mUserAgent);
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode != NetworkUtilities.HTTP_OK) {
                Log.e(BonusPackHelper.LOG_TAG, "Invalid response from server: " + httpURLConnection.getResponseMessage());
                return responseCode;
            } else {
                stream = httpURLConnection.getInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NetworkUtilities.HTTP_OK;
    }

//    public int doPost(String sUrl, List<NameValuePair> nameValuePairs) {
//        try {
//            HttpPost request = new HttpPost(sUrl);
//            if (mUserAgent != null)
//                request.setHeader("User-Agent", mUserAgent);
//            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//            HttpResponse response = client.execute(request);
//            StatusLine status = response.getStatusLine();
//            if (status.getStatusCode() != HttpStatus.SC_OK) {
//                Log.e(BonusPackHelper.LOG_TAG, "Invalid response from server: " + status.toString());
//                return status.getStatusCode();
//            } else {
//                entity = response.getEntity();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return HttpStatus.SC_OK;
//    }

    /**
     * @return the opened InputStream, or null if creation failed for any reason.
     */
    public InputStream getStream() {
        return stream;
    }

    /**
     * @return the whole content as a String, or null if creation failed for any reason.
     */
    public String getContentAsString() {
        try {
            if (stream !=null){
                BufferedReader bi = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = bi.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                stream.close();
                return sb.toString().trim();
            } else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Calling close once is mandatory.
     */
    public void close() {
        if (stream != null) {
            try {
                stream.close();
                stream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (httpURLConnection!=null){
            httpURLConnection.disconnect();
        }
    }

}
