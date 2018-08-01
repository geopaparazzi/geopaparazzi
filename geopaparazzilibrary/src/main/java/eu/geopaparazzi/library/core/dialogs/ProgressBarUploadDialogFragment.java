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
import eu.geopaparazzi.library.network.upload.UploadFileIntentService;
import eu.geopaparazzi.library.network.upload.UploadResultReceiver;
import eu.geopaparazzi.library.util.LibraryConstants;

import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.max;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;

/**
 * Progress bar dialog for uploading files.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Brent Fraser (www.geoanalytic.com)
 */
public class ProgressBarUploadDialogFragment extends DialogFragment {

    public static final String UPLOADABLES = "UPLOADABLES";
    public static final String LASTMESSAGE = "lastmessage";

    private Parcelable[] uploadables;

    private IProgressChangeListener iProgressChangeListener;
    private TextView messageView;
    private String startMsg = null;
    private ProgressBar progressBar;

    private String user;
    private String pwd;

    public interface IProgressChangeListener {
        void onProgressError(String error);
        void onProgressDone(String msg);
    }

    public static ProgressBarUploadDialogFragment newInstance(Parcelable[] uploadables) {
        ProgressBarUploadDialogFragment f = new ProgressBarUploadDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(UPLOADABLES, uploadables);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            uploadables = arguments.getParcelableArray(UPLOADABLES);
        } else {
            startMsg = savedInstanceState.getString(LASTMESSAGE);
        }

        Bundle extras = getActivity().getIntent().getExtras();
        user = extras.getString(PREFS_KEY_USER);
        pwd = extras.getString(PREFS_KEY_PWD);
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
                            UploadFileIntentService.isCancelled = true;
                        }
                    }
            );

            if (startMsg == null) {
                Intent intent = new Intent(activity, UploadFileIntentService.class);
                intent.putExtra(LibraryConstants.PREFS_KEY_USER, user);
                intent.putExtra(LibraryConstants.PREFS_KEY_PWD, pwd);

                intent.setAction(UploadResultReceiver.UPLOAD_ACTION);
                intent.putExtra(UploadResultReceiver.EXTRA_KEY, new UploadResultReceiver(new Handler()) {
                    @Override
                    public ProgressBar getProgressBar() {
                        return progressBar;
                    }

                    @Override
                    public TextView getProgressView() {
                        return messageView;
                    }

                    @Override
                    public ProgressBarUploadDialogFragment.IProgressChangeListener getProgressChangeListener() {
                        return iProgressChangeListener;
                    }
                });
                intent.putExtra(UploadResultReceiver.EXTRA_FILES_KEY, uploadables);
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
