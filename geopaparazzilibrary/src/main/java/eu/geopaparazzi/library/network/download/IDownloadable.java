package eu.geopaparazzi.library.network.download;

/**
 * Created by hydrologis on 20/03/18.
 */

public interface IDownloadable {
    long getSize();

    String getUrl();

    String getDestinationPath();

    void setDestinationPath(String path);
}
