package com.receparslan.travelbook.views;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.receparslan.travelbook.R;
import com.receparslan.travelbook.databinding.ActivityMapsBinding;
import com.receparslan.travelbook.model.Location;
import com.receparslan.travelbook.roomDB.AppDatabase;
import com.receparslan.travelbook.roomDB.LocationDAO;

import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Composite disposable for disposing observables
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    // Location database access object
    private LocationDAO locationDAO;
    // Fused location provider client for updating location
    private FusedLocationProviderClient fusedLocationProviderClient;
    // Location objects for old and new locations
    private Location oldLocation;
    private Location newLocation;
    // Marker options for the map
    private MarkerOptions markerOptions;
    private String info; // Info for old or new location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View binding
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        // Get the info from intent for old or new location
        info = getIntent().getStringExtra("info");

        // Room database initialization
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "locations").build();
        locationDAO = db.locationDAO();

        // Fused location provider client initialization
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if the activity is started for old or new location
        if (info.equals("old")) {
            // If the activity is started for old location, show the old location on the map
            binding.nameTextView.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.VISIBLE);
            binding.saveButton.setVisibility(View.GONE);
            binding.nameEditText.setVisibility(View.GONE);

            // Get the old location from the database
            compositeDisposable.add(locationDAO.getLocationById(getIntent().getIntExtra("locationID", 0))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(location -> {
                        oldLocation = location;
                        binding.nameTextView.setText(oldLocation.getName());
                    }));
        } else {
            // If the activity is started for new location, show the map and allow the user to select a location
            binding.nameTextView.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.GONE);
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.nameEditText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Set the myLocation button to the bottom right corner
        View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.setMargins(0, 0, 30, 150);
        locationButton.setLayoutParams(params);

        checkPermission(); // Check location permission

        // Check if the activity is started for old or new location and set the map accordingly
        if (info.equals("old")) {
            // Show the old location on the map with a marker
            mMap.clear();
            markerOptions = new MarkerOptions().position(new LatLng(oldLocation.getLatitude(), oldLocation.getLongitude())).title(oldLocation.getName());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(oldLocation.getLatitude(), oldLocation.getLongitude()), 15));
            mMap.addMarker(markerOptions);

            // Set the on click listeners for the move camera to the old location
            binding.nameTextView.setOnClickListener(view -> mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(), 15)));

            // Set the on click listener for the delete button
            binding.deleteButton.setOnClickListener(view -> {
                // Show an alert dialog for the delete confirmation
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Delete Location");
                builder.setMessage("Are you sure you want to delete this location?");
                builder.setPositiveButton("Yes", (dialogInterface, i) -> compositeDisposable.add(locationDAO.deleteLocation(oldLocation)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            // If the location is deleted, go back to the home screen
                            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        })));
                builder.setNegativeButton("No", null);
                builder.show();
            });
        } else {
            // Set the on long click listener for the map to select a new location
            mMap.setOnMapLongClickListener(latLng -> {
                mMap.clear();
                markerOptions = new MarkerOptions().position(latLng);
                mMap.addMarker(markerOptions);

                newLocation = new Location();
                newLocation.setLatitude(markerOptions.getPosition().latitude);
                newLocation.setLongitude(markerOptions.getPosition().longitude);
            });

            // Set the on click listener for the save button
            binding.saveButton.setOnClickListener(view -> {
                // Check if the new location is selected
                if (newLocation == null) {
                    Toast.makeText(this, "Please select a location!", Toast.LENGTH_SHORT).show();
                } else {
                    String locationName = binding.nameEditText.getText().toString(); // Get the location name from the edit text

                    // Check if the location name is empty
                    if (locationName.isEmpty()) {
                        Toast.makeText(this, "Please enter a name!", Toast.LENGTH_SHORT).show();
                    } else {
                        newLocation.setName(locationName); // Set the location name

                        // Insert the new location to the database
                        compositeDisposable.add(locationDAO.insertLocation(newLocation)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(() -> {
                                    Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }));
                    }
                }
            });
        }
    }

    // Check location permission
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted and show the user's location on the map
            fusedLocationProviderClient.requestLocationUpdates(new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build(), location -> {
                // Enable the location on the map and move the camera to the user's location once
                if (!mMap.isMyLocationEnabled()) {
                    mMap.setMyLocationEnabled(true);
                    if (markerOptions == null)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                }
            }, getMainLooper());
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Permission is denied
            Snackbar.make(findViewById(R.id.main), "Permission needed for location!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission", v -> locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION))
                    .show();
        } else {
            // Permission is denied and never asked before
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // Dispose the composite disposable on activity destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    public final ActivityResultLauncher<String> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        // Callback for the permission request result
        if (isGranted) {
            // Location access granted by the user and check the permission for updating user's location
            checkPermission();
        } else {
            // No location access granted by the user and show a toast message
            Toast.makeText(this, "Permission needed for location!", Toast.LENGTH_SHORT).show();
        }
    });
}
