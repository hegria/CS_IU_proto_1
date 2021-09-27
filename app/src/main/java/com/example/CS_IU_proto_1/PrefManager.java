package com.example.CS_IU_proto_1;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "welcome";
    private static final String Key1 = "GuideLine1ForFirstTime";
    private static final String Key2 = "GuideLine2ForFirstTime";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch1(boolean isFirstTime) {
        editor.putBoolean(Key1, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch1() {
        return pref.getBoolean(Key1, true);
    }

    public void setFirstTimeLaunch2(boolean isFirstTime) {
        editor.putBoolean(Key2, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch2() {
        return pref.getBoolean(Key2, true);
    }
}