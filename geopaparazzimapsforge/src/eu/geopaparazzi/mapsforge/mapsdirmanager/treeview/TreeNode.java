package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
import java.io.File;
import java.util.Comparator;

import com.vividsolutions.jts.geom.Envelope;

import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.util.NodeSortParameter;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialTable;
import eu.geopaparazzi.spatialite.util.SpatialDataType;
/**
 * The tree node object.
 *
 * @param <T> type of the id for the tree
 */
@SuppressWarnings("nls")
public class TreeNode<T> {
    /**
     * 
     */
    public static final String DIRECTORY = "directory";

    private final T id;
    private final int type;
    private int level = 0;
    private boolean isEnabled = false;
    private SpatialTable spatialTable;
    private String folder;

    /**
     * Creates the class node information.
     *
     * @param id id of the node
     * @param spatialTable the wrapped {@link SpatialTable}. If <code>null</code>, folder needs to be set.
     * @param folder the folder it represents in case it doesn't wrap a spatial table. 
     *
     */
    public TreeNode( final T id, SpatialTable spatialTable, String folder ) {
        this.spatialTable = spatialTable;
        this.folder = folder;
        if (spatialTable == null && folder == null) {
            throw new RuntimeException("Either table or folder need to be set.");
        }
        this.id = id;

        if (folder != null) {
            type = 100;
        } else {
            String mapType = spatialTable.getMapType();
            type = SpatialDataType.getCode4Name(mapType);
        }
    }

    /**
      * Set Position Values
      *
      * <p>strict checking is done, since anything could be sent
      * <p>intended for use with SpatialVectorTable
      *
      * @param bounds_zoom 5 values: west, south, east, north wsg84 values and zoom-level
      * @param checkEnabledNodes
      *           <ol>
      *                <li>0 = return all  ;</li>
      *                <li>1 = return only those that are enabled  ;</li>
      *           </ol>
      * @return i_rc
      *           <ol>
      *                <li>0 = conditions not fulfilled ;</li>
      *                <li>1 = completely inside valid bounds ;</li>
      *                <li>2 = partially inside valid bounds ; </li>
      *                <li>-1= zoom invalid ; </li>
      *                <li>-2= bounds invalid </li>
      *           </ol>
      */
    public int checkPositionValues( double[] bounds_zoom, int checkEnabledNodes ) {
        int i_rc = 0;
        if (checkEnabledNodes == 1 && !isEnabled()) {
            /*
             * this is not enabled, so return now
             * [checking is desired]
             */
            return i_rc;
        }
        if ((bounds_zoom == null) || (bounds_zoom.length != 5)) {
            // no checking of any kind are done
            i_rc = 10;
            return i_rc;
        }
        if ((bounds_zoom.length == 5) || (bounds_zoom.length == 7)) {
            /*
             * we must have 5 values:
             * west,south,east,north wsg84 values and zoom-level
             */
            int i_zoom = (int) bounds_zoom[4];
            if ((i_zoom >= spatialTable.getMinZoom()) && (i_zoom <= spatialTable.getMaxZoom())) {
                // inside valid zoom-levels
                Envelope envelope = new Envelope(bounds_zoom[0], bounds_zoom[2], bounds_zoom[1], bounds_zoom[3]);
                Envelope boundsEnvelope = new Envelope(spatialTable.getMinLongitude(), spatialTable.getMaxLongitude(),
                        spatialTable.getMinLatitude(), spatialTable.getMaxLatitude());

                if (boundsEnvelope.contains(envelope)) {
                    // completely inside valid bounds
                    i_rc = 1;
                } else {
                    if (boundsEnvelope.intersects(envelope)) {
                        // partially inside valid bounds
                        i_rc = 2;
                    } else {
                        i_rc = -2;
                    }
                }
            } else {
                i_rc = -1;
            }
        }
        return i_rc;
    }

    /**
     * @return the node id.
     */
    public T getId() {
        return id;
    }

    /**
     * @return get the {@link SpatialTable} that is backed by the node.
     */
    public SpatialTable getSpatialTable() {
        return spatialTable;
    }

    /**
     * @return the full path that backs this node.
     */
    public String getFilePath() {
        if (folder != null) {
            return folder;
        }
        return spatialTable.getDatabasePath();
    }

    /**
     * @return the file that backs this node.
     */
    public File getFile() {
        if (folder != null) {
            return new File(folder);
        }
        return spatialTable.getDatabaseFile();
    }

    /**
    * @return the name of the file that backs the node.
    */
    public String getFileName() {
        if (folder != null) {
            return new File(folder).getName();
        }
        return spatialTable.getFileName();
    }

    /**
     * @return the node type description.
     */
    public String getTypeText() {
        if (folder != null) {
            return DIRECTORY;
        }
        return spatialTable.getMapType();
    }

    /**
     * @return the bounds as string.
     */
    public String getBounds() {
        if (folder != null) {
            return "";
        }
        return spatialTable.getBoundsAsString();
    }
    /**
     * @return the center as string.
     */
    public String getCenter() {
        if (folder != null) {
            return "";
        }
        return spatialTable.getCenterAsString();
    }

    /**
     * @return the min max zoom levels as string. 
     */
    public String getZoomLevels() {
        if (folder != null) {
            return "";
        }
        return spatialTable.getMinMaxZoomLevelsAsString();
    }

    /**
     * @return the map data type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return <code>true</code> if the node is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @param isEnabled the enablement state to set.
     */
    public void setEnabled( boolean isEnabled ) {
        this.isEnabled = isEnabled;
    }

    /**
     * @return the node level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level the node level to set.
     */
    public void setLevel( int level ) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "TreeNode [id=" + id + ", type=" + type + ", level=" + level + ", enabled=" + isEnabled + ", FilePath["
                + getFilePath() + "]]";
    }

    /**
     * Create a treenode comparator.
     * 
     * @param sortParameters the {@link NodeSortParameter}s to check.
     * @return the comparator.
     */
    public static Comparator<TreeNode< ? >> getComparator( NodeSortParameter... sortParameters ) {
        return new TreeNodeComparator(sortParameters);
    }
}
