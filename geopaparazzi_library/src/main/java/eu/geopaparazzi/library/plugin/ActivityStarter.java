/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ActivityStarter implements IActivitySupporter {
    private Intent intent = new Intent();
    private Context context;

    public ActivityStarter(Context context) {
        this.context = context;
    }


    @Override
    public void startActivity(Intent intent) {
        context.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager();
        }
        return null;
    }

//    public void start(Context context, String action) {
//        intent.setAction(action);
//        List<ResolveInfo> info = context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
//        if (intent.resolveActivity(context.getPackageManager()) != null) {
//            context.startActivity(intent);
//        }
//    }
}
