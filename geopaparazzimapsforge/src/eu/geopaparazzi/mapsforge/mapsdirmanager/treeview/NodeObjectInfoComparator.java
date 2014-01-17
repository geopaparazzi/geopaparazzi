package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
import java.util.Comparator;
// Collections.sort(list, decending(getComparator(SORT_DIRECTORY, SORT_FILE)));
public class NodeObjectInfoComparator implements Comparator<NodeObjectInfo> {
    private NodeObjectInfo.SortParameter[] parameters;
    public NodeObjectInfoComparator( NodeObjectInfo.SortParameter[] parameters ) {
        this.parameters = parameters;
    }
    public int compare( NodeObjectInfo one, NodeObjectInfo another ) {
        int comparison;
        for( NodeObjectInfo.SortParameter parameter : parameters ) {
            switch( parameter ) {
            case SORT_TYPE_TEXT:
                comparison = one.getTypeText().toLowerCase().compareTo(another.getTypeText().toLowerCase());
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_DIRECTORY:
                comparison = one.getFilePath().getParent().toLowerCase()
                        .compareTo(another.getFilePath().getParent().toLowerCase());
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_FILE:
                comparison = one.getFileName().toLowerCase().compareTo(another.getFileName().toLowerCase());
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_FILENAME_PATH:
                comparison = one.getFileNamePath().toLowerCase().compareTo(another.getFileNamePath().toLowerCase());
                if (comparison != 0)
                    return comparison;
                break;
            case SORT_ENABLED:
                String s_one = "" + one.getEnabled();
                String s_another = "" + another.getEnabled();
                comparison = s_one.compareTo(s_another);
                if (comparison != 0)
                    return comparison;
                break;
            }
        }
        return 0;
    }
}