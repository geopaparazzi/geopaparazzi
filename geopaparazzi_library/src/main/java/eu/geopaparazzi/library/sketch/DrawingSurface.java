/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.sketch.commands.CommandManager;
import eu.geopaparazzi.library.sketch.commands.DrawingPath;

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
    /**
     *
     */
    public static boolean isDrawing = true;
    /**
     *
     */
    public DrawingPath previewPath;

    private CommandManager commandManager;

    private volatile boolean isDisposed = false;

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public DrawingSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        isDisposed = false;
        getHolder().addCallback(this);

        commandManager = new CommandManager();
        thread = new DrawThread(getHolder());
    }

    private static Handler previewDoneHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            isDrawing = false;
        }
    };
    private File imageFile;
    private boolean dumpToImage;

    class DrawThread extends Thread {
        private SurfaceHolder mSurfaceHolder;

        public DrawThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;

        }

        public void setRunning(boolean run) {
            isDisposed = false;
            _run = run;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            while (_run) {
                if (isDrawing == true) {
                    try {
                        canvas = mSurfaceHolder.lockCanvas(null);
                        if (mBitmap == null) {
                            // Logger.i(this, "Canvas not ready yet...");
                            continue;
                        }
                        if (isDisposed) {
                            break;
                        }
                        final Canvas c = new Canvas(mBitmap);

                        c.drawColor(Color.WHITE);
                        // c.drawColor(0, PorterDuff.Mode.CLEAR);
                        if (canvas == null || mBitmap.isRecycled()) {
                            break;
                        }
                        canvas.drawColor(Color.WHITE);
                        // canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                        commandManager.executeAll(c, previewDoneHandler);

                        // if (Debug.D) {
                        // if (!previewPath.path.isEmpty()) {
                        // Logger.i(this,
                        // "Style: " + previewPath.paint.getStrokeWidth() + "/" + previewPath.paint.getColor()); //$NON-NLS-1$//$NON-NLS-2$
                        // }
                        // }
                        previewPath.draw(c);

                        canvas.drawBitmap(mBitmap, 0, 0, null);

                        if (dumpToImage) {
                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream(imageFile);
                                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                out.flush();
                            } catch (Exception e) {
                                GPLog.error(this, null, e);
                                if (out != null)
                                    try {
                                        out.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                            }
                            dumpToImage = false;
                        }
                    } finally {
                        if (canvas != null) {
                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }

                }

            }

        }
    }

    /**
     * @param drawingPath drawing path
     */
    public void addDrawingPath(DrawingPath drawingPath) {
        commandManager.addCommand(drawingPath);
    }

    /**
     * @return has more redo.
     */
    public boolean hasMoreRedo() {
        return commandManager.hasMoreRedo();
    }

    /**
     *
     */
    public void redo() {
        isDrawing = true;
        commandManager.redo();

    }

    /**
     *
     */
    public void undo() {
        isDrawing = true;
        commandManager.undo();
    }

    /**
     * @return more undo
     */
    public boolean hasMoreUndo() {
        return commandManager.hasMoreUndo();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "Recreating bitmap"); //NON-NLS
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (!_run) {
            thread.setRunning(true);
            thread.start();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    /**
     *
     */
    public void dispose() {
        if (mBitmap != null) {
            isDisposed = true;
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    /**
     * Dump image to file.
     *
     * @param imageFile the file.
     * @throws IOException if something goes wrong.
     */
    public void dumpImage(File imageFile) throws IOException {
        this.imageFile = imageFile;
        dumpToImage = true;
    }

}
