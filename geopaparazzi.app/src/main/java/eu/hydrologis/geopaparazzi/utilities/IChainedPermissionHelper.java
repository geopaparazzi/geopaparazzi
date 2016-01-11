package eu.hydrologis.geopaparazzi.utilities;

import android.app.Activity;
import android.content.Context;

/**
 * An interface to handle chained permissions.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IChainedPermissionHelper {
    /**
     * Checks if the permission is granted.
     *
     * @param context the context to use.
     * @return true, if permission is granted.
     */
    boolean hasPermission(Context context);

    /**
     * Request the permission.
     *
     * @param activity the asking activity.
     */
    void requestPermission(Activity activity);

    /**
     * Checks if the permission has finally been granted.
     * <p>
     * <p>To be checked in the <code>onRequestPermissionsResult</code> method once the request comes back.</p>
     *
     * @param requestCode  the requestcode used.
     * @param grantResults the results array.
     * @return true if permission has been granted.
     */
    boolean hasGainedPermission(int requestCode, int[] grantResults);

    /**
     * Get the next non granted permission in the chain.
     *
     * @param context the context to use.
     * @return the next non granted permission or null if all are granted.
     */
    IChainedPermissionHelper getNextWithoutPermission(Context context);
}
