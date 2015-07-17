package eu.hydrologis.geopaparazzi.osm;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

import eu.geopaparazzi.library.database.GPLog;

/**
 * Image cache.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmImageCache {

    private static OsmImageCache osmImageCache = null;

    private LinkedHashMap<String, SoftReference<Drawable>> imageMap = new LinkedHashMap<String, SoftReference<Drawable>>();
    private File tagsFolderFile;
    private String postfix = OsmTagsManager.ICON_POSTFIX_MEDIUMDENSITY;

    private OsmImageCache(Context context) {
        try {
            tagsFolderFile = OsmTagsManager.getInstance().getTagsFolderFile(context);

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            switch (metrics.densityDpi) {
                case DisplayMetrics.DENSITY_LOW:
                    postfix = OsmTagsManager.ICON_POSTFIX_LOWDENSITY;
                    break;
                case DisplayMetrics.DENSITY_DEFAULT:
                    postfix = OsmTagsManager.ICON_POSTFIX_MEDIUMDENSITY;
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                case DisplayMetrics.DENSITY_XHIGH:
                case DisplayMetrics.DENSITY_XXHIGH:
                case DisplayMetrics.DENSITY_XXXHIGH:
                    postfix = OsmTagsManager.ICON_POSTFIX_HIGHDENSITY;
                    break;
                default:
                    postfix = OsmTagsManager.ICON_POSTFIX_MEDIUMDENSITY;
                    break;
            }
        } catch (Exception e) {
            GPLog.error(this, null, e); //$NON-NLS-1$
        }
    }

    // public void dispose(){
    // Collection<SoftReference<Drawable>> values = imageMap.values();
    // for( SoftReference<Drawable> drawableRef : values ) {
    // Drawable drawable = drawableRef.get();
    // dra
    // }
    // }

    /**
     * @param context the context to use.
     * @return teh image cache.
     */
    public static OsmImageCache getInstance(Context context) {
        if (osmImageCache == null) {
            osmImageCache = new OsmImageCache(context);
        }
        return osmImageCache;
    }

    /**
     * Get image for tag and category.
     *
     * @param tagName  the tag
     * @param category the category.
     * @return the {@link Drawable}.
     */
    public Drawable getImageForTag(String tagName, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append(category);
        sb.append("/"); //$NON-NLS-1$
        sb.append(tagName);
        sb.append(postfix);
        String relativePath = sb.toString();

        SoftReference<Drawable> softReference = imageMap.get(relativePath);
        if (softReference == null || softReference.get() == null) {
            File tagImageFile = new File(tagsFolderFile, sb.toString());
            Drawable drawable = Drawable.createFromPath(tagImageFile.getAbsolutePath());
            softReference = new SoftReference<Drawable>(drawable);
            imageMap.put(relativePath, softReference);
        }

        return softReference.get();
    }
}
