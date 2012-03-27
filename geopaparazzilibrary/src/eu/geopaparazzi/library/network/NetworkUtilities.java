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
package eu.geopaparazzi.library.network;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Network utils methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class NetworkUtilities {

    private static final String TAG = "NETWORKUTILITIES";
    public static final long maxBufferSize = 4096;

    private static HttpURLConnection makeNewConnection( String fileUrl ) throws Exception {
        // boolean doHttps =
        // CorePlugin.getDefault().getPreferenceStore().getBoolean(KeyManager.keys().getHttpConnectionTypeKey());
        URL url = new URL(fileUrl);
        if (fileUrl.startsWith("https")) {
            HttpsURLConnection urlC = (HttpsURLConnection) url.openConnection();
            return urlC;
        } else {
            HttpURLConnection urlC = (HttpURLConnection) url.openConnection();
            return urlC;
        }
    }

    /**
     * Sends an HTTP GET request to a url
     *
     * @param urlStr - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param file the output file.
     * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). 
     *           Note: This method will add the question mark (?) to the request - 
     *           DO NOT add it yourself
     * @param user
     * @param password
     * @return - The response from the end point
     * @throws Exception 
     */
    public static void sendGetRequest4File( String urlStr, File file, String requestParameters, String user, String password )
            throws Exception {
        if (requestParameters != null && requestParameters.length() > 0) {
            urlStr += "?" + requestParameters;
        }
        HttpURLConnection conn = makeNewConnection(urlStr);
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // conn.setChunkedStreamingMode(0);
        conn.setUseCaches(false);

        if (user != null && password != null) {
            conn.setRequestProperty("Authorization", getB64Auth(user, password));
        }
        conn.connect();

        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(file);

            byte[] buffer = new byte[(int) maxBufferSize];
            int bytesRead = in.read(buffer, 0, (int) maxBufferSize);
            while( bytesRead > 0 ) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer, 0, (int) maxBufferSize);
            }
            out.flush();

        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    /**
     * Sends a string via POST to a given url.
     * 
     * @param urlStr the url to which to send to.
     * @param string the string to send as post body.
     * @param user the user or <code>null</code>.
     * @param password the password or <code>null</code>.
     * @return the response.
     * @throws Exception
     */
    public static String sendPost( String urlStr, String string, String user, String password ) throws Exception {
        BufferedOutputStream wr = null;
        HttpURLConnection conn = null;
        try {
            conn = makeNewConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // conn.setChunkedStreamingMode(0);
            conn.setUseCaches(false);
            if (user != null && password != null) {
                conn.setRequestProperty("Authorization", getB64Auth(user, password));
            }
            conn.connect();

            // Make server believe we are form data...
            wr = new BufferedOutputStream(conn.getOutputStream());
            byte[] bytes = string.getBytes();
            wr.write(bytes);
            wr.flush();

            int responseCode = conn.getResponseCode();
            StringBuilder returnMessageBuilder = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                while( true ) {
                    String line = br.readLine();
                    if (line == null)
                        break;
                    returnMessageBuilder.append(line + "\n");
                }
                br.close();
            }

            return returnMessageBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    /**
     * Send a file via HTTP POST with basic authentication.
     * 
     * @param urlStr the server url to POST to.
     * @param file the file to send.
     * @param user the user or <code>null</code>.
     * @param password the password or <code>null</code>.
     * @return the return string from the POST.
     * @throws Exception
     */
    public static String sendFilePost( String urlStr, File file, String user, String password ) throws Exception {
        BufferedOutputStream wr = null;
        FileInputStream fis = null;
        HttpURLConnection conn = null;
        try {
            fis = new FileInputStream(file);
            long fileSize = file.length();
            // Authenticator.setDefault(new Authenticator(){
            // protected PasswordAuthentication getPasswordAuthentication() {
            // return new PasswordAuthentication("test", "test".toCharArray());
            // }
            // });
            conn = makeNewConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // conn.setChunkedStreamingMode(0);
            conn.setUseCaches(true);

            // conn.setRequestProperty("Accept-Encoding", "gzip ");
            // conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Type", "application/x-zip-compressed");
            // conn.setRequestProperty("Content-Length", "" + fileSize);
            // conn.setRequestProperty("Connection", "Keep-Alive");

            if (user != null && password != null) {
                conn.setRequestProperty("Authorization", getB64Auth(user, password));
            }
            conn.connect();

            wr = new BufferedOutputStream(conn.getOutputStream());
            long bufferSize = Math.min(fileSize, maxBufferSize);
            Logger.i(TAG, "BUFFER USED: " + bufferSize);
            byte[] buffer = new byte[(int) bufferSize];
            int bytesRead = fis.read(buffer, 0, (int) bufferSize);
            long totalBytesWritten = 0;
            while( bytesRead > 0 ) {
                wr.write(buffer, 0, (int) bufferSize);
                totalBytesWritten = totalBytesWritten + bufferSize;
                if (totalBytesWritten >= fileSize)
                    break;

                bufferSize = Math.min(fileSize - totalBytesWritten, maxBufferSize);
                bytesRead = fis.read(buffer, 0, (int) bufferSize);
            }
            wr.flush();

            String responseMessage = conn.getResponseMessage();
            if (Debug.D)
                Logger.d(TAG, "POST RESPONSE: " + responseMessage);
            return responseMessage;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (wr != null)
                wr.close();
            if (fis != null)
                fis.close();
            if (conn != null)
                conn.disconnect();
        }
    }

    private static String getB64Auth( String login, String pass ) {
        String source = login + ":" + pass;
        String ret = source; //"Basic " + Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        return ret;
    }

    /**
     * Checks is the network is available.
     * 
     * @param context the {@link Context}.
     * @return true if the network is available.
     */
    public static boolean isNetworkAvailable( Context context ) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for( int i = 0; i < info.length; i++ ) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String sendGetRequest( String urlStr, String requestParameters, String user, String password ) throws Exception {
        if (requestParameters != null && requestParameters.length() > 0) {
            urlStr += "?" + requestParameters;
        }
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(urlStr);

        if (user != null && password != null) {
            httpGet.addHeader("Authorization", getB64Auth(user, password));
        }
        HttpResponse response = client.execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while( (line = reader.readLine()) != null ) {
                builder.append(line);
            }
        } else {
            String message = "Failed to download file";
            Logger.d(TAG, message);
            throw new IOException(message);
        }
        return builder.toString();
    }

    // public static String uploadFile( Context context, String urlStr, File file, String user,
    // String password ) {
    // try {
    // FileInputStream fileInputStream = new FileInputStream(file);
    // String lineEnd = "\r\n";
    // String twoHyphens = "--";
    // String boundary = "*****";
    // // ------------------ CLIENT REQUEST
    // URL connectURL = new URL(urlStr);
    // HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
    // conn.setDoInput(true);
    // conn.setDoOutput(true);
    // conn.setUseCaches(false);
    // conn.setRequestMethod("POST");
    // conn.setRequestProperty("Connection", "Keep-Alive");
    // conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
    //
    // DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
    // dos.writeBytes(twoHyphens + boundary + lineEnd);
    // dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" +
    // file.getName() + "\"" + lineEnd);
    // dos.writeBytes(lineEnd);
    //
    // // create a buffer of maximum size
    // int bytesAvailable = fileInputStream.available();
    // int maxBufferSize = 1024;
    // int bufferSize = Math.min(bytesAvailable, maxBufferSize);
    // byte[] buffer = new byte[bufferSize];
    // // read file and write it into form...
    // int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    // while( bytesRead > 0 ) {
    // dos.write(buffer, 0, bufferSize);
    // bytesAvailable = fileInputStream.available();
    // bufferSize = Math.min(bytesAvailable, maxBufferSize);
    // bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    // }
    // dos.writeBytes(lineEnd);
    // dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
    // fileInputStream.close();
    // dos.flush();
    //
    // InputStream is = conn.getInputStream();
    // int ch;
    // StringBuffer b = new StringBuffer();
    // while( (ch = is.read()) != -1 ) {
    // b.append((char) ch);
    // }
    // String s = b.toString();
    // Log.i("Response", s);
    // dos.close();
    // return s;
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    // return null;
    // }
    // }

    // public void executeMultipartPost() throws Exception {
    //
    // try {
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // bm.compress(CompressFormat.JPEG, 75, bos);
    // byte[] data = bos.toByteArray();
    // HttpClient httpClient = new DefaultHttpClient();
    // HttpPost postRequest = new HttpPost(
    // "http://10.0.2.2/cfc/iphoneWebservice.cfc?returnformat=json&amp;method=testUpload");
    // ByteArrayBody bab = new ByteArrayBody(data, "forest.jpg");
    // // File file= new File("/mnt/sdcard/forest.png");
    // // FileBody bin = new FileBody(file);
    // MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
    // reqEntity.addPart("uploaded", bab);
    // reqEntity.addPart("photoCaption", new StringBody("sfsdfsdf"));
    // postRequest.setEntity(reqEntity);
    // HttpResponse response = httpClient.execute(postRequest);
    // BufferedReader reader = new BufferedReader(new
    // InputStreamReader(response.getEntity().getContent(), "UTF-8"));
    // String sResponse;
    // StringBuilder s = new StringBuilder();
    // while( (sResponse = reader.readLine()) != null ) {
    // s = s.append(sResponse);
    // }
    // System.out.println("Response: " + s);
    // } catch (Exception e) {
    // Log.e(e.getClass().getName(), e.getMessage());
    // }
    // }

}