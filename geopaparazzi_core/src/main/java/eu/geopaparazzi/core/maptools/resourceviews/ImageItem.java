package eu.geopaparazzi.core.maptools.resourceviews;

import android.graphics.Bitmap;

import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.Resource;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ImageItem {
    private Bitmap image;
    private String title;
    private Resource resource;

    public ImageItem(Bitmap image, String title, Resource res) {
        super();
        this.image = image;
        this.title = title;
        this.setResource(res);
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
