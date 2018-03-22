package eu.geopaparazzi.library.network.download;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import eu.geopaparazzi.library.core.dialogs.ProgressBarDialogFragment;
import eu.geopaparazzi.library.network.requests.ProgressListener;
import eu.geopaparazzi.library.network.requests.Requests;
import eu.geopaparazzi.library.network.requests.ResponsePromise;

import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_ENDED_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_ERRORED_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_MESSAGE_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.max;

public class DownloadFileIntentService extends IntentService implements ProgressListener {
    public static final int PERCENTAGE_UNIT = 5;
    ResultReceiver resultReceiver;
    private Bundle updateBundle;
    private int previousPercentage;
    private long totalSize;
    private String message;

    /**
     * An ugly method to stop the service. To be changed.
     */
    public static volatile boolean isCancelled = false;

    public static void startService(Activity activity, Parcelable[] downloadables, final ProgressBar progressBar, final TextView progressView, final ProgressBarDialogFragment.IProgressChangeListener iProgressChangeListener) {
        Intent intent = new Intent(activity, DownloadFileIntentService.class);
        intent.setAction(DownloadResultReceiver.DOWNLOAD_ACTION);
        intent.putExtra(DownloadResultReceiver.EXTRA_KEY, new DownloadResultReceiver(new Handler()) {
            @Override
            public ProgressBar getProgressBar() {
                return progressBar;
            }

            @Override
            public TextView getProgressView() {
                return progressView;
            }

            @Override
            public ProgressBarDialogFragment.IProgressChangeListener getProgressChangeListener() {
                return iProgressChangeListener;
            }
        });
        intent.putExtra(DownloadResultReceiver.EXTRA_FILES_KEY, downloadables);
        activity.startService(intent);
    }


    public DownloadFileIntentService() {
        super("DownloadFileIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        resultReceiver = intent.getParcelableExtra(DownloadResultReceiver.EXTRA_KEY);


        Parcelable[] downloadItems = intent.getParcelableArrayExtra(DownloadResultReceiver.EXTRA_FILES_KEY);

        String user = null;//intent.getStringExtra("user");
        String password = null;//intent.getStringExtra("pwd");
        try {

            String action = intent.getAction();
            if (action != null && action.equals(DownloadResultReceiver.DOWNLOAD_ACTION)) {

                updateBundle = new Bundle();
                for (Parcelable p : downloadItems) {
                    if (p instanceof IDownloadable) {
                        IDownloadable downloadable = (IDownloadable) p;
                        if (isCancelled) {
                            sendError("Download cancelled by user.");
                            return;
                        }

                        File destFile = new File(downloadable.getDestinationPath());
                        if (destFile.exists()) {
                            updateBundle.putString(PROGRESS_MESSAGE_KEY, "Not downloading existing: " + destFile.getName());
                            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                            continue;
                        }

                        File parentFile = destFile.getParentFile();
                        if (!parentFile.exists() && !parentFile.mkdirs()) {
                            // can't write on disk
                            sendError("Unable to write to file: " + destFile.getAbsolutePath());
                            pause();
                            return;
                        }

                        String url = downloadable.getUrl();
                        updateBundle.putString(PROGRESS_MESSAGE_KEY, "Downloading: " + destFile.getName());
                        updateBundle.putInt(PROGRESS_KEY, 0);
                        resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
                        try {
                            previousPercentage = 0;
                            totalSize = downloadable.getSize();
                            message = "Downloading: " + destFile.getName();
                            ResponsePromise rp = Requests.get(url, null, null);
                            rp.asFile(destFile, this);
                            updateBundle.putInt(PROGRESS_KEY, max);
                            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                        } catch (Exception e) {
                            sendError("An error occurred: " + e.getLocalizedMessage());
//                        e.printStackTrace();
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
        resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
    }

    private void sendDone() {
        updateBundle.putString(PROGRESS_ENDED_KEY, "Data properly downloaded.");
        resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
    }

    @Override
    public void handleSizeProgress(long currentReadSize) {
        if (totalSize>0) {
            int percentage = (int) ((currentReadSize * 100) / totalSize);
            handlePercentageProgress(percentage);
        }
        else {
            //FIXME: we should use an undeterminated progress bar
            updateBundle.putString(DownloadResultReceiver.PROGRESS_MESSAGE_KEY, message + ": " + currentReadSize);
            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
        }
    }

    @Override
    public void handlePercentageProgress(int percentage) {
        if (percentage < PERCENTAGE_UNIT) {
            int p = 0;
            if (percentage < 1) p = 1;
            else if (percentage < 2) p = 2;
            else if (percentage < 3) p = 3;
            else if (percentage < 4) p = 4;
            else p = PERCENTAGE_UNIT;
            if (message != null)
                updateBundle.putString(PROGRESS_MESSAGE_KEY, message);
            updateBundle.putInt(PROGRESS_KEY, p);
            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
        } else if (percentage - previousPercentage > PERCENTAGE_UNIT) {
            if (message != null)
                updateBundle.putString(PROGRESS_MESSAGE_KEY, message);
            updateBundle.putInt(PROGRESS_KEY, percentage);
            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
            previousPercentage = percentage;
        }
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

}