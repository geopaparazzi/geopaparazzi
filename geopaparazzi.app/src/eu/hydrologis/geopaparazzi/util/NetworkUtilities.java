package eu.hydrologis.geopaparazzi.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("nls")
public class NetworkUtilities {

    public static final int maxBufferSize = 4096;

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
        conn.setChunkedStreamingMode(0);
        conn.setUseCaches(false);

        if (user != null && password != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(user);
            stringBuilder.append(":");
            stringBuilder.append(password);
            conn.setRequestProperty("Authorization", "Basic " + Base64.encode(stringBuilder.toString().getBytes()));
        }
        conn.connect();

        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(file);

            byte[] buffer = new byte[maxBufferSize];
            int bytesRead = in.read(buffer, 0, maxBufferSize);
            while( bytesRead > 0 ) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer, 0, maxBufferSize);
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
            conn.setChunkedStreamingMode(0);
            conn.setUseCaches(false);
            if (user != null && password != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(user);
                stringBuilder.append(":");
                stringBuilder.append(password);
                conn.setRequestProperty("Authorization", "Basic " + Base64.encode(stringBuilder.toString().getBytes()));
            }
            conn.connect();

            // Make server believe we are form data...
            wr = new BufferedOutputStream(conn.getOutputStream());
            byte[] bytes = string.getBytes();
            wr.write(bytes);
            wr.flush();

            String responseMessage = conn.getResponseMessage();
            return responseMessage;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (wr != null)
                wr.close();
            if (conn != null)
                conn.disconnect();
        }
    }

}