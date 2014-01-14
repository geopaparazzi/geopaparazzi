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
package eu.geopaparazzi.library.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * An simple {@link AsyncTask} string based wrapper.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class StringAsyncTask extends AsyncTask<String, Integer, String> {
    private Context context;
    private ProgressDialog progressDialog;

    /**
     * @param context  the context to use.
     */
    public StringAsyncTask( Context context ) {
        this.context = context;
    }

    /**
     * Also create a {@link ProgressDialog} and start it.
     * 
     * @param title a title.
     * @param message a message.
     * @param cancelable if it is cancelable.
     * @param max the max progress. If <code>null</code>, indeterminate is used.
     */
    public void startProgressDialog( String title, String message, boolean cancelable, Integer max ) {
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(cancelable);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (max == null) {
            progressDialog.setIndeterminate(true);
        } else {
            progressDialog.setProgress(0);
            progressDialog.setMax(max);
        }
        progressDialog.show();
    }

    protected String doInBackground( String... params ) {
        return doBackgroundWork();
    }

    protected void onProgressUpdate( Integer... progress ) {
        if (progressIsOk()) {
            progressDialog.setProgress(progress[0]);
        }
    }

    protected void onPostExecute( String response ) {
        if (progressIsOk()) {
            progressDialog.dismiss();
        }
        doUiPostWork(response);
    }

    private boolean progressIsOk() {
        return progressDialog != null && progressDialog.isShowing();
    }

    /**
     * Dispose any connected resource.
     */
    public void dispose() {
        if (progressIsOk()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Do the background work (non UI).
     * 
     * <p>To update progress:
     * <code>
     * publishProgress(1);
     * // and to escape early if cancel() is called
     * if (isCancelled())
     *    break;
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
    protected abstract void doUiPostWork( String response );
}