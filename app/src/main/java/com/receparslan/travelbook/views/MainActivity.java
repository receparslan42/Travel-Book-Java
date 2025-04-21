package com.receparslan.travelbook.views;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.receparslan.travelbook.R;
import com.receparslan.travelbook.databinding.ActivityMainBinding;
import com.receparslan.travelbook.model.Location;
import com.receparslan.travelbook.recycler_adapter.RecyclerAdapter;
import com.receparslan.travelbook.roomDB.AppDatabase;
import com.receparslan.travelbook.roomDB.LocationDAO;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable(); // Disposable for RxJava operations
    private List<Location> locationList; // List of locations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // View binding
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater()); // Inflate view binding
        setContentView(binding.getRoot()); // Set view

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set custom action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.action_bar);
        }

        // Create database and DAO objects
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "locations").build();
        LocationDAO locationDAO = db.locationDAO();

        // Get all locations from database with RxJava
        compositeDisposable.add(locationDAO.getAllLocations()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(locations -> {
                    locationList = locations;

                    // Set recycler view with location list
                    RecyclerView recyclerView = binding.recyclerView;
                    recyclerView.setAdapter(new RecyclerAdapter(locationList));
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                }));
    }

    // Create options menu and inflate it
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Handle options menu item click
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // If add place button clicked, start MapsActivity with new location info
        if (item.getItemId() == R.id.addPlace) {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("info", "new");
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    // Dispose composite disposable on destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }
}