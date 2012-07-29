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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.routing.google.GRouter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Activity that performs geocoding on a user entered location.
 * 
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 * @author Andrea Antonello (www.hydrologis.com) - geopaparazzi adaptions
 */
public class GeocodeActivity extends ListActivity {
    private static final int MAX_ADDRESSES = 30;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.geocode);
    }

    public void onLookupLocationClick( View view ) {
        // TODO add it back when the version permits it
        // if (Geocoder.isPresent())
        // {
        EditText addressText = (EditText) findViewById(R.id.enterLocationValue);

        try {
            List<Address> addressList = new Geocoder(this).getFromLocationName(addressText.getText().toString(), MAX_ADDRESSES);

            List<AddressWrapper> addressWrapperList = new ArrayList<AddressWrapper>();

            for( Address address : addressList ) {
                addressWrapperList.add(new AddressWrapper(address));
            }

            setListAdapter(new ArrayAdapter<AddressWrapper>(this, R.layout.geocode_row, addressWrapperList));
        } catch (IOException e) {
            Logger.e(this, "Could not geocode address", e); //$NON-NLS-1$
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

    public void onOkClick( View view ) {
        ListView listView = getListView();

        Intent intent = getIntent();
        if (listView.getCheckedItemPosition() != ListView.INVALID_POSITION) {
            AddressWrapper addressWrapper = (AddressWrapper) listView.getItemAtPosition(listView.getCheckedItemPosition());

            intent.putExtra(LibraryConstants.NAME, addressWrapper.toString());
            intent.putExtra(LibraryConstants.LATITUDE, addressWrapper.getAddress().getLatitude());
            intent.putExtra(LibraryConstants.LONGITUDE, addressWrapper.getAddress().getLongitude());

            this.setResult(RESULT_OK, intent);
            finish();
        }

    }

    public void onNavClick( View view ) {
        ListView listView = getListView();

        if (listView.getCheckedItemPosition() != ListView.INVALID_POSITION) {
            AddressWrapper addressWrapper = (AddressWrapper) listView.getItemAtPosition(listView.getCheckedItemPosition());
            String featureName = addressWrapper.getAddress().getFeatureName();
            double latitude = addressWrapper.getAddress().getLatitude();
            double longitude = addressWrapper.getAddress().getLongitude();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            double[] lonLatZoom = PositionUtilities.getMapCenterFromPreferences(preferences, false, false);

            GRouter router = new GRouter(lonLatZoom[1], lonLatZoom[0], latitude, longitude);
            String routeString = router.getRouteString();

            Intent intent = getIntent();
            intent.putExtra(LibraryConstants.ROUTE, routeString);
            intent.putExtra(LibraryConstants.NAME, featureName);

            this.setResult(RESULT_OK, intent);

            finish();
        }

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
