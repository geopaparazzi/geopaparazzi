// SettingsActivityFragment.java
// Subclass of PreferenceFragment for managing app settings
package eu.hydrologis.geopaparazzi.ui.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import eu.hydrologis.geopaparazzi.R;

public class SettingsActivityFragment extends PreferenceFragment {
   // creates preferences GUI from preferences.xml file in res/xml
   @Override
   public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      addPreferencesFromResource(R.xml.preferences); // load from XML
   }
}
