package com.receparslan.travelbook.roomDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.receparslan.travelbook.model.Location;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface LocationDAO {
    @Query("SELECT * FROM locations")
    Flowable<List<Location>> getAllLocations();

    @Query("SELECT * FROM locations WHERE id IN (:id)")
    Flowable<Location> getLocationById(int id);

    @Insert
    Completable insertLocation(Location location);

    @Delete
    Completable deleteLocation(Location location);
}
