package eu.geopaparazzi.map.gui;

import android.graphics.Bitmap;

import eu.geopaparazzi.map.features.editing.resourcestorage.Resource;


/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ResourceImageItem {
    private Bitmap image;
    private String title;
    private Resource resource;

    public ResourceImageItem(Bitmap image, String title, Resource res) {
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
