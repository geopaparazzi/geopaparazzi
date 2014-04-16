///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.hydrologis.geopaparazzi.util;
//
//import java.util.Date;
//
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.text.Editable;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.EditText;
//import eu.geopaparazzi.library.gps.GpsManager;
//import eu.geopaparazzi.library.util.TimeUtilities;
//import eu.geopaparazzi.library.util.Utilities;
//import eu.hydrologis.geopaparazzi.R;
//import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
//import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
//import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
//import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
//import eu.hydrologis.geopaparazzi.maps.DataManager;
//
///**
// * A factory for quick actions.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class QuickActionsFactory {
//
//    /**
//     * @param actionBar the action bar.
//     * @param qa the quick action.
//     * @param context  the context to use.
//     * @return the action.
//     */
//    public static ActionItem getStartLogQuickAction( final ActionBar actionBar, final QuickAction qa, final Context context ) {
//        ActionItem startLogQuickaction = new ActionItem();
//        startLogQuickaction.setTitle("Start Log"); //$NON-NLS-1$
//        startLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_start_log));
//        startLogQuickaction.setOnClickListener(new OnClickListener(){
//            public void onClick( View v ) {
//                final GpsManager gpsManager = GpsManager.getInstance(context);
//                if (gpsManager.hasFix()) {
//                    final String defaultLogName = "log_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$
//                    final EditText input = new EditText(context);
//                    input.setText(defaultLogName);
//                    new AlertDialog.Builder(context).setTitle(R.string.gps_log).setMessage(R.string.gps_log_name).setView(input)
//                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
//                                public void onClick( DialogInterface dialog, int whichButton ) {
//                                    Editable value = input.getText();
//                                    String newName = value.toString();
//                                    if (newName == null || newName.length() < 1) {
//                                        newName = defaultLogName;
//                                    }
//
//                                    DaoGpsLog daoGpsLog = new DaoGpsLog();
//                                    gpsManager.startDatabaseLogging(context, newName, daoGpsLog);
//                                    actionBar.checkLogging();
//                                    DataManager.getInstance().setLogsVisible(true);
//                                }
//                            }).setCancelable(false).show();
//                } else {
//                    Utilities.messageDialog(context, R.string.gpslogging_only, null);
//                }
//                qa.dismiss();
//            }
//        });
//        return startLogQuickaction;
//    }
//
//    /**
//     * @param actionBar the action bar.
//     * @param qa the quick action.
//     * @param context  the context to use.
//     * @return the action.
//     */
//    public static ActionItem getStopLogQuickAction( final ActionBar actionBar, final QuickAction qa, final Context context ) {
//        ActionItem stopLogQuickaction = new ActionItem();
//        stopLogQuickaction.setTitle("Stop Log"); //$NON-NLS-1$
//        stopLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_stop_log));
//        stopLogQuickaction.setOnClickListener(new OnClickListener(){
//            public void onClick( View v ) {
//                GpsManager gpsManager = GpsManager.getInstance(context);
//                if (gpsManager.isDatabaseLogging()) {
//                    gpsManager.stopDatabaseLogging(context);
//                    actionBar.checkLogging();
//                }
//                qa.dismiss();
//            }
//        });
//        return stopLogQuickaction;
//    }
//
//    // /**
//    // * Create a {@link QuickAction} for sketches collection.
//    // *
//    // * @param qa the {@link QuickAction} to attache the {@link ActionItem} to.
//    // * @param activity the context to use.
//    // * @return the {@link ActionItem} created.
//    // */
//    // public ActionItem getSketchQuickAction( final QuickAction qa, final Activity activity, final
//    // int requestCode ) {
//    // ActionItem pictureQuickaction = new ActionItem();
//    // pictureQuickaction.setTitle("Geosketch");
//    // pictureQuickaction.setIcon(activity.getResources().getDrawable(R.drawable.quickaction_sketch));
//    // pictureQuickaction.setOnClickListener(new OnClickListener(){
//    // public void onClick( View v ) {
//    // try {
//    // boolean isValid = false;
//    // if (GpsManager.getInstance(activity).hasFix()) {
//    // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
//    // double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
//    // // double[] gpsLocation =
//    // // PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
//    // if (gpsLocation != null) {
//    // java.util.Date currentDate = new java.util.Date();
//    // String currentDatestring = LibraryConstants.TIMESTAMPFORMATTER.format(currentDate);
//    // File mediaDir = null;
//    // try {
//    // mediaDir = ResourcesManager.getInstance(activity).getMediaDir();
//    // } catch (Exception e) {
//    // e.printStackTrace();
//    // }
//    // File newImageFile = new File(mediaDir, "SKETCH_" + currentDatestring + ".png");
//    // MarkersUtilities.launchForResult(activity, newImageFile, gpsLocation, requestCode);
//    //
//    // // Intent cameraIntent = new Intent(activity, DrawingActivity.class);
//    // // cameraIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
//    // // cameraIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
//    // // cameraIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
//    // // activity.startActivityForResult(cameraIntent, requestCode);
//    // isValid = true;
//    // }
//    // }
//    // if (!isValid)
//    // Utilities.messageDialog(activity, R.string.gpslogging_only, null);
//    // qa.dismiss();
//    // } catch (Exception e) {
//    // GPLog.error(this, e.getLocalizedMessage(), e);
//    // }
//    // }
//    // });
//    // return pictureQuickaction;
//    // }
// }
