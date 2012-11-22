package eu.geopaparazzi.library.database.spatial;

import java.util.Comparator;

public class OrderComparator implements Comparator<SpatialTable> {

    @Override
    public int compare( SpatialTable t1, SpatialTable t2 ) {
        if (t1.style == null) {
            t1.makeDefaultStyle();
        }
        if (t2.style == null) {
            t2.makeDefaultStyle();
        }

        if (t1.style.order < t2.style.order) {
            return -1;
        } else if (t1.style.order > t2.style.order) {
            return 1;
        } else
            return 0;
    }

}
