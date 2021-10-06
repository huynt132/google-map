package vn.edu.fpt.google_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    boolean isPermissionGranted; //result for User allow access or not
    GoogleMap googleMap;
    ImageView imageViewSearch;
    EditText inputLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageViewSearch = findViewById(R.id.imageViewSearch);
        inputLocation = findViewById(R.id.inputLocation);
        checkPermission();

        if (isPermissionGranted) {
            if (checkGoolePlaServices()) {
                //show map when permission is granted
                SupportMapFragment supportMapFragment = SupportMapFragment.newInstance();
                getSupportFragmentManager().beginTransaction().add(R.id.container, supportMapFragment).commit();
                supportMapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Google Playservices Not Available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Pls grant permission", Toast.LENGTH_SHORT).show();
        }

        imageViewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String location = inputLocation.getText().toString();
                //validate
                if (TextUtils.isEmpty(location)) {
                    Toast.makeText(MainActivity.this, "Type any location name", Toast.LENGTH_SHORT).show();
                } else {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    try {
                        List<Address> listAddress = geocoder.getFromLocationName(location, 1);

                        if (listAddress.size() > 0) {

                            hideSoftKeyboard(findViewById(android.R.id.content).getRootView());

                            LatLng latLng = new LatLng(listAddress.get(0).getLatitude(), listAddress.get(0).getLongitude());

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.title(listAddress.get(0).getAddressLine(0));
                            markerOptions.position(latLng);
                            googleMap.addMarker(markerOptions);

                            movePosition(latLng, googleMap, 12);

                            //show the text of searched COUNTRY NAME
                            Toast.makeText(MainActivity.this, "" + listAddress.get(0).getCountryName(), Toast.LENGTH_SHORT).show();
                        } else {
                            hideSoftKeyboard(findViewById(android.R.id.content).getRootView());
                            Toast.makeText(MainActivity.this, "Location not found!!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /*
     this method use to hide virtual input keyboard
     */
    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /*
     this method use to move the map position by LatLng position
     */
    private void movePosition(LatLng position, GoogleMap map, int zoomValue) {
        //Camera animate to zoom in 15 times to the marked location
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .zoom(zoomValue)
                .tilt(25)
                .bearing(75)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(
                cameraPosition));
    }

    private boolean checkGoolePlaServices() {
        //check for available of google play service
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApiAvailability.isUserResolvableError(result)) {
            //if it got error, print error in the dialog
            Dialog dialog = googleApiAvailability.getErrorDialog(this, result, 201, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(MainActivity.this, "User Cancelked Dialoge", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        }
        return false;
    }

    /*
     this method use to check permission to access current location of user
     */
    private void checkPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                isPermissionGranted = true;
                //show message when permission is granted
                Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                //foward user to setting for allow permission
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                //keep continue show permission request
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    /*
     this method use to set up and initial for map when map is ready
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        //mark on tap
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                googleMap.clear();
                googleMap.addMarker(markerOptions);
            }
        });

        //create initial position
        LatLng latLng = new LatLng(21.013550445956177, 105.52711794224213);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title("FPT university");
        markerOptions.position(latLng);
        googleMap.addMarker(markerOptions);
        movePosition(latLng, googleMap, 15);

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        //googleMap.getUiSettings().setScrollGesturesEnabled(false);//disable scroll mouse

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    /*
     This method use to assign options menu for menu activity
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /*
     This method use to handle when selected event raise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //change the map type depend on user selected option
        if (item.getItemId() == R.id.noneMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        if (item.getItemId() == R.id.NormalMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        if (item.getItemId() == R.id.SatelliteMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        if (item.getItemId() == R.id.MapHybrid) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        if (item.getItemId() == R.id.MapTerrain) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }

        return super.onOptionsItemSelected(item);
    }
}