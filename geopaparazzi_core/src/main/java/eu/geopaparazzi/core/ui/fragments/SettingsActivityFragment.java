// SettingsActivityFragment.java
// Subclass of PreferenceFragment for managing app settings
package eu.geopaparazzi.core.ui.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import eu.geopaparazzi.core.R;

public class SettingsActivityFragment extends PreferenceFragment {
   // creates preferences GUI from preferences.xml file in res/xml
   @Override
   public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      addPreferencesFromResource(R.xml.preferences); // load from XML
   }

}
