package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class BlobResource extends AbstractResource {
    private byte[] blob = null;
    private byte[] thumbnail = null;

    public BlobResource(byte[] blob, String name, ResourceType type) {
        super(-1, name, type);
        this.blob = blob;
    }

    public BlobResource(long id, byte[] data, String name, ResourceType type) {
        super(id, name, type);
        this.blob = data;
    }

    /**
     * Gets the resource data (image, video, pdf, etc) as a byte array
     *
     * @return
     */
    public byte[] getBlob() {
        return blob;
    }

    /**
     * Sets the resource data (image, video, pdf, etc) as a byte array
     * @param blob
     */
    public void setBlob(byte[] blob) {
        this.blob = blob;
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
