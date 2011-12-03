package eu.hydrologis.geopaparazzi.osm;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.LinkedHashMap;

import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class OsmImageCache {

    private static OsmImageCache osmImageCache = null;
    private final Context context;

    private LinkedHashMap<String, SoftReference<Drawable>> imageMap = new LinkedHashMap<String, SoftReference<Drawable>>();

    private OsmImageCache( Context context ) {
        this.context = context;
    }

    // public void dispose(){
    // Collection<SoftReference<Drawable>> values = imageMap.values();
    // for( SoftReference<Drawable> drawableRef : values ) {
    // Drawable drawable = drawableRef.get();
    // dra
    // }
    // }

    public static OsmImageCache getInstance( Context context ) {
        if (osmImageCache == null) {
            osmImageCache = new OsmImageCache(context);
        }
        return osmImageCache;
    }

    public Drawable getImageForTag( String tagName, String category ) {

        File tagsFolderFile = OsmTagsManager.getInstance().getTagsFolderFile(context);
        StringBuilder sb = new StringBuilder();
        sb.append(category);
        sb.append("/");
        sb.append(tagName);
        sb.append(OsmTagsManager.ICON_POSTFIX_XLARGE);
        String relativePath = sb.toString();

        SoftReference<Drawable> softReference = imageMap.get(relativePath);
        if (softReference == null) {
            File tagImageFile = new File(tagsFolderFile, sb.toString());
            Drawable drawable = Drawable.createFromPath(tagImageFile.getAbsolutePath());
            softReference = new SoftReference<Drawable>(drawable);
            imageMap.put(relativePath, softReference);
        }

        return softReference.get();
    }
}
