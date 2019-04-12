package eu.geopaparazzi.map.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import eu.geopaparazzi.map.R;

public class MapLayerListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            showFragment(MapLayerListFragment.newInstance());
        }

        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.main_decorations)));
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment, "fragment").commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        boolean listFragment = getSupportFragmentManager().findFragmentByTag("fragment") instanceof ListFragment;
//        menu.findItem(R.id.action_lists).setVisible(!listFragment);
//        menu.findItem(R.id.action_board).setVisible(listFragment);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_lists:
//                showFragment(ListFragment.newInstance());
//                return true;
//            case R.id.action_board:
//                showFragment(MapLayerListFragment.newInstance());
//                return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}