package eu.geopaparazzi.library.sketch;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.sketch.brush.Brush;
import eu.geopaparazzi.library.sketch.brush.PenBrush;
import eu.geopaparazzi.library.sketch.commands.DrawingPath;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Main drawing activity.
 * 
 * @author almondmendoza (http://www.tutorialforandroid.com/)
 */
public class DrawingActivity extends Activity implements View.OnTouchListener {
    private DrawingSurface drawingSurface;
    private DrawingPath currentDrawingPath;
    private Paint currentPaint;
    private Paint previewPaint;

    private Button redoBtn;
    private Button undoBtn;

    private Brush currentBrush;
    private int currentColor;
    private float currentWidth;

    private File APP_FILE_PATH = new File("/sdcard/TutorialForAndroidDrawings");
    private Spinner colorSpinner;
    private Spinner widthSpinner;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);

        currentBrush = new PenBrush();
        currentWidth = 2f;

        drawingSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
        drawingSurface.setOnTouchListener(this);
        drawingSurface.previewPath = new DrawingPath();
        drawingSurface.previewPath.path = new Path();

        redoBtn = (Button) findViewById(R.id.redoButton);
        undoBtn = (Button) findViewById(R.id.undoButton);

        colorSpinner = (Spinner) findViewById(R.id.colorspinner);
        ArrayAdapter< ? > colorSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_colornames,
                android.R.layout.simple_spinner_item);
        colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorSpinnerAdapter);
        colorSpinner.setSelection(0);
        colorSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                onClick(colorSpinner);
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

        widthSpinner = (Spinner) findViewById(R.id.widthspinner);
        ArrayAdapter< ? > widthSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_widths,
                android.R.layout.simple_spinner_item);
        widthSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widthSpinner.setAdapter(widthSpinnerAdapter);
        widthSpinner.setSelection(1);
        widthSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                onClick(widthSpinner);
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

        setCurrentPaint();
        drawingSurface.previewPath.paint = previewPaint;

        redoBtn.setEnabled(false);
        undoBtn.setEnabled(false);
    }

    private void setCurrentPaint() {
        checkColor();
        checkWidth();

        currentPaint = new Paint();
        currentPaint.setDither(true);
        currentPaint.setColor(currentColor);
        currentPaint.setAlpha(255);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(currentWidth);

        previewPaint = new Paint();
        previewPaint.setDither(true);
        previewPaint.setColor(currentColor);
        previewPaint.setAlpha(255);
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeJoin(Paint.Join.ROUND);
        previewPaint.setStrokeCap(Paint.Cap.ROUND);
        previewPaint.setStrokeWidth(currentWidth);

    }

    public boolean onTouch( View view, MotionEvent motionEvent ) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        // if (Debug.D) {
        //            Logger.i(this, "Drawing: " + x + "/" + y); //$NON-NLS-1$//$NON-NLS-2$
        // }
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            drawingSurface.isDrawing = true;

            currentDrawingPath = new DrawingPath();
            currentDrawingPath.paint = currentPaint;
            currentDrawingPath.path = new Path();
            currentBrush.mouseDown(currentDrawingPath.path, x, y);
            currentBrush.mouseDown(drawingSurface.previewPath.path, x, y);

        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            drawingSurface.isDrawing = true;
            currentBrush.mouseMove(currentDrawingPath.path, x, y);
            currentBrush.mouseMove(drawingSurface.previewPath.path, x, y);

        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            currentBrush.mouseUp(drawingSurface.previewPath.path, x, y);
            drawingSurface.previewPath.path = new Path();
            drawingSurface.addDrawingPath(currentDrawingPath);

            currentBrush.mouseUp(currentDrawingPath.path, x, y);

            undoBtn.setEnabled(true);
            redoBtn.setEnabled(false);

        }

        return true;
    }

    public void onClick( View view ) {
        int id = view.getId();
        if (id == R.id.colorspinner || id == R.id.widthspinner) {
            setCurrentPaint();
            drawingSurface.previewPath.paint = previewPaint;
        } else if (id == R.id.undoButton) {
            drawingSurface.undo();
            if (drawingSurface.hasMoreUndo() == false) {
                undoBtn.setEnabled(false);
            }
            redoBtn.setEnabled(true);
        } else if (id == R.id.redoButton) {
            drawingSurface.redo();
            if (drawingSurface.hasMoreRedo() == false) {
                redoBtn.setEnabled(false);
            }

            undoBtn.setEnabled(true);
        }
        // else if (id == R.id.saveBtn) {
        // final Activity currentActivity = this;
        // Handler saveHandler = new Handler(){
        // @Override
        // public void handleMessage( Message msg ) {
        // final AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).create();
        // alertDialog.setTitle("Saved 1");
        // alertDialog.setMessage("Your drawing had been saved :)");
        // alertDialog.setButton("OK", new DialogInterface.OnClickListener(){
        // public void onClick( DialogInterface dialog, int which ) {
        // return;
        // }
        // });
        // alertDialog.show();
        // }
        // };
        // new ExportBitmapToFile(this, saveHandler, drawingSurface.getBitmap()).execute();
        // } else if (id == R.id.circleBtn) {
        // currentBrush = new CircleBrush();
        // } else if (id == R.id.pathBtn) {
        // currentBrush = new PenBrush();
        // }
    }

    private void checkWidth() {
        Object selectedItem = widthSpinner.getSelectedItem();
        float newWidth = Float.parseFloat(selectedItem.toString());
        currentWidth = newWidth;
    }

    private void checkColor() {
        Object selectedItem = colorSpinner.getSelectedItem();
        String newColorStr = selectedItem.toString();
        // if (newColorStr.equals("red")) {
        // currentColor = 0xFF0000FF;
        // currentColor = 0xFFC1C1C1;
        // } else {
        currentColor = Color.parseColor(newColorStr.trim());
        // }
    }

    private class ExportBitmapToFile extends AsyncTask<Intent, Void, Boolean> {
        private Context mContext;
        private Handler mHandler;
        private Bitmap nBitmap;

        public ExportBitmapToFile( Context context, Handler handler, Bitmap bitmap ) {
            mContext = context;
            nBitmap = bitmap;
            mHandler = handler;
        }

        @Override
        protected Boolean doInBackground( Intent... arg0 ) {
            try {
                if (!APP_FILE_PATH.exists()) {
                    APP_FILE_PATH.mkdirs();
                }

                final FileOutputStream out = new FileOutputStream(new File(APP_FILE_PATH + "/myAwesomeDrawing.png"));
                nBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // mHandler.post(completeRunnable);
            return false;
        }

        @Override
        protected void onPostExecute( Boolean bool ) {
            super.onPostExecute(bool);
            if (bool) {
                mHandler.sendEmptyMessage(1);
            }
        }
    }
}