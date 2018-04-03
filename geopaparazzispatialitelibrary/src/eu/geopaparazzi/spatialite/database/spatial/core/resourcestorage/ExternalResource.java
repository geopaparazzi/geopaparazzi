package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ExternalResource extends Resource {
    private String path;

    public ExternalResource(String path, String name, ResourceType type) {
        super(-1, name, type);
        this.path = path;
    }

    public ExternalResource(long id, String path, String name, ResourceType type) {
        super(id, name, type);
        this.path = path;
    }

    public ExternalResource(long id, String path, String name, ResourceType type, String mimeType) {
        super(id, name, type, mimeType);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
