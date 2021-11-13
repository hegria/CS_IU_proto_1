package com.example.CS_IU_proto_1;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimberinfoDao {
    @Query("SELECT * FROM timberinfo")
    List<Timberinfo> getAll();

    @Query("SELECT * FROM timberinfo WHERE id IN (:timberinfoIds)")
    List<Timberinfo> loadAllByIds(int[] timberinfoIds);

    @Insert
    void insertAll(Timberinfo... timberinfos);

    @Delete
    void delete(Timberinfo timberinfo);
}
