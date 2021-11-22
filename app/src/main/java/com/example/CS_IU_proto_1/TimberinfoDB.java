package com.example.CS_IU_proto_1;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Timberinfo.class}, version = 1)
public abstract class TimberinfoDB extends RoomDatabase {
    private  static TimberinfoDB INSTANCE = null;

    public abstract TimberinfoDao timberinfoDao();

    public static TimberinfoDB getInstance(Context context){
        if (INSTANCE == null){
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),TimberinfoDB.class, "timberinfo.db").build();
        }
        return INSTANCE;
    }
    public static void destoryInstance() {
        INSTANCE = null;
    }
}
