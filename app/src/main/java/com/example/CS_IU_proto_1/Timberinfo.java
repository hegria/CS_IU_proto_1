package com.example.CS_IU_proto_1;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Timberinfo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name="filename")
    public String filename;

    @ColumnInfo(name="speice")
    public String spiece;

    @ColumnInfo(name="date")
    public String date;

    @ColumnInfo(name="location")
    public String location;

    @ColumnInfo(name="space")
    public String space;

    @ColumnInfo(name="longivity")
    public float longivity;

    @ColumnInfo(name="volumn")
    public float volumn;

    @ColumnInfo(name="count")
    public int count;

    @ColumnInfo(name="human")
    public String human;

    @ColumnInfo(name="tag")
    public String tag;

}
