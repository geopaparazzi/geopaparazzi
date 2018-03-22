package eu.geopaparazzi.library.network.requests;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public class ResponsePromise {
    public static final long maxBufferSize = 8192;

    private HttpURLConnection connection;
    public ResponsePromise(HttpURLConnection conn) {
        connection = conn;
    }

    public String asString() {
        return "";
    }

    public void asFile(File outFile) throws IOException {
        asFile(outFile, null);
    }

    public void asFile(File outFile, ProgressListener listener) throws IOException {
        connection.setDoInput(true);
        connection.setUseCaches(false);
        if (listener.isCancelled()) return;
        InputStream in = null;
        FileOutputStream out = null;
        BufferedInputStream bis = null;
        boolean deleteFile = false;
        try {
            in = connection.getInputStream();
            bis = new BufferedInputStream(in);

            int size = connection.getContentLength();

            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[8192];
            long total = 0;
            int count;

            int prevPercentage = 0;
            while ((count = bis.read(buffer)) != -1) {
                total += count;

                out.write(buffer, 0, count);

                if (listener.isCancelled()) {
                    deleteFile = true;
                    return;
                }

                if (size!=-1) {
                    int percentage = (int) ((total * 100) / size);
                    listener.handlePercentageProgress(percentage);
                }
                else {
                    listener.handleSizeProgress(total);
                }
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
                outFile.delete();
            }
            connection.disconnect();
        }
    }
}
