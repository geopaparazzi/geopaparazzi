package eu.hydrologis.geopaparazzi.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class Toasts {

    public static void longAsyncToast( final Context context, final String message ) {
        Log.d("TOASTS", message);
        // final Handler handler = new Handler(){
        // public void handleMessage( Message msg ) {
        // Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        // };
        // };
        // new Thread(){
        // public void run() {
        // Looper.prepare();
        // //
        // Handler handler = new Handler(){
        // public void handleMessage( Message msg ) {
        // Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        // }
        // };
        //
        // Looper.loop();
        //
        // handler.sendEmptyMessage(0);
        // }
        // }.start();
        AsyncToast asyncToast = new AsyncToast(context, message, Toast.LENGTH_LONG);
        asyncToast.execute("");
    }
    private static class AsyncToast extends AsyncTask<String, String, String> {

        private final Context context;
        private final String message;
        private final int length;

        public AsyncToast( Context context, String message, int length ) {
            this.context = context;
            this.message = message;
            this.length = length;
        }

        protected String doInBackground( String... arg0 ) {
            return null;
        }

        @Override
        protected void onPostExecute( String result ) {
            Toast.makeText(context, message, length).show();
        }

    }
}
