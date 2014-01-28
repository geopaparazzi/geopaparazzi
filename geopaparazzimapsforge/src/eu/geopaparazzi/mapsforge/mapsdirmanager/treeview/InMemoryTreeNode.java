package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import eu.geopaparazzi.spatialite.database.spatial.core.SpatialTable;

/**
 * Node. It is package protected so that it cannot be used outside.
 *
 * @param <T>
 *            type of the identifier used by the tree
 */
@SuppressWarnings("nls")
class InMemoryTreeNode<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final T id;
    private final T parent;
    // private final T id_classinfo;
    private final int level;
    private boolean visible = true;
    private final List<InMemoryTreeNode<T>> children = new LinkedList<InMemoryTreeNode<T>>();
    private List<T> childIdListCache = null;
    // note: adding TreeNode caused Serialization problems
    private final String filePath;
    private final String typeText;
    private final int type;
    private final String bounds;
    private final String center;
    private final String zoom_levels;
    private SpatialTable spatialTable;

    public InMemoryTreeNode( final T id, final T parent, final int level, final boolean visible, TreeNode< ? > treeNode ) {
        super();
        this.id = id;
        this.parent = parent;
        this.level = level;
        this.visible = visible;
        if (treeNode != null) {
            // note: the getId() is lost. [why can't this be stored?]
            // this.id_classinfo=this_classinfo.getId();
            this.filePath = treeNode.getFilePath();
            this.typeText = treeNode.getTypeText();
            this.type = treeNode.getType();
            this.bounds = treeNode.getBounds();
            this.center = treeNode.getCenter();
            this.zoom_levels = treeNode.getZoomLevels();

            spatialTable = treeNode.getSpatialTable();
        } else {
            // this.id_classinfo=0;
            this.filePath = "";
            this.typeText = "this_classinfo == null";
            this.type = -1;
            this.bounds = "";
            this.center = "";
            this.zoom_levels = "";
        }
    }

    public int indexOf( final T id ) {
        return getChildIdList().indexOf(id);
    }

    /**
     * Cache is built lasily only if needed. The cache is cleaned on any
     * structure change for that node!).
     *
     * @return list of ids of children
     */
    public synchronized List<T> getChildIdList() {
        if (childIdListCache == null) {
            childIdListCache = new LinkedList<T>();
            for( final InMemoryTreeNode<T> n : children ) {
                childIdListCache.add(n.getId());
            }
        }
        return childIdListCache;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible( final boolean visible ) {
        this.visible = visible;
    }

    public int getChildrenListSize() {
        return children.size();
    }

    public synchronized InMemoryTreeNode<T> add( final int index, final T child, final boolean visible, TreeNode< ? > treeNode ) {
        childIdListCache = null;
        // Note! top levell children are always visible (!)
        final InMemoryTreeNode<T> newNode = new InMemoryTreeNode<T>(child, getId(), getLevel() + 1, getId() == null
                ? true
                : visible, treeNode);
        children.add(index, newNode);
        return newNode;
    }

    /**
     * Note. This method should technically return unmodifiable collection, but
     * for performance reason on small devices we do not do it.
     *
     * @return children list
     */
    public List<InMemoryTreeNode<T>> getChildren() {
        return children;
    }

    public synchronized void clearChildren() {
        children.clear();
        childIdListCache = null;
    }

    public synchronized void removeChild( final T child ) {
        final int childIndex = indexOf(child);
        if (childIndex != -1) {
            children.remove(childIndex);
            childIdListCache = null;
        }
    }

    @Override
    public String toString() {
        return "InMemoryTreeNode [id=" + getId() + ", parent=" + getParent() + ", level=" + getLevel() + ", visible=" + visible
                + ", file_path[" + getFilePath() + "], typeText[" + getTypeText() + "],  type[" + getType() + "], bounds["
                + getBounds() + "]  center[" + getCenter() + "], zoom_levels[" + getZoomLevels() + "]";
    }

    T getId() {
        return id;
    }

    T getParent() {
        return parent;
    }

    int getLevel() {
        return level;
    }

    public TreeNode<T> getTreeNode() {
        if (spatialTable == null) {
            return new TreeNode<T>(getId(), null, getFilePath());
        } else {
            return new TreeNode<T>(getId(), spatialTable, null);
        }
    }

    public String getFilePath() {
        return filePath;
    }
    public String getBounds() {
        return bounds;
    }
    public String getCenter() {
        return center;
    }
    public String getZoomLevels() {
        return zoom_levels;
    }
    public String getTypeText() {
        return typeText;
    }
    public int getType() {
        return type;
    }
}
