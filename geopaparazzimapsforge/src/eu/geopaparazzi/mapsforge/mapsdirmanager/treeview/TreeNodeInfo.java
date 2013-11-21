package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;
/**
 * Information about the node.
 *
 * @param <T>
 *            type of the id for the tree
 */
public class TreeNodeInfo<T> {
    private final T id;
    private final int level;
    private final boolean withChildren;
    private final boolean visible;
    private final boolean expanded;
    private final ClassNodeInfo this_classinfo;

    /**
     * Creates the node information.
     *
     * @param id
     *            id of the node
     * @param level
     *            level of the node
     * @param withChildren
     *            whether the node has children.
     * @param visible
     *            whether the tree node is visible.
     * @param expanded
     *            whether the tree node is expanded
     * @param s_short_text
     *            short text to be shown
     * @param s_long_text
     *            long text to be retrieved
     * @param s_type
     *            type to be retrieved
     *
     */
    public TreeNodeInfo(final T id, final int level,
            final boolean withChildren, final boolean visible,
            final boolean expanded,
            final ClassNodeInfo this_classinfo) {
        super();
        this.id = id;
        this.level = level;
        this.withChildren = withChildren;
        this.visible = visible;
        this.expanded = expanded;
        this.this_classinfo=this_classinfo;
    }

    public T getId() {
        return id;
    }

    public boolean isWithChildren() {
        return withChildren;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isExpanded() {
        return expanded;
    }
    public int getLevel() {
        return level;
    }

    public ClassNodeInfo getClassNodeInfo() {
        return this_classinfo;
    }
    public String getClassName() {
        return this_classinfo.getClassName();
    }
    public String getShortText() {
        return this_classinfo.getShortText();
    }
    public String getLongText() {
        return this_classinfo.getLongText();
    }
    public String getShortDescription() {
        return this_classinfo.getShortDescription();
    }
    public String getLongDescription() {
        return this_classinfo.getLongDescription();
    }
    public String getTypeText() {
        return this_classinfo.getTypeText();
    }
    public int getType() {
        return this_classinfo.getType();
    }
    @Override
    public String toString() {
        return "TreeNodeInfo [id=" + id + ", level=" + level
                + ", withChildren=" + withChildren + ", visible=" + visible + ", expanded=" + expanded
                + ", ClassNodeInfo=" + getClassNodeInfo().toString()
                + "]";
    }

}
