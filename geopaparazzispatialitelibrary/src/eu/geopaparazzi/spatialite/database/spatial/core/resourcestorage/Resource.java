package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class Resource {
    private long id;
    private String name;
    private ResourceType type;
    private String mimeType = "";
    private Boolean isExternal;
    private byte[] thumbnail = null;

    public enum ResourceType {
        EXTERNAL_IMAGE,
        EXTERNAL_VIDEO,
        EXTERNAL_PDF,
        /** Generic external resource; use mime type to decide the resource type */
        EXTERNAL_FILE,
        BLOB_IMAGE,
        BLOB_VIDEO,
        BLOB_PDF,
        /** Generic internal resource; use mime type to decide the resource type */
        BLOB_FILE,
        /**
         * Used when only the resource thumbnail has been loaded, so we still
         * don't know whether it is an internal or external resource
         */
        THUMBNAIL
    }

    public Resource(long id, String name, ResourceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        if (this.type.ordinal() < ResourceType.BLOB_IMAGE.ordinal() ) {
            this.isExternal = true;
        } else {
            this.isExternal = false;
        }
    }

    public Resource(long id, String name, ResourceType type, String mimeType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.mimeType = mimeType;
        if (this.type.ordinal() < ResourceType.BLOB_IMAGE.ordinal() ) {
            this.isExternal = true;
        } else {
            this.isExternal = false;
        }
    }

    public Boolean isExternal() { return this.isExternal; }

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
        if (this.type.ordinal() < ResourceType.BLOB_IMAGE.ordinal() ) {
            this.isExternal = true;
        } else {
            this.isExternal = false;
        }
    }

    /**
     * Gets the type of resource
     * @return
     */
    public ResourceType getType() {
        return this.type;
    }

    /**
     * Gets the mime type of the resource
     * @return
     */
    public String getMimeType() {
        if (this.mimeType.equals("")) {
            if (this.type == ResourceType.EXTERNAL_IMAGE || this.type == ResourceType.BLOB_IMAGE) {
                return "image/*";
            }
            if (this.type == ResourceType.EXTERNAL_PDF || this.type == ResourceType.BLOB_PDF) {
                return "application/pdf";
            }
            if (this.type == ResourceType.EXTERNAL_PDF || this.type == ResourceType.BLOB_PDF) {
                return "video/*";
            }
        }
        return this.mimeType;
    }

    /**
     * Gets a thumbnail of the resource.
     *
     * @return A low resolution image representing the resource,
     * or null if no thumbnail is available
     */
    public byte[] getThumbnail() { return this.thumbnail; }

    /**
     * Sets the thumbnail of the resource
     *
     * @param thumbnail
     */
    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }
}
