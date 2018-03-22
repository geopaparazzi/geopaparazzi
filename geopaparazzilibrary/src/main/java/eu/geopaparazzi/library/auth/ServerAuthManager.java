package eu.geopaparazzi.library.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import eu.geopaparazzi.library.plugin.serverauth.IAuthProvider;
import eu.geopaparazzi.library.profiles.ProfilesHandler;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public enum ServerAuthManager {

    INSTANCE;

    public IAuthProvider getProvider(Context context) {
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String providerType = mPreferences.getString(ProfilesHandler.PREF_KEY_SERVER_AUTH_PROVIDER, "");
        IAuthProvider provider = null;
        // Try to find a declaration of this type on the application manifest
        ApplicationInfo ai = null;
        String className = context.getApplicationInfo().metaData.getString(providerType);
        if (className!=null) {
            try {
                IAuthProvider handler = (IAuthProvider) Class.forName(className).newInstance();
                setParams(handler, mPreferences);
            } catch (ClassNotFoundException e1) {
            } catch (IllegalAccessException e1) {
            } catch (InstantiationException e1) {
            }
        }
        IAuthProvider handler = new BasicHttpServerAuthHandler();
        setParams(handler, mPreferences);
        return handler;
    }

    private void setParams(IAuthProvider handler, SharedPreferences mPreferences) {
        for (String prefKey: handler.getParamNames()) {
            String prefValue = mPreferences.getString(prefKey, null);
            handler.setParamValue(prefKey, prefValue);
        }

    }
}
