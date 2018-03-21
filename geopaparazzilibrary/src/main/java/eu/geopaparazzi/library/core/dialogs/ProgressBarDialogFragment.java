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

package eu.geopaparazzi.library.core.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.network.download.DownloadFileIntentService;
import eu.geopaparazzi.library.network.download.DownloadResultReceiver;

import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.max;

/**
 * Progress bar dialog.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProgressBarDialogFragment extends DialogFragment {
    public static final String DOWNLOADABLES = "DOWNLOADABLES";
    public static final String LASTMESSAGE = "lastmessage";
    private Parcelable[] downloadables;

    private IProgressChangeListener iProgressChangeListener;
    private TextView messageView;
    private String startMsg = null;
    private ProgressBar progressBar;

    public interface IProgressChangeListener {
        void onProgressError(String error);

        void onProgressDone(String msg);
    }


    public static ProgressBarDialogFragment newInstance(Parcelable[] downloadables) {
        ProgressBarDialogFragment f = new ProgressBarDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(DOWNLOADABLES, downloadables);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            downloadables = arguments.getParcelableArray(DOWNLOADABLES);
        } else {
            startMsg = savedInstanceState.getString(LASTMESSAGE);
        }
        // get user and pwd
    }


    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        try {

            View newProjectDialogView = activity.getLayoutInflater().inflate(
                    R.layout.fragment_dialog_progress, null);
            builder.setView(newProjectDialogView); // add GUI to dialog

            messageView = newProjectDialogView.findViewById(R.id.downloadmessageview);
            progressBar = newProjectDialogView.findViewById(R.id.downloadprogressbar);
            progressBar.setMax(max);

            builder.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            DownloadFileIntentService.isCancelled = true;
                        }
                    }
            );

            if (startMsg == null) {
                Intent intent = new Intent(activity, DownloadFileIntentService.class);
                intent.setAction(DownloadResultReceiver.DOWNLOAD_ACTION);
                intent.putExtra(DownloadResultReceiver.EXTRA_KEY, new DownloadResultReceiver(new Handler()) {
                    @Override
                    public ProgressBar getProgressBar() {
                        return progressBar;
                    }

                    @Override
                    public TextView getProgressView() {
                        return messageView;
                    }

                    @Override
                    public ProgressBarDialogFragment.IProgressChangeListener getProgressChangeListener() {
                        return iProgressChangeListener;
                    }
                });
                intent.putExtra(DownloadResultReceiver.EXTRA_FILES_KEY, downloadables);
                activity.startService(intent);
            } else {
                messageView.setText(startMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LASTMESSAGE, messageView.getText().toString());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IProgressChangeListener) {
            iProgressChangeListener = (IProgressChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        iProgressChangeListener = null;
    }
}
