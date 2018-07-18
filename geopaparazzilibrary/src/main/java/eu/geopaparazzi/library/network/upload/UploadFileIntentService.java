package eu.geopaparazzi.library.network.upload;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;

import eu.geopaparazzi.library.core.dialogs.ProgressBarUploadDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;

import static eu.geopaparazzi.library.network.NetworkUtilities.maxBufferSize;
import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.PROGRESS_ENDED_KEY;
import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.PROGRESS_ERRORED_KEY;
import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.PROGRESS_KEY;
import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.PROGRESS_MESSAGE_KEY;
import static eu.geopaparazzi.library.network.upload.UploadResultReceiver.max;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;

/**
 * A service to download files.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class UploadFileIntentService extends IntentService {
    public static final int PERCENTAGE_UNIT = 5;
    ResultReceiver resultReceiver;
    private Bundle updateBundle;

    /**
     * An ugly method to stop the service. To be changed.
     */
    public static volatile boolean isCancelled = false;

    public static void startService(Activity activity, Parcelable[] uploadables, final ProgressBar progressBar, final TextView progressView, final ProgressBarUploadDialogFragment.IProgressChangeListener iProgressChangeListener) {
        Intent intent = new Intent(activity, UploadFileIntentService.class);
        intent.setAction(UploadResultReceiver.UPLOAD_ACTION);
        intent.putExtra(UploadResultReceiver.EXTRA_KEY, new UploadResultReceiver(new Handler()) {
            @Override
            public ProgressBar getProgressBar() {
                return progressBar;
            }

            @Override
            public TextView getProgressView() {
                return progressView;
            }

            @Override
            public ProgressBarUploadDialogFragment.IProgressChangeListener getProgressChangeListener() {
                return iProgressChangeListener;
            }
        });
        intent.putExtra(UploadResultReceiver.EXTRA_FILES_KEY, uploadables);
        activity.startService(intent);
    }

    public UploadFileIntentService() {
        super("" +
                "UploadFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        resultReceiver = intent.getParcelableExtra(UploadResultReceiver.EXTRA_KEY);
        Parcelable[] uploadItems = intent.getParcelableArrayExtra(UploadResultReceiver.EXTRA_FILES_KEY);

        Bundle extras = intent.getExtras();
        String user = extras.getString(PREFS_KEY_USER);
        String password = extras.getString(PREFS_KEY_PWD);

        String charset = "UTF-8";

        try {
            String action = intent.getAction();
            if (action != null && action.equals(UploadResultReceiver.UPLOAD_ACTION)) {

                updateBundle = new Bundle();
                for (Parcelable p : uploadItems) {
                    if (p instanceof IUploadable) {
                        IUploadable uploadable = (IUploadable) p;
                        if (isCancelled) {
                            sendError("Upload cancelled by user.");
                            return;
                        }

                        File sourceFile = new File(uploadable.getDestinationPath());
                        if (!sourceFile.exists()) {
                            updateBundle.putString(PROGRESS_MESSAGE_KEY, "Cannot find: " + sourceFile.getName());
                            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                            continue;
                        }

                        String url = uploadable.getUploadUrl();
                        updateBundle.putString(PROGRESS_MESSAGE_KEY, "Uploading: " + sourceFile.getName());
                        updateBundle.putInt(PROGRESS_KEY, 0);
                        resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                        try {
                            MultipartUtility multipart = new MultipartUtility(url, charset, user, password);
                            multipart.addFilePart("document", sourceFile);  // fieldName value is what the server is expecting
                            String response = multipart.finish();
                            updateBundle.putInt(PROGRESS_KEY, max);
                            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                        } catch (Exception e) {
                            sendError("An error occurred: " + e.getLocalizedMessage());
                            return;
                        }
                    }
                }
            }
            sendDone();
        } finally {
            isCancelled = false;
        }
    }

    private void pause() {
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void sendError(String msg) {
        updateBundle.putString(PROGRESS_ERRORED_KEY, msg);
        resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
    }

    private void sendDone() {
        updateBundle.putString(PROGRESS_ENDED_KEY, "Data properly uploaded.");
        resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
    }

    public int handlePercentage(int percentage, int prevPercentage, String msg) {
        if (percentage < PERCENTAGE_UNIT) {
            int p = 0;
            if (percentage < 1) p = 1;
            else if (percentage < 2) p = 2;
            else if (percentage < 3) p = 3;
            else if (percentage < 4) p = 4;
            else p = PERCENTAGE_UNIT;
            if (msg != null)
                updateBundle.putString(PROGRESS_MESSAGE_KEY, msg);
            updateBundle.putInt(PROGRESS_KEY, p);
            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
        } else if (percentage - prevPercentage > PERCENTAGE_UNIT) {
            if (msg != null)
                updateBundle.putString(PROGRESS_MESSAGE_KEY, msg);
            updateBundle.putInt(PROGRESS_KEY, percentage);
            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
            return percentage;
        }
        return prevPercentage;
    }
}