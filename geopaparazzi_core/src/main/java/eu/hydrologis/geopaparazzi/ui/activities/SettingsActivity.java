// SettingsActivity.java
// Activity to display SettingsActivityFragment on a phone
package eu.hydrologis.geopaparazzi.ui.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import eu.hydrologis.geopaparazzi.R;

public class SettingsActivity extends AppCompatActivity {
   // inflates the GUI, displays Toolbar and adds "up" button
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_settings);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
   }
}
