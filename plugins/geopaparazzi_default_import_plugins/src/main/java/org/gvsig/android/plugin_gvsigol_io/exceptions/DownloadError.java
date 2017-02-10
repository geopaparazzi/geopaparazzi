package org.gvsig.android.plugin_gvsigol_io.exceptions;

/**
 * Created by cesar on 7/02/17.
 */

public class DownloadError extends Exception {
    public DownloadError(String message) {
        super(message);
    }

    public DownloadError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadError(Throwable cause) {
        super(cause);
    }
}
