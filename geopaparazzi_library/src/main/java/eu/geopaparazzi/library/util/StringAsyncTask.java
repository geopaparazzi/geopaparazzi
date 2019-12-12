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
package eu.geopaparazzi.library.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.Window;

/**
 * An simple {@link AsyncTask} string based wrapper.
 * <p/>
 * <p>example usage:</p>
 * <pre>
 * StringAsyncTask task = new StringAsyncTask(this) {
 * protected String doBackgroundWork() {
 * try {
 * int index = 0;
 * for (...){
 * // do stuff
 * publishProgress(index);
 * }
 * } catch (Exception e) {
 * return "ERROR: " + e.getLocalizedMessage();
 * }
 * return "";
 * }
 *
 * protected void doUiPostWork(String response) {
 * dispose();
 * if (response.length() != 0) {
 * GPDialogs.warningDialog(YourActivity.this, response, null);
 * }
 * // do UI stuff
 * }
 * };
 * task.setProgressDialog("TITLE", "Process...", false, progressCount);
 * task.execute();
 * </pre>
 * <p>
 * <p>Remember to dispose the progressdialog in the activity destroy method.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class StringAsyncTask extends AsyncTask<String, Integer, String> {
    private Context context;
    private ProgressDialog progressDialog;
    private String title;
    private String message;
    private boolean cancelable;
    private Integer max;
    private boolean doProgress = false;

    /**
     * @param context the context to use.
     */
    public StringAsyncTask(Context context) {
        this.context = context;
    }

    /**
     * Also create a {@link ProgressDialog} and start it.
     *
     * @param title      a title.
     * @param message    a message.
     * @param cancelable if it is cancelable.
     * @param max        the max progress. If <code>null</code>, indeterminate is used.
     */
    public void setProgressDialog(String title, String message, boolean cancelable, Integer max) {
        this.title = title;
        this.message = message;
        this.cancelable = cancelable;
        this.max = max;
        doProgress = true;
    }

    @Override
    protected void onPreExecute() {
        if (doProgress) {
            progressDialog = new ProgressDialog(context);
            if (title == null) {
                progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            } else {
                progressDialog.setTitle(title);
            }
            progressDialog.setMessage(message);
            progressDialog.setCancelable(cancelable);
            if (max == null) {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setIndeterminate(true);
            } else {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setIndeterminate(false);
                progressDialog.setProgress(0);
                progressDialog.setMax(max);
            }
            progressDialog.show();
        }
    }

    protected String doInBackground(String... params) {
        return doBackgroundWork();
    }

    protected void onProgressUpdate(Integer... progress) {
        if (progressIsOk()) {
            progressDialog.setProgress(progress[0]);
        }
    }

    protected void onPostExecute(String response) {
        dismissProgressDialog();
        doUiPostWork(response);
    }

    private boolean progressIsOk() {
        return progressDialog != null && progressDialog.isShowing();
    }

    /**
     * Dispose any connected resource.
     */
    public void dispose() {
        dismissProgressDialog();
    }

    private void dismissProgressDialog() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isDestroyed()) {
                return;
            }
        }
        if (progressIsOk()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Do the background work (non UI).
     * <p/>
     * <p>To update progress:
     * <code>
     * publishProgress(1);
     * // and to escape early if cancel() is called
     * if (isCancelled())
     * break;
     * </code>
     *
     * @return the result of the work.
     */
    protected abstract String doBackgroundWork();

    /**
     * Do the UI work after the {@link #doBackgroundWork()}.
     *
     * @param response the response coming from the {@link #doBackgroundWork()}.
     */
    protected abstract void doUiPostWork(String response);

}