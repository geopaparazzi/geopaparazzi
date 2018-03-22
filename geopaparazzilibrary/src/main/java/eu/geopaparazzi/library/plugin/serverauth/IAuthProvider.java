package eu.geopaparazzi.library.plugin.serverauth;

import android.os.Parcelable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Set;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public interface IAuthProvider extends Parcelable {
    String getTypeName();

    Set<String> getParamNames();

    public HttpURLConnection getConnection(String url) throws IOException;


    public String getParamValue(String paramName);

    public void setParamValue(String paramName, String paramValue);

    /**
     * Gets the order in which the provider should be shown in the list of providers
     * The Activity showing the providers is free to use or ignore this proposed order.
     * Zero will be the top-most menu entry and 500 is the default value.
     *
     * @return An integer number >= 0, where 0 means the top most
     * item. Negative numbers means no particular order.
     */
    int getOrder();
}
