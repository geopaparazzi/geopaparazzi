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
package eu.hydrologis.geopaparazzi.dashboard;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.QuickAction;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * 
 * The action bar utilities class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ActionBar {
    private static DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
    private final View actionBarView;
    private final ApplicationManager applicationManager;
    private ActionItem infoQuickaction;

    private static String nodataString;
    private static String timeString;
    private static String lonString;
    private static String latString;
    private static String altimString;
    private static String azimString;
    private static String loggingString;
    private static String gpsonString;
    // private static String validPointsString;
    // private static String distanceString;
    // private static String satellitesString;

    public ActionBar( View actionBarView, ApplicationManager applicationManager ) {
        this.actionBarView = actionBarView;
        this.applicationManager = applicationManager;

        initVars();
        createQuickActions();

        final int gpsInfoButtonId = R.id.action_bar_info;
        ImageButton gpsInfoButton = (ImageButton) actionBarView.findViewById(gpsInfoButtonId);
        gpsInfoButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(gpsInfoButtonId, v);
            }
        });

        final int noteButtonId = R.id.action_bar_note;
        ImageButton noteButton = (ImageButton) actionBarView.findViewById(noteButtonId);
        noteButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(noteButtonId, v);
            }
        });

        final int compassButtonId = R.id.action_bar_compass;
        ImageButton compassButton = (ImageButton) actionBarView.findViewById(compassButtonId);
        compassButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(compassButtonId, v);
            }
        });

        checkLogging();
    }

    public View getActionBarView() {
        return actionBarView;
    }

    private void initVars() {
        Context context = actionBarView.getContext();
        timeString = context.getString(R.string.utctime);
        lonString = context.getString(R.string.lon);
        latString = context.getString(R.string.lat);
        altimString = context.getString(R.string.altim);
        azimString = context.getString(R.string.azimuth);
        // validPointsString = context.getString(R.string.log_points);
        // distanceString = context.getString(R.string.log_distance);
        // satellitesString = context.getString(R.string.satellite_num);
        nodataString = context.getString(R.string.nogps_data);
        loggingString = context.getString(R.string.text_logging);
        gpsonString = context.getString(R.string.text_gpson);

    }

    public static ActionBar getActionBar( Activity activity, int activityId, ApplicationManager applicationManager ) {
        View view = activity.findViewById(activityId);
        return new ActionBar(view, applicationManager);
    }

    public void setTitle( int titleResourceId, int titleViewId ) {
        TextView textView = (TextView) actionBarView.findViewById(titleViewId);
        if (textView != null) {
            textView.setText(titleResourceId);
        }
    }

    public void push( int id, View v ) {
        switch( id ) {
        case R.id.action_bar_info: {
            QuickAction qa = new QuickAction(v);

            infoQuickaction.setTitle(createGpsInfo());
            qa.addActionItem(infoQuickaction);
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();

            break;
        }
        case R.id.action_bar_note: {

            eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction qa = new eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction(
                    v);
            qa.addActionItem(applicationManager.getNotesQuickAction());
            qa.addActionItem(applicationManager.getPicturesQuickAction());
            qa.addActionItem(applicationManager.getAudioQuickAction());
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();
            break;
        }
        case R.id.action_bar_compass: {
            Context context = actionBarView.getContext();
            Intent intent = new Intent(Constants.VIEW_COMPASS);
            context.startActivity(intent);
            break;
        }
        default:
            break;
        }
    }

    private void createQuickActions() {
        /*
         * LOG QUICKACTIONS
         */
        infoQuickaction = new ActionItem();
        infoQuickaction.setTitle("Info:\ninfo1\ninfo2");
        // Context context = actionBarView.getContext();
        // infoQuickaction.setIcon(context.getResources().getDrawable(R.drawable.action_bar_info));
        // infoQuickaction.setOnClickListener(new OnClickListener(){
        // public void onClick( View v ) {
        // }
        // });

    }

    private String createGpsInfo() {
        double azimuth = applicationManager.getNormalAzimuth();
        GpsLocation loc = applicationManager.getLoc();

        StringBuilder sb = new StringBuilder();
        if (loc == null || !applicationManager.isGpsEnabled()) {
            // Logger.d("COMPASSVIEW", "Location from gps is null!");
            sb.append(nodataString);
            sb.append("\n");
            sb.append(gpsonString);
            sb.append(": ").append(applicationManager.isGpsEnabled()); //$NON-NLS-1$
            sb.append("\n");
        } else {
            sb.append(timeString);
            sb.append(" ").append(loc.getTimeString()); //$NON-NLS-1$
            sb.append("\n");
            sb.append(latString);
            sb.append(" ").append(formatter.format(loc.getLatitude())); //$NON-NLS-1$
            sb.append("\n");
            sb.append(lonString);
            sb.append(" ").append(formatter.format(loc.getLongitude())); //$NON-NLS-1$
            sb.append("\n");
            sb.append(altimString);
            sb.append(" ").append((int) loc.getAltitude()); //$NON-NLS-1$
            sb.append("\n");
            sb.append(azimString);
            sb.append(" ").append((int) (360 - azimuth)); //$NON-NLS-1$
            sb.append("\n");
            sb.append(loggingString);
            sb.append(": ").append(applicationManager.isGpsLogging()); //$NON-NLS-1$
        }
        return sb.toString();
    }

    public void checkLogging() {
        // activity.runOnUiThread(new Runnable(){
        // public void run() {
        View gpsOnOffView = actionBarView.findViewById(R.id.gpsOnOff);
        Resources resources = gpsOnOffView.getResources();

        if (applicationManager.isGpsEnabled()) {
            if (applicationManager.isGpsLogging()) {
                gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_logging));
            } else {
                gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_notlogging));
            }
        } else {
            gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_off));
        }
        // }
        // });

    }
}
