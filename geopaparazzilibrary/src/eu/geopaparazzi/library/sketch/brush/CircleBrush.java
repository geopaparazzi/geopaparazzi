package eu.geopaparazzi.library.sketch.brush;

import android.graphics.Path;

/**
 * Created by IntelliJ IDEA.
 * User: almondmendoza
 * Date: 01/12/2010
 * Time: 11:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class CircleBrush extends Brush{

    @Override
    public void mouseMove(Path path, float x, float y) {
        path.addCircle(x,y,10,Path.Direction.CW);
    }

}
