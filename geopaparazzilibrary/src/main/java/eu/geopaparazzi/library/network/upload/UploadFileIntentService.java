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

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import eu.geopaparazzi.library.R;
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

        try {
            String action = intent.getAction();
            if (action != null && action.equals(UploadResultReceiver.UPLOAD_ACTION)) {

                updateBundle = new Bundle();
                for (Parcelable p : uploadItems) {
                    if (p instanceof IUploadable) {
                        IUploadable uploadable = (IUploadable) p;
                        if (isCancelled) {
                            sendError(getString(R.string.upload_Canceled));
                            return;
                        }

                        File sourceFile = new File(uploadable.getDestinationPath());
                        if (!sourceFile.exists()) {
                            updateBundle.putString(PROGRESS_MESSAGE_KEY, getString(R.string.upload_NotFound) + sourceFile.getName());
                            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                            continue;
                        }

                        String url = uploadable.getUploadUrl();
                        updateBundle.putString(PROGRESS_MESSAGE_KEY, getString(R.string.upload_Uploading) + sourceFile.getName());
                        updateBundle.putInt(PROGRESS_KEY, 0);
                        resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                        try {
                            String response = sendFileViaPost(url, sourceFile, "document",user, password);
                            updateBundle.putInt(PROGRESS_KEY, max);
                            resultReceiver.send(UploadResultReceiver.RESULT_CODE, updateBundle);
                            pause();
                        } catch (Exception e) {
                            sendError(getString(R.string.upload_Error) + e.getLocalizedMessage());
                            return;
                        } finally {
                            if (GPLog.LOG)
                                GPLog.addLogEntry("UploadFile", getString(R.string.upload_Uploaded) + sourceFile.getName());
                        }
                    }
                }
            }
            sendDone();
        } finally {
            isCancelled = false;
        }
    }

    /**
     * This utility method provides an abstraction layer for sending multipart HTTP
     * POST requests to a web server.
     * (even though we use it to send one file at a time)
     * ---- Adapted from: ----
     * @author www.codejava.net
     * https://gist.github.com/Antarix/a36faeaff3092b1fd977
     *  (License MIT, attribution required)
     */
    public String sendFileViaPost(String requestURL, File uploadFile, String fieldName, String user, String password)
            throws Exception {


        // ====== Multipart Utility "New" ======
        String charset = "UTF-8";
        String boundary;
        String LINE_FEED = "\r\n";
        HttpURLConnection httpConn;
        OutputStream outputStream;
        PrintWriter writer;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        String fileName = uploadFile.getName();

        String header = "";
        header += "--" + boundary + LINE_FEED;
        header += "Content-Disposition: form-data; name=\"" + fieldName
                  + "\"; filename=\"" + fileName + "\"" + LINE_FEED;
        header += "Content-Type: "
                  + URLConnection.guessContentTypeFromName(fileName) + LINE_FEED;
        header += "Content-Transfer-Encoding: binary" + LINE_FEED;
        header += LINE_FEED;

        String footer = "";
        footer += LINE_FEED;
//        footer += LINE_FEED;
        footer += "--" + boundary + "--";
        footer += LINE_FEED;

        long totalLength = header.length() + uploadFile.length() + footer.length();

        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);

        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (user != null && password != null && user.trim().length() > 0 && password.trim().length() > 0) {
            httpConn.setRequestProperty("Authorization", NetworkUtilities.getB64Auth(user, password));
        }

        // required for Progress bar updating:
        httpConn.setFixedLengthStreamingMode(totalLength);

        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),true);


        // ====== Multipart Utility "addFilePart()" ======
        FileInputStream inputStream = new FileInputStream(uploadFile);

        try {
            writer.append(header);
            writer.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = -1;

            long fileSize = uploadFile.length();
            long totalBytesWritten = 0;
            int percentage = 0;
            int prevPercentage = 0;
            String msg = getString(R.string.upload_Uploading)+ uploadFile.getName();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesWritten = totalBytesWritten + bytesRead;
                percentage = (int) ((totalBytesWritten * 100) / fileSize);
                prevPercentage = handlePercentage(percentage, prevPercentage, msg + " ("+ String.valueOf(percentage) + "%)");
                outputStream.flush();
                writer.flush();
            }
        } finally {
            if (inputStream != null)
                inputStream.close();
        }


        // ====== Multipart Utility "finish()" ======
        String response = "";
        writer.append(footer).flush();
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status >= HttpURLConnection.HTTP_OK  && status < 400 ) {

            BufferedInputStream in = new BufferedInputStream(httpConn.getInputStream());
            response = inputStreamToString(in);

            httpConn.disconnect();
        } else {
            throw new Exception(getString(R.string.upload_NonOK) + status);
        }

        return response;
    }

    private static String inputStreamToString(InputStream in) {
        String result = "";
        if (in == null) {
            return result;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            result = out.toString();
            reader.close();

            return result;
        } catch (Exception e) {
            // TODO: handle exception
//            Logcat.e("InputStream", "Error : " + e.toString());
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("UploadFile.InputStream", e.toString());
            return result;
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
        updateBundle.putString(PROGRESS_ENDED_KEY, getString(R.string.upload_Uploaded));
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