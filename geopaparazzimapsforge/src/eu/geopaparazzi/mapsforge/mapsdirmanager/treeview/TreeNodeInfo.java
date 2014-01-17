package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
/**
 * Information about the node.
 *
 * @param <T> type of the id for the tree
 */
@SuppressWarnings("nls")
public class TreeNodeInfo<T> {
    private final T id;
    private final int level;
    private final boolean withChildren;
    private final boolean visible;
    private final boolean expanded;
    private final TreeNode< ? > treeNode;

    /**
     * Creates the node information.
     *
     * @param id  id of the node.
     * @param level  level of the node.
     * @param withChildren  whether the node has children.
     * @param visible  whether the tree node is visible.
     * @param expanded   whether the tree node is expanded.
     * @param treeNode the node object.
     *
     */
    public TreeNodeInfo( final T id, final int level, final boolean withChildren, final boolean visible, final boolean expanded,
            final TreeNode< ? > treeNode ) {
        super();
        this.id = id;
        this.level = level;
        this.withChildren = withChildren;
        this.visible = visible;
        this.expanded = expanded;
        this.treeNode = treeNode;

        if (treeNode == null) {
            System.out.println();
        }
    }

    /**
     * @return the id.
     */
    public T getId() {
        return id;
    }

    /**
     * @return <code>true</code> if it has children.
     */
    public boolean isWithChildren() {
        return withChildren;
    }

    /**
     * @return <code>true</code> if it is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return <code>true</code> if it is expanded.
     */
    public boolean isExpanded() {
        return expanded;
    }
    /**
     * @return order level of the node.
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return the node object.
     */
    public TreeNode< ? > getClassNodeInfo() {
        return treeNode;
    }

    /**
     * @return the file name for the node.
     */
    public String getFileName() {
        if (treeNode == null) {
            return "";
        }
        return treeNode.getFileName();
    }

    /**
     * @return the file path for the node.
     */
    public String getFilePath() {
        if (treeNode == null) {
            return "";
        }
        return treeNode.getFilePath();
    }

    /**
     * @return a short name for the node.
     */
    public String getShortName() {
        if (treeNode == null) {
            return "";
        }
        return treeNode.getShortName();
    }

    /**
     * @return a long name fo rthe node.
     */
    public String getLongName() {
        if (treeNode == null) {
            return "";
        }
        return treeNode.getLongName();
    }

    /**
     * @return The type description.
     */
    public String getTypeDescriptionText() {
        if (treeNode == null) {
            return "";
        }
        return treeNode.getTypeText();
    }

    @Override
    public String toString() {
        return "TreeNodeInfo [id=" + id + ", level=" + level + ", withChildren=" + withChildren + ", visible=" + visible
                + ", expanded=" + expanded + ", TreeNode=" + getClassNodeInfo().toString() + "]";
    }

}
