package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class BlobResource extends Resource {
    private byte[] blob = null;

    public BlobResource(byte[] blob, String name, ResourceType type) {
        super(-1, name, type);
        this.blob = blob;
    }

    public BlobResource(long id, byte[] data, String name, ResourceType type) {
        super(id, name, type);
        this.blob = data;
    }

    public BlobResource(long id, byte[] data, String name, ResourceType type, String mimeType) {
        super(id, name, type, mimeType);
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
}
