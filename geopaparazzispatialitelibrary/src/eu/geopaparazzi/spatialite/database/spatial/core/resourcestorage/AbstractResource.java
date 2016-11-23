package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class AbstractResource {
    private long id;
    private String name;
    private ResourceType type;

    public enum ResourceType {
        EXTERNAL_IMAGE,
        EXTERNAL_VIDEO,
        EXTERNAL_PDF,
        EXTERNAL_FILE,
        BLOB_IMAGE,
        BLOB_VIDEO,
        BLOB_PDF,
        BLOB_FILE
    }

    public AbstractResource(long id, String name, ResourceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the name of the resource (title or textual description)
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the resource (title or textual description)
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the identifier (primary key) of the resource in the resources
     * table
     *
     * @return
     */
    public long getId() {
        return this.id;
    }

    /**
     * Sets the type of resource
     *
     * @param type
     */
    public void setType(ResourceType type) {
        this.type = type;
    }

    /**
     * Gets the type of resource
     * @return
     */
    public ResourceType getType() {
        return this.type;
    }
}
