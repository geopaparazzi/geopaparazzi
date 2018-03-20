package eu.geopaparazzi.library.profiles.objects;

import android.os.Parcelable;

/**
 * Created by hydrologis on 20/03/18.
 */

public abstract class ARelativePathResource implements Parcelable {
    protected String relativePath = "";

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}
