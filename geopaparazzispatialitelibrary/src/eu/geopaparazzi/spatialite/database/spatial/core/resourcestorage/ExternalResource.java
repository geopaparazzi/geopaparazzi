package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ExternalResource extends AbstractResource {
    private String path;

    public enum TYPES {
        EXTERNAL_IMAGE,
        EXTERNAL_VIDEO,
        EXTERNAL_PDF,
        EXTERNAL_FILE
    }

    public ExternalResource(long id, String path, String name) {
        super(id, name);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
