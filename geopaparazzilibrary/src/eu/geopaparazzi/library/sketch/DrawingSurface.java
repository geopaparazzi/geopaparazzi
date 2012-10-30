package eu.geopaparazzi.library.sketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import eu.geopaparazzi.library.sketch.commands.CommandManager;
import eu.geopaparazzi.library.sketch.commands.DrawingPath;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * The drawing surface..
 * 
 * <p>Adapted for geopaparazzi.</p>
 * 
 * @author almondmendoza (http://www.tutorialforandroid.com/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback {
    private Boolean _run = false;
    protected DrawThread thread;
    private Bitmap mBitmap;
    public static boolean isDrawing = true;
    public DrawingPath previewPath;

    private CommandManager commandManager;

    public DrawingSurface( Context context, AttributeSet attrs ) {
        super(context, attrs);

        getHolder().addCallback(this);

        commandManager = new CommandManager();
        thread = new DrawThread(getHolder());
    }

    private static Handler previewDoneHandler = new Handler(){
        @Override
        public void handleMessage( Message msg ) {
            isDrawing = false;
        }
    };

    class DrawThread extends Thread {
        private SurfaceHolder mSurfaceHolder;

        public DrawThread( SurfaceHolder surfaceHolder ) {
            mSurfaceHolder = surfaceHolder;

        }

        public void setRunning( boolean run ) {
            _run = run;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            while( _run ) {
                if (isDrawing == true) {
                    try {
                        canvas = mSurfaceHolder.lockCanvas(null);
                        if (mBitmap == null) {
                            // mBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
                            Logger.i(this, "Canvas not ready yet...");
                            continue;
                        }
                        final Canvas c = new Canvas(mBitmap);

                        c.drawColor(Color.WHITE);
                        // c.drawColor(0, PorterDuff.Mode.CLEAR);
                        canvas.drawColor(Color.WHITE);
                        // canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                        commandManager.executeAll(c, previewDoneHandler);

                        // if (Debug.D) {
                        // if (!previewPath.path.isEmpty()) {
                        // Logger.i(this,
                        //                                        "Style: " + previewPath.paint.getStrokeWidth() + "/" + previewPath.paint.getColor()); //$NON-NLS-1$//$NON-NLS-2$
                        // }
                        // }
                        previewPath.draw(c);

                        canvas.drawBitmap(mBitmap, 0, 0, null);
                    } finally {
                        if (canvas != null) {
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }

                }

            }

        }
    }

    public void addDrawingPath( DrawingPath drawingPath ) {
        commandManager.addCommand(drawingPath);
    }

    public boolean hasMoreRedo() {
        return commandManager.hasMoreRedo();
    }

    public void redo() {
        isDrawing = true;
        commandManager.redo();

    }

    public void undo() {
        isDrawing = true;
        commandManager.undo();
    }

    public boolean hasMoreUndo() {
        return commandManager.hasMoreUndo();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);;
    }

    public void surfaceCreated( SurfaceHolder holder ) {
        if (!_run) {
            thread.setRunning(true);
            thread.start();
        }
    }

    public void surfaceDestroyed( SurfaceHolder holder ) {
        boolean retry = true;
        thread.setRunning(false);
        while( retry ) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

}
