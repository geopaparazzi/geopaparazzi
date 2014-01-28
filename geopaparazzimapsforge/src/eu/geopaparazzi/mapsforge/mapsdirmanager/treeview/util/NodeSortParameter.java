package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.util;

import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeNode;

/**
 * {@link TreeNode} sort parameters.
 */
public enum NodeSortParameter {
    /**
     * Sort by the text of the node type.
     */
    SORT_TYPE_TEXT,
    /**
     * Sort by folder.
     */
    SORT_DIRECTORY,
    /**
     * Sort by file name.
     */
    SORT_FILE_NAME,
    /**
     * Sort by file path.
     */
    SORT_FILE_PATH,
    /**
     * Sort by enablement.
     */
    SORT_ENABLED;

}
