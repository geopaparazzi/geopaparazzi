package eu.geopaparazzi.library.util.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * @author javacodegeeks
 */
public class ProximityIntentReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1000;

    @Override
    public void onReceive( Context context, Intent intent ) {

        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        Boolean entering = intent.getBooleanExtra(key, false);

        if (entering) {
            if (GPLog.LOG)
                GPLog.addLogEntry(getClass().getSimpleName(), "entering proximity radius"); //$NON-NLS-1$
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Notification notification = createNotification();
            notification.setLatestEventInfo(context, context.getString(R.string.proximity_alert),
                    context.getString(R.string.approaching_poi), pendingIntent);
            notificationManager.notify(NOTIFICATION_ID, notification);

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeProximityAlert(pendingIntent);
            context.unregisterReceiver(this);
        } else {
            if (GPLog.LOG)
                GPLog.addLogEntry(getClass().getSimpleName(), "exiting proximity radius"); //$NON-NLS-1$
        }

    }

    private static Notification createNotification() {
        Notification notification = new Notification();

        notification.icon = R.drawable.current_position;
        notification.when = System.currentTimeMillis();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;

        notification.ledARGB = Color.RED;
        notification.ledOnMS = 1500;
        notification.ledOffMS = 1500;

        return notification;
    }

}
