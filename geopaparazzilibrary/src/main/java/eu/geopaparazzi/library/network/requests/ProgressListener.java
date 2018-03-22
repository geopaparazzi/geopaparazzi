package eu.geopaparazzi.library.network.requests;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */

public interface ProgressListener {

    /**
     * Report a progress update
     *
     * @param currentReadSize The number of bytes that have been read since the beginning of the
     *                        task
     */
    public void handleSizeProgress(long currentReadSize);

    /**
     * Report a progress update
     *
     * @param percentage The percentage of the file that has been transferred
     */
    public void handlePercentageProgress(int percentage);

    /**
     * Returns true if the task has been cancelled
     * @return
     */
    public boolean isCancelled();
}
