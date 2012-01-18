package eu.hydrologis.geopaparazzi.util;

import static eu.hydrologis.geopaparazzi.util.Constants.DECIMATION_FACTOR;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GpUtilities {

    public static int getDecimationFactor( Context context ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String decimationFactorStr = preferences.getString(DECIMATION_FACTOR, "5"); //$NON-NLS-1$
        int decimationFactor = 5;
        try {
            decimationFactor = Integer.parseInt(decimationFactorStr);
        } catch (Exception e) {
            // use default
        }
        return decimationFactor;
    }
}
