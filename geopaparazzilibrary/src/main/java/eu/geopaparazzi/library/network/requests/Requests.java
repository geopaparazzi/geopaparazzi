package eu.geopaparazzi.library.network.requests;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import eu.geopaparazzi.library.plugin.serverauth.IAuthProvider;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public class Requests {
    public static final String SLASH = "/";

    public static ResponsePromise get(String url, String requestParameters, IAuthProvider authProvider) throws IOException {
        if (requestParameters != null && requestParameters.length() > 0) {
            url += "?" + requestParameters;
        }
        HttpURLConnection conn;
        if (authProvider==null) {
            conn = makeNewConnection(normalizeUrl(url));
        }
        else {
            conn = authProvider.getConnection(normalizeUrl(url));
        }
        conn.setRequestMethod("GET");
        return new ResponsePromise(conn);
    }

    public static HttpURLConnection makeNewConnection(String fileUrl) throws IOException {
        URL url = new URL(normalizeUrl(fileUrl));
        return (HttpURLConnection) url.openConnection();
    }


    private static String normalizeUrl(String url) {
        return normalizeUrl(url, false);
    }

    private static String normalizeUrl(String url, boolean addSlash) {
        if ((!url.startsWith("http://")) && (!url.startsWith("https://"))) {
            url = "http://" + url;
        }
        if (addSlash && !url.endsWith(SLASH)) {
            url = url + SLASH;
        }
        return url;
    }

    /**
     * Gets a destination file using the provided outputFolder and the name provided by the connection.
     * If the outputFolder parameter is not a folder, it is returned as-is.
     *
     * @param outputFolder
     * @param connection
     * @return
     */
    public static File getFile(File outputFolder, HttpURLConnection connection) {
        if (outputFolder.isDirectory()) {
            // try to get the header
            String headerField = connection.getHeaderField("Content-Disposition");
            String fileName = null;
            if (headerField != null) {
                String[] split = headerField.split(";");
                for (String string : split) {
                    String pattern = "filename=";
                    if (string.toLowerCase().startsWith(pattern)) {
                        fileName = string.replaceFirst(pattern, "");
                        break;
                    }
                }
            }
            if (fileName == null) {
                // give a name
                fileName = "FILE_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
            }
            return new File(outputFolder, fileName);
        }
        return outputFolder;
    }

}
