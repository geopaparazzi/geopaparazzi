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
package eu.hydrologis.geopaparazzi.maps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * A service to support the {@link MapsActivity}.
 * <p/>
 * <p/>
 * use this to start and trigger a service</br>
 * <code>Intent i= new Intent(context, MapsSupportService.class)</code>;</br>
 * add data to the intent</br>
 * <code>i.putExtra("KEY1", "Value to be used by the service");</br>
 * context.startService(i);</code>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class MapsSupportService extends Service {
    /**
     * Intent key to pass the boolean to start the MAPSSUPPORTSERVICE_SERVICE.
     */
    public static final String START_MAPSSUPPORT_SERVICE = "START_MAPSSUPPORT_SERVICE";

    /**
     * Intent key to use for broadcasts.
     */
    public static final String MAPSSUPPORT_SERVICE_BROADCAST_NOTIFICATION = "eu.hydrologis.geopaparazzi.maps.MapsSupportService";

    /**
     * Intent key to use for map redraw requests.
     */
    public static final String REDRAW_MAP_REQUEST = "REDRAW_MAP_REQUEST";

    /**
     * Intent key to use for map reread requests.
     */
    public static final String REREAD_MAP_REQUEST = "REREAD_MAP_REQUEST";

    /**
     * Intent key to use for center on position requests.
     */
    public static final String CENTER_ON_POSITION_REQUEST = "CENTER_ON_POSITION_REQUEST";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // GPLog.addLogEntry(this, "onStartCommand called with intent: " + intent);

        /*
         * If startService(intent) is called while the service is running, 
         * its onStartCommand() is also called. Therefore your service needs 
         * to be prepared that onStartCommand() can be called several times.
         */
        if (intent != null) {
            if (intent.hasExtra(REDRAW_MAP_REQUEST)) {
                Intent sendIntent = new Intent(MAPSSUPPORT_SERVICE_BROADCAST_NOTIFICATION);
                sendIntent.putExtra(REDRAW_MAP_REQUEST, true);
                sendBroadcast(sendIntent);
            } else if (intent.hasExtra(REREAD_MAP_REQUEST)) {
                Intent sendIntent = new Intent(MAPSSUPPORT_SERVICE_BROADCAST_NOTIFICATION);
                sendIntent.putExtra(REREAD_MAP_REQUEST, true);
                sendBroadcast(sendIntent);
            } else if (intent.hasExtra(CENTER_ON_POSITION_REQUEST)) {
                Intent sendIntent = new Intent(MAPSSUPPORT_SERVICE_BROADCAST_NOTIFICATION);
                sendIntent.putExtra(CENTER_ON_POSITION_REQUEST, true);
                double lon = intent.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                double lat = intent.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                sendIntent.putExtra(LibraryConstants.LONGITUDE, lon);
                sendIntent.putExtra(LibraryConstants.LATITUDE, lat);
                sendBroadcast(sendIntent);
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // /////////////////////////////////////////////
    // UNUSET METHODS
    // /////////////////////////////////////////////
    // @Override
    // public void onCreate() {
    // super.onCreate();
    // /*
    // * If the startService(intent) method is called and the service is not
    // * yet running, the service object is created and the onCreate()
    // * method of the service is called.
    // */
    // }
    //
    // @Override
    // public ComponentName startService( Intent service ) {
    // /*
    // * Once the service is started, the startService(intent) method in the
    // * service is called. It passes in the Intent object from the
    // * startService(intent) call.
    // */
    // return super.startService(service);
    // }
    //
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //
    // @Override
    // public boolean stopService( Intent name ) {
    // /*
    // * You stop a service via the stopService() method. No matter how
    // * frequently you called the startService(intent) method, one call
    // * to the stopService() method stops the service.
    // *
    // * A service can terminate itself by calling the stopSelf() method.
    // * This is typically done if the service finishes its work.
    // */
    // return super.stopService(name);
    // }
}
