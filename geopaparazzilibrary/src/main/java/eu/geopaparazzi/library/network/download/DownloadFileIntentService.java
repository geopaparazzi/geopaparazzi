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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;

import eu.geopaparazzi.library.core.dialogs.ProgressBarDialogFragment;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.util.TimeUtilities;

import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_ENDED_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_ERRORED_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.PROGRESS_MESSAGE_KEY;
import static eu.geopaparazzi.library.network.download.DownloadResultReceiver.max;

/**
 * A service to download files.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DownloadFileIntentService extends IntentService {
    public static final int PERCENTAGE_UNIT = 5;
    ResultReceiver resultReceiver;
    private Bundle updateBundle;

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

                            sendGetRequest4File(url, destFile, downloadable.getSize(), null, user, password);
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

    /**
     * Sends an HTTP GET request to a url
     *
     * @param urlStr            - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param file              the output file. If it is a folder, it tries to get the file name from the header.
     * @param size
     * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2").
     *                          Note: This method will add the question mark (?) to the request -
     *                          DO NOT add it yourself
     * @param user              user.
     * @param password          password.    @return the file written.
     * @throws Exception if something goes wrong.
     */
    public void sendGetRequest4File(String urlStr, File file, long size, String requestParameters, String user, String password)
            throws Exception {
        if (requestParameters != null && requestParameters.length() > 0) {
            urlStr += "?" + requestParameters;
        }
        HttpURLConnection conn = NetworkUtilities.makeNewConnection(urlStr);
        conn.setRequestMethod("GET");
        // conn.setDoOutput(true);
        conn.setDoInput(true);
        // conn.setChunkedStreamingMode(0);
        conn.setUseCaches(false);

        if (user != null && password != null && user.trim().length() > 0 && password.trim().length() > 0) {
            conn.setRequestProperty("Authorization", NetworkUtilities.getB64Auth(user, password));
        }
        conn.connect();

        if (file.isDirectory()) {
            // try to get the header
            String headerField = conn.getHeaderField("Content-Disposition");
            String fileName = null;
            if (headerField != null) {
                String[] split = headerField.split(";");
                for (String string : split) {
                    String pattern = "filename=";
                    if (string.toLowerCase().startsWith(pattern)) {
                        fileName = string.replaceFirst(pattern, "");
                        break;
                    }
                }
            }
            if (fileName == null) {
                // give a name
                fileName = "FILE_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date());
            }
            file = new File(file, fileName);
        }

        String msg = "Downloading: " + file.getName();

        if (isCancelled) return;
        InputStream in = null;
        FileOutputStream out = null;
        BufferedInputStream bis = null;
        boolean deleteFile = false;
        try {
            in = conn.getInputStream();
            bis = new BufferedInputStream(in);

            if (size == -1)
                size = conn.getContentLength();

            out = new FileOutputStream(file);

            byte[] buffer = new byte[8192];
            long total = 0;
            int count;

            int prevPercentage = 0;
            while ((count = bis.read(buffer)) != -1) {
                total += count;

                out.write(buffer, 0, count);

                if (isCancelled) {
                    deleteFile = true;
                    return;
                }

                int percentage = (int) ((total * 100) / size);
                prevPercentage = handlePercentage(percentage, prevPercentage, msg);
            }
            out.flush();
        } finally {
            if (bis != null)
                bis.close();
            if (in != null)
                in.close();
            if (out != null) {
                out.close();
            }
            if (deleteFile) {
                file.delete();
            }
            conn.disconnect();
        }
    }

    private int handlePercentage(int percentage, int prevPercentage, String msg) {
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
            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
        } else if (percentage - prevPercentage > PERCENTAGE_UNIT) {
            if (msg != null)
                updateBundle.putString(PROGRESS_MESSAGE_KEY, msg);
            updateBundle.putInt(PROGRESS_KEY, percentage);
            resultReceiver.send(DownloadResultReceiver.RESULT_CODE, updateBundle);
            return percentage;
        }
        return prevPercentage;
    }
}