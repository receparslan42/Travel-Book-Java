package com.receparslan.travelbook.roomDB;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.receparslan.travelbook.model.Location;

@Database(entities = {Location.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDAO locationDAO();
}
