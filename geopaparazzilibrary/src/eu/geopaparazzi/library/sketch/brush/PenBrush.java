package eu.geopaparazzi.library.sketch.brush;

import android.graphics.Path;

/**
 * Created by IntelliJ IDEA.
 * User: almondmendoza
 * Date: 01/12/2010
 * Time: 10:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class PenBrush extends Brush{
    @Override
    public void mouseDown(Path path, float x, float y) {
        path.moveTo( x, y );
        path.lineTo(x, y);
    }

    @Override
    public void mouseMove(Path path, float x, float y) {
        path.lineTo( x, y );
    }

    @Override
    public void mouseUp(Path path, float x, float y) {
        path.lineTo( x, y );
    }
}
