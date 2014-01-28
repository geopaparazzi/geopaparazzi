package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
import java.util.Comparator;
import java.util.Locale;

import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.util.NodeSortParameter;

/**
 * A {@link TreeNode} comparator.
 */
public class TreeNodeComparator implements Comparator<TreeNode< ? >> {
    private static final Locale l = Locale.US;
    private NodeSortParameter[] parameters;

    /**
     * Constructor.
     * 
     * @param parameters the {@link NodeSortParameter}s checked. 
     */
    public TreeNodeComparator( NodeSortParameter[] parameters ) {
        this.parameters = parameters;
    }

    public int compare( TreeNode< ? > one, TreeNode< ? > other ) {
        int comparison;
        for( NodeSortParameter parameter : parameters ) {
            switch( parameter ) {
            case SORT_TYPE_TEXT:
                String oneTypeText = one.getTypeText();
                String otherTypeText = other.getTypeText();
                comparison = oneTypeText.toLowerCase(l).compareTo(otherTypeText.toLowerCase(l));
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_DIRECTORY:
                String oneParent = one.getFile().getParent();
                String otherParent = other.getFile().getParent();
                comparison = oneParent.toLowerCase(l).compareTo(otherParent.toLowerCase(l));
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_FILE_NAME:
                String oneFileName = one.getFileName();
                String otherFileName = other.getFileName();
                comparison = oneFileName.toLowerCase(l).compareTo(otherFileName.toLowerCase(l));
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_FILE_PATH:
                String oneFilePath = one.getFilePath();
                String otherFilePath = other.getFilePath();
                comparison = oneFilePath.toLowerCase(l).compareTo(otherFilePath.toLowerCase(l));
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_ENABLED:
                String oneEnabled = String.valueOf(one.isEnabled());
                String otherEnabled = String.valueOf(other.isEnabled());
                comparison = oneEnabled.compareTo(otherEnabled);
                if (comparison != 0)
                    return comparison;
                break;
            }
        }
        return 0;
    }
}
