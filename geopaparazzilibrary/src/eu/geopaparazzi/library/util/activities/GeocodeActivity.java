/*
 * Copyright 2011 Greg Milette and Adam Stroud
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.geopaparazzi.library.util.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.routing.openrouteservice.OpenRouteServiceHandler;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Activity that performs geocoding on a user entered location.
 * 
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 * @author Andrea Antonello (www.hydrologis.com) - geopaparazzi adaptions/additions.
 */
public class GeocodeActivity extends ListActivity {
    private static final int MAX_ADDRESSES = 30;

    private String noValidItemSelectedMsg = null;
    private ProgressDialog orsProgressDialog;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.geocode);

        noValidItemSelectedMsg = getString(R.string.no_valid_destination_selected);
    }

    @Override
    protected void onPause() {
        Utilities.dismissProgressDialog(orsProgressDialog);
        super.onPause();
    }

    /**
     * Lookup action.
     * 
     * @param view parent.
     */
    public void onLookupLocationClick( View view ) {
        if (!checkNetwork()) {
            return;
        }
        // TODO add it back when the version permits it
        // if (Geocoder.isPresent())
        // {
        EditText addressText = (EditText) findViewById(R.id.enterLocationValue);

        try {
            List<Address> addressList = new Geocoder(this).getFromLocationName(addressText.getText().toString(), MAX_ADDRESSES);
            if (addressList.size() == 0) {
                Utilities.messageDialog(this, R.string.couldnt_find_geocache_results, null);
                return;
            }

            List<AddressWrapper> addressWrapperList = new ArrayList<AddressWrapper>();

            for( Address address : addressList ) {
                addressWrapperList.add(new AddressWrapper(address));
            }

            setListAdapter(new ArrayAdapter<AddressWrapper>(this, R.layout.geocode_row, addressWrapperList));
        } catch (IOException e) {
            GPLog.error(this, "Could not geocode address", e); //$NON-NLS-1$
            new AlertDialog.Builder(this).setMessage(R.string.geocodeErrorMessage).setTitle(R.string.geocodeErrorTitle)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            dialog.dismiss();
                        }
                    }).show();
        }
        // }
    }

    /**
     * Ok action.
     * 
     * @param view parent.
     */
    public void onOkClick( View view ) {
        if (!checkNetwork()) {
            return;
        }
        ListView listView = getListView();

        Intent intent = getIntent();
        if (listView.getCheckedItemPosition() != ListView.INVALID_POSITION) {
            AddressWrapper addressWrapper = (AddressWrapper) listView.getItemAtPosition(listView.getCheckedItemPosition());

            intent.putExtra(LibraryConstants.NAME, addressWrapper.toString());
            intent.putExtra(LibraryConstants.LATITUDE, addressWrapper.getAddress().getLatitude());
            intent.putExtra(LibraryConstants.LONGITUDE, addressWrapper.getAddress().getLongitude());

            this.setResult(RESULT_OK, intent);
            finish();
        } else {
            Utilities.messageDialog(this, noValidItemSelectedMsg, null);
        }
    }

    private boolean checkNetwork() {
        if (NetworkUtilities.isNetworkAvailable(this)) {
            return true;
        }

        Utilities.messageDialog(this, R.string.available_only_with_network, null);
        return false;
    }

    /**
     * Nav action.
     * 
     * @param view parent.
     */
    public void onNavClick( View view ) {
        if (!checkNetwork()) {
            return;
        }

        ListView mainListView = getListView();
        if (mainListView.getCheckedItemPosition() == ListView.INVALID_POSITION) {
            Utilities.messageDialog(this, noValidItemSelectedMsg, null);
            return;
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String[] items = new String[]{//
        OpenRouteServiceHandler.Preference.Fastest.toString(), //
                OpenRouteServiceHandler.Preference.Shortest.toString(), //
                OpenRouteServiceHandler.Preference.Bicycle.toString() //
        };

        new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    public void onClick( DialogInterface dialog, int whichButton ) {
                        dialog.dismiss();

                        ListView preferenceChoiceListView = ((AlertDialog) dialog).getListView();
                        int selectedPosition = preferenceChoiceListView.getCheckedItemPosition();
                        OpenRouteServiceHandler.Preference orsPreference = OpenRouteServiceHandler.Preference.Fastest;
                        switch( selectedPosition ) {
                        case 1:
                            orsPreference = OpenRouteServiceHandler.Preference.Shortest;
                            break;
                        case 2:
                            orsPreference = OpenRouteServiceHandler.Preference.Bicycle;
                            break;
                        case 0:
                        default:
                            orsPreference = OpenRouteServiceHandler.Preference.Fastest;
                            break;
                        }
                        final OpenRouteServiceHandler.Preference tmpOrsPreference = orsPreference;

                        ListView mainListView = getListView();

                        if (mainListView.getCheckedItemPosition() != ListView.INVALID_POSITION) {
                            AddressWrapper addressWrapper = (AddressWrapper) mainListView.getItemAtPosition(mainListView
                                    .getCheckedItemPosition());
                            final String featureName = addressWrapper.getAddress().getFeatureName();
                            final double latitude = addressWrapper.getAddress().getLatitude();
                            final double longitude = addressWrapper.getAddress().getLongitude();

                            final double[] lonLatZoom = PositionUtilities.getMapCenterFromPreferences(preferences, false, false);
                            final Intent intent = getIntent();

                            orsProgressDialog = ProgressDialog.show(GeocodeActivity.this, getString(R.string.openrouteservice),
                                    getString(R.string.downloading_route), true, false);
                            new AsyncTask<String, Void, String>(){
                                private String usedUrlString;

                                protected String doInBackground( String... params ) {
                                    try {

                                        OpenRouteServiceHandler router = new OpenRouteServiceHandler(lonLatZoom[1],
                                                lonLatZoom[0], latitude, longitude, tmpOrsPreference,
                                                OpenRouteServiceHandler.Language.en);
                                        String errorMessage = router.getErrorMessage();
                                        usedUrlString = router.getUsedUrlString();
                                        if (errorMessage == null) {
                                            float[] routePoints = router.getRoutePoints();

                                            intent.putExtra(LibraryConstants.ROUTE, routePoints);

                                            String distance = router.getDistance();
                                            if (distance != null && distance.length() > 0) {
                                                distance = " (" + distance + router.getUom() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                                            } else {
                                                distance = ""; //$NON-NLS-1$
                                            }
                                            String routeName = getString(R.string.route_to) + featureName + distance;
                                            intent.putExtra(LibraryConstants.NAME, routeName);
                                            return null;
                                        } else {
                                            return errorMessage;
                                        }
                                    } catch (Exception e) {
                                        return getString(R.string.route_extraction_error);
                                    }
                                }

                                protected void onPostExecute( String errorMessage ) { // on UI
                                                                                      // thread!
                                    Utilities.dismissProgressDialog(orsProgressDialog);
                                    if (errorMessage == null) {
                                        GeocodeActivity.this.setResult(RESULT_OK, intent);
                                        finish();
                                    } else {
                                        String openString = errorMessage + "\n\n" //$NON-NLS-1$
                                                + getString(R.string.view_routing_url_in_browser);
                                        Utilities.yesNoMessageDialog(GeocodeActivity.this, openString, new Runnable(){
                                            public void run() {
                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(usedUrlString));
                                                startActivity(browserIntent);
                                            }
                                        }, null);

                                    }
                                }

                            }.execute((String) null);
                        }
                    }
                }).show();

    }

    private static class AddressWrapper {
        private Address address;

        public AddressWrapper( Address address ) {
            this.address = address;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();

            for( int i = 0; i < address.getMaxAddressLineIndex(); i++ ) {
                stringBuilder.append(address.getAddressLine(i));

                if ((i + 1) < address.getMaxAddressLineIndex()) {
                    stringBuilder.append(", "); //$NON-NLS-1$
                }
            }

            return stringBuilder.toString();
        }

        public Address getAddress() {
            return address;
        }
    }
}
