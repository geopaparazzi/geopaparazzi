package eu.geopaparazzi.map.graphics;

import android.app.Application;

public enum GPGraphicsFactory {
    INSTANCE;

    public static void createInstance(Application app) {
//        AndroidGraphicFactory.createInstance(app);
    }

    public static void clearResourceMemoryCache() {
//        AndroidGraphicFactory.clearResourceMemoryCache();
    }

    public GPStyle createPaint() {
        return null;
    }
}
