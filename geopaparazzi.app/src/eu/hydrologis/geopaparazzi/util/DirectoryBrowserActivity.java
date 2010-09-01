package eu.hydrologis.geopaparazzi.util;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import eu.hydrologis.geopaparazzi.R;

public class DirectoryBrowserActivity extends ListActivity {

    private List<String> items = null;
    private File geoPaparazziDir;
    private String intentId;
    private String extention;
    private FileFilter fileFilter;

    private File currentDir;

    @Override
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.browse);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intentId = extras.getString(Constants.INTENT_ID);
            extention = extras.getString(Constants.EXTENTION);

            fileFilter = new FileFilter(){
                public boolean accept( File pathname ) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    return pathname.getAbsolutePath().toLowerCase().endsWith(extention.toLowerCase());
                }
            };
        }

        geoPaparazziDir = ApplicationManager.getInstance().getGeoPaparazziDir();
        getFiles(geoPaparazziDir, geoPaparazziDir.listFiles(fileFilter));
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        int selectedRow = (int) id;
        if (selectedRow == 0) {
            currentDir = currentDir.getParentFile();
            getFiles(currentDir, currentDir.listFiles(fileFilter));
        } else {
            File file = new File(items.get(selectedRow));
            if (file.isDirectory()) {
                currentDir = file;
                getFiles(currentDir, currentDir.listFiles(fileFilter));
            } else {
                String absolutePath = file.getAbsolutePath();
                Intent intent = new Intent(intentId);
                intent.putExtra(Constants.PATH, absolutePath);
                startActivity(intent);
                finish();
            }
        }
    }

    private void getFiles( File parent, File[] files ) {
        Arrays.sort(files);
        currentDir = parent;
        items = new ArrayList<String>();
        items.add(parent.getAbsolutePath());

        // items.add(getString(R.string.goto_root));
        for( File file : files ) {
            items.add(file.getPath());
        }
        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.browse_file_row, items);
        setListAdapter(fileList);
    }
}