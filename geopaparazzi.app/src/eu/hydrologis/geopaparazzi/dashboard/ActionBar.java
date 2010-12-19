package eu.hydrologis.geopaparazzi.dashboard;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.QuickAction;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

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
    private static String validPointsString;
    private static String distanceString;
    private static String satellitesString;

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
        ImageButton noteButton = (ImageButton) actionBarView.findViewById(R.id.action_bar_note);
        final int compassButtonId = R.id.action_bar_compass;
        ImageButton compassButton = (ImageButton) actionBarView.findViewById(compassButtonId);
        compassButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(compassButtonId, v);
            }
        });

    }

    private void initVars() {
        Context context = actionBarView.getContext();
        timeString = context.getString(R.string.utctime);
        lonString = context.getString(R.string.lon);
        latString = context.getString(R.string.lat);
        altimString = context.getString(R.string.altim);
        azimString = context.getString(R.string.azimuth);
        validPointsString = context.getString(R.string.log_points);
        distanceString = context.getString(R.string.log_distance);
        satellitesString = context.getString(R.string.satellite_num);
        nodataString = context.getString(R.string.nogps_data);

    }

    public static ActionBar getActionBar( Activity activity, int activityId, ApplicationManager applicationManager ) {
        View view = activity.findViewById(activityId);
        return new ActionBar(view, applicationManager);
    }

    public void hideButton( int buttonId, int... others ) {
        View firstButton = actionBarView.findViewById(buttonId);
        if (firstButton != null) {
            firstButton.setVisibility(View.GONE);
        }
        if (others != null) {
            for( int otherId : others ) {
                View otherButton = actionBarView.findViewById(otherId);
                if (otherButton != null) {
                    otherButton.setVisibility(View.GONE);
                }
            }
        }
    }

    public void showButton( int buttonId, int... others ) {
        View firstButton = actionBarView.findViewById(buttonId);
        if (firstButton != null) {
            firstButton.setVisibility(View.VISIBLE);
        }
        if (others != null) {
            for( int otherId : others ) {
                View otherButton = actionBarView.findViewById(otherId);
                if (otherButton != null) {
                    otherButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void registerListenerForButton( int buttonId, OnClickListener listener ) {
        View button = actionBarView.findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }

    public void startAnimation( int buttonId, int animationId ) {
        View button = actionBarView.findViewById(buttonId);
        if (button != null) {
            Animation buttonAnimation = AnimationUtils.loadAnimation(button.getContext(), animationId);
            button.startAnimation(buttonAnimation);
        }
    }

    public void stopAnimation( int buttonId ) {
        View button = actionBarView.findViewById(buttonId);
        if (button != null) {
            Animation animation = button.getAnimation();
            if (animation != null) {
                DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(2.f);
                animation.setInterpolator(decelerateInterpolator);
                animation.setRepeatCount(0);
            }
        }
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
            GpsLocation loc = applicationManager.getLoc();
            Context context = actionBarView.getContext();
            if (loc != null) {
                Intent intent = new Intent(Constants.TAKE_NOTE);
                context.startActivity(intent);
            } else {
                ApplicationManager.openDialog(R.string.gpslogging_only, context);
            }
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

    public void setTitleWithCustomFont( int titleResourceId, int titleViewId, String assetFontFile ) {
        Context context = actionBarView.getContext();
        AssetManager assets = context.getResources().getAssets();
        Typeface typeface = Typeface.createFromAsset(assets, assetFontFile); // "fonts/title_font.ttf"

        TextView textView = (TextView) actionBarView.findViewById(titleViewId);
        if (textView != null) {
            textView.setText(titleResourceId);
            // textView.setTypeface(typeface);
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
        if (loc == null) {
            // Log.d("COMPASSVIEW", "Location from gps is null!");
            sb.append(nodataString);
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
        }
        return sb.toString();
    }

}
