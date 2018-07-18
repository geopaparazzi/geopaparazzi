package eu.geopaparazzi.library.network.upload;

/**
 * Created by GeoAnaltyic on 11/06/18.
 */

public interface IUploadable {
    long getSize();

    String getUploadUrl();

    String getDestinationPath();

    void setDestinationPath(String path);
}
