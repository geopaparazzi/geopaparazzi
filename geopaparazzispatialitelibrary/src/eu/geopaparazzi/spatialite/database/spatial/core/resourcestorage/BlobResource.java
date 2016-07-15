package eu.geopaparazzi.spatialite.database.spatial.core.resourcestorage;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class BlobResource extends AbstractResource {
    private Byte[] blob;

    public BlobResource(long id, Byte[] blob, String name) {
        super(id, name);
        this.blob = blob;
    }

    public Byte[] getBlob() {
        return blob;
    }

    public void setBlob(Byte[] blob) {
        this.blob = blob;
    }
}
