// SettingsActivityFragment.java
// Subclass of PreferenceFragment for managing app settings
package eu.geopaparazzi.core.ui.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.Arrays;
import java.util.Map;

import eu.geopaparazzi.core.R;

public class SettingsActivityFragment
        extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{
   // creates preferences GUI from preferences.xml file in res/xml
   SharedPreferences sharedPreferences;

   @Override
   public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      addPreferencesFromResource(R.xml.preferences); // load from XML
   }
   @Override
   public void onResume() {
      super.onResume();
      sharedPreferences = getPreferenceManager().getSharedPreferences();
      // we want to watch the preference values' changes
      sharedPreferences.registerOnSharedPreferenceChangeListener(this);

      Map<String, ?> preferencesMap = sharedPreferences.getAll();
      // iterate through preference entries and update summary if are an instance of EditTextPreference
      for (Map.Entry<String, ?> preferenceEntry : preferencesMap.entrySet()) {
         String key = preferenceEntry.getKey();
         onSharedPreferenceChanged(sharedPreferences,key);
      }
   }

   @Override
   public void onPause() {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
      super.onPause();
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      // get the preference that has been changed
      Preference pref = findPreference(key);
      // and if it's an instance of EditTextPreference class, update its summary
      if (pref instanceof EditTextPreference) {
         updateSummary((EditTextPreference) pref);
      } else if (pref instanceof ListPreference) {
         updateSummary((ListPreference) pref);
      } else if (pref instanceof MultiSelectListPreference) {
         updateSummary((MultiSelectListPreference) pref);
      }
   }

   private void updateSummary(MultiSelectListPreference pref) {
      pref.setSummary(Arrays.toString(pref.getValues().toArray()));
   }

   private void updateSummary(ListPreference pref) {
      pref.setSummary(pref.getValue());
   }

   private void updateSummary(EditTextPreference pref) {
      // set the EditTextPreference's summary value to its current text (except if it is a password)
      if (pref.getTitle().toString().toLowerCase().contains("password")) {
         StringBuilder hidden = new StringBuilder();
         for (int i=0; i< pref.getText().length(); i++) hidden.append("*");
         pref.setSummary(hidden.toString());
      } else {
         pref.setSummary(pref.getText());
      }
   }

}
