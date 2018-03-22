package eu.geopaparazzi.library.auth;

import android.os.Parcel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import eu.geopaparazzi.library.network.requests.Requests;

import static eu.geopaparazzi.library.network.NetworkUtilities.getB64Auth;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public class BasicHttpServerAuthHandler extends AbstractServerAuthHandler {
    public static final String TYPE_NAME = "SERVERAUTH.HANDLER.BASIC_HTTPS_AUTH";
    private final HashSet<String> paramNames = new HashSet<String>();
/*
    public static final String PARAM_USER_NAME = "SERVERAUTH.HANDLER.BASIC_HTTPS_AUTH.URL";
    public static final String PARAM_PASSWORD = "SERVERAUTH.HANDLER.BASIC_HTTPS_AUTH.PASSWORD";
    public static final String PARAM_SERVER_URL = "SERVERAUTH.HANDLER.BASIC_HTTPS_AUTH.URL";
*/
    public static final String PARAM_USER_NAME = "stage_user_key";
    public static final String PARAM_PASSWORD = "stage_pwd_key";
    public static final String PARAM_SERVER_URL = "stage_server_key";

    protected BasicHttpServerAuthHandler() {
        paramNames.add(PARAM_USER_NAME);
        paramNames.add(PARAM_PASSWORD);
        paramNames.add(PARAM_SERVER_URL);
    }

    protected BasicHttpServerAuthHandler(Parcel in) {
        this();
        parameters = (HashMap<String, String>) in.readHashMap(HashMap.class.getClassLoader());
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Set<String> getParamNames() {
        return paramNames;
    }

    @Override
    public HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection conn = Requests.makeNewConnection(url);
        String user = getParamValue(PARAM_USER_NAME);
        String password = getParamValue(PARAM_PASSWORD);
        if (user != null && password != null && user.trim().length() > 0 && password.trim().length() > 0) {
            conn.setRequestProperty("Authorization", getB64Auth(user, password));
        }
        return conn;
    }

    public static final Creator<BasicHttpServerAuthHandler> CREATOR = new Creator<BasicHttpServerAuthHandler>() {
        @Override
        public BasicHttpServerAuthHandler createFromParcel(Parcel in) {
            return new BasicHttpServerAuthHandler(in);
        }

        @Override
        public BasicHttpServerAuthHandler[] newArray(int size) {
            return new BasicHttpServerAuthHandler[size];
        }
    };

}
