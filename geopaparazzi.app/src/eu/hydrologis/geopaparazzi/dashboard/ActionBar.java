package eu.hydrologis.geopaparazzi.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

public class ActionBar {

    private final View actionBarView;

    public ActionBar( View actionBarView ) {
        this.actionBarView = actionBarView;
    }

    public static ActionBar getActionBar( Activity activity, int activityId ) {
        View view = activity.findViewById(activityId);
        return new ActionBar(view);
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

}
