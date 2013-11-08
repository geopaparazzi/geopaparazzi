package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

/**
 * This exception is thrown when the tree does not contain node requested.
 *
 */
public class NodeNotInTreeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NodeNotInTreeException(final String id) {
        super("The tree does not contain the node specified: " + id);
    }

}
