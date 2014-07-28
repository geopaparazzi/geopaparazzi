package eu.hydrologis.geopaparazzi.osm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

/**
 * Image cache.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmImageCache {

    private static OsmImageCache osmImageCache = null;

    private LinkedHashMap<String, SoftReference<Drawable>> imageMap = new LinkedHashMap<String, SoftReference<Drawable>>();
    private File tagsFolderFile;

    private OsmImageCache( Context context ) {
        try {
            tagsFolderFile = OsmTagsManager.getInstance().getTagsFolderFile(context);
        } catch (Exception e) {
            e.printStackTrace();
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
     * @param context  the context to use.
     * @return teh image cache.
     */
    public static OsmImageCache getInstance( Context context ) {
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
    public Drawable getImageForTag( String tagName, String category ) {
        StringBuilder sb = new StringBuilder();
        sb.append(category);
        sb.append("/"); //$NON-NLS-1$
        sb.append(tagName);
        sb.append(OsmTagsManager.ICON_POSTFIX_MEDIUMDENSITY);
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
