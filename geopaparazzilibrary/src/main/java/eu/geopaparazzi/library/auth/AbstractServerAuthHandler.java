package eu.geopaparazzi.library.auth;

import android.os.Binder;
import android.os.Parcel;

import java.net.URLConnection;
import java.util.HashMap;
import java.util.Set;

import eu.geopaparazzi.library.plugin.serverauth.IAuthProvider;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public abstract class AbstractServerAuthHandler extends Binder implements IAuthProvider {
    protected final int DEFAULT_ORDER = 500;
    protected HashMap<String, String> parameters = new HashMap<String, String>();

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(parameters);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getParamValue(String paramName) {
        return parameters.get(paramName);
    }

    public void setParamValue(String paramName, String paramValue) {
        parameters.put(paramName, paramValue);
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }
}
