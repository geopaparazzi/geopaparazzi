package eu.geopaparazzi.core.maptools.resourceviews;

import android.graphics.Bitmap;

import eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage.AbstractResource;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ImageItem {
    private Bitmap image;
    private String title;
    private AbstractResource resource;

    public ImageItem(Bitmap image, String title, AbstractResource res) {
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

    public AbstractResource getResource() {
        return resource;
    }

    public void setResource(AbstractResource resource) {
        this.resource = resource;
    }
}
