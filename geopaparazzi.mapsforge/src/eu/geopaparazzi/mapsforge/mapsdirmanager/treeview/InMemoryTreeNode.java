package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;

/**
 * Node. It is package protected so that it cannot be used outside.
 *
 * @param <T>
 *            type of the identifier used by the tree
 */
class InMemoryTreeNode<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final T id;
    private final T parent;
    // private final T id_classinfo;
    private final int level;
    private boolean visible = true;
    private final List<InMemoryTreeNode<T>> children = new LinkedList<InMemoryTreeNode<T>>();
    private List<T> childIdListCache = null;
    // note: adding ClassNodeInfo caused Serialization problems
    private final String s_file_path;
    private final String s_type;
    private final int i_type;
    private final String s_short_text;
    private final String s_long_text;
    private final String s_short_description;
    private final String s_long_description;
    private final String s_class_name;
    private final String s_bounds;
    private final String s_center;
    private final String s_zoom_levels;

    public InMemoryTreeNode(final T id, final T parent, final int level,
            final boolean visible, ClassNodeInfo this_classinfo) {
        super();
        this.id = id;
        this.parent = parent;
        this.level = level;
        this.visible = visible;
        if (this_classinfo != null)
        { // note: the getId() is lost. [why can't this be stored?]
         // this.id_classinfo=this_classinfo.getId();
         this.s_file_path=this_classinfo.getFileNamePath();
         this.s_type=this_classinfo.getTypeText();
         this.s_short_text=this_classinfo.getShortText();
         this.s_long_text=this_classinfo.getLongText();
         this.s_short_description=this_classinfo.getShortDescription();
         this.s_long_description=this_classinfo.getLongDescription();
         this.s_class_name=this_classinfo.getClassName();
         this.i_type=this_classinfo.getType();
         this.s_bounds=this_classinfo.getBounds();
         this.s_center=this_classinfo.getCenter();
         this.s_zoom_levels=this_classinfo.getZoom_Levels();
        }
        else
        {
         // this.id_classinfo=0;
         this.s_file_path="";
         this.s_type="";
         this.s_short_text="this_classinfo == null";
         this.s_long_text="";
         this.s_short_description="";
         this.s_long_description="";
         this.s_class_name="";
         this.i_type=-1;
         this.s_bounds="";
         this.s_center="";
         this.s_zoom_levels="";
        }
    }

    public int indexOf(final T id) {
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
            for (final InMemoryTreeNode<T> n : children) {
                childIdListCache.add(n.getId());
            }
        }
        return childIdListCache;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public int getChildrenListSize() {
        return children.size();
    }

    public synchronized InMemoryTreeNode<T> add(final int index, final T child,
            final boolean visible, ClassNodeInfo this_classinfo) {
        childIdListCache = null;
        // Note! top levell children are always visible (!)
        final InMemoryTreeNode<T> newNode = new InMemoryTreeNode<T>(child,
                getId(), getLevel() + 1, getId() == null ? true : visible,this_classinfo);
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

    public synchronized void removeChild(final T child) {
        final int childIndex = indexOf(child);
        if (childIndex != -1) {
            children.remove(childIndex);
            childIdListCache = null;
        }
    }

    @Override
    public String toString() {
        return "InMemoryTreeNode [id=" + getId() + ", parent=" + getParent()
                + ", level=" + getLevel() + ", visible=" + visible
                + ", children=" + children + ", childIdListCache="
                + childIdListCache
                + ", file_path=" + getFileNamePath();
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
    public ClassNodeInfo getClassNodeInfo()
    {
      return new ClassNodeInfo(getId(),getType(),getTypeText(),getClassName(),getFileNamePath(),
      getShortText(),getLongText(),getShortDescription(),getLongDescription(),getBounds(),getCenter(),getZoom_Levels());
    }

    public String getFileNamePath()
    {
      return s_file_path;
    }
    public String getClassName() {
        return s_class_name;
    }
    public String getShortText() {
        return s_short_text;
    }
    public String getLongText() {
        return s_long_text;
    }
    public String getShortDescription() {
        return s_short_description;
    }
    public String getLongDescription() {
        return s_long_description;
    }
    public String getBounds()
   {
    return s_bounds;
   }
   public String getCenter()
   {
    return s_center;
   }
    public String getZoom_Levels()
   {
    return s_zoom_levels;
   }
    public String getTypeText() {
        return s_type;
    }
    public int getType() {
        return i_type;
    }
}
