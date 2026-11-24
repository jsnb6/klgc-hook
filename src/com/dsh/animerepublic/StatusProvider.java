package com.dsh.animerepublic;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** 宿主进程（我们的 hook 在那边跑）把运行状态回写给模块自身，供状态页展示。 */
public class StatusProvider extends ContentProvider {

    static final String AUTHORITY = "com.klgc.animerepublic.status";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        try {
            if ("report".equals(method) && extras != null && getContext() != null) {
                SharedPreferences sp = getContext().getSharedPreferences("klgc_rt", 0);
                sp.edit()
                        .putString("fw", extras.getString("fw", ""))
                        .putString("fv", extras.getString("fv", ""))
                        .putLong("fvc", extras.getLong("fvc", 0L))
                        .putInt("api", extras.getInt("api", 0))
                        .putLong("props", extras.getLong("props", 0L))
                        .putString("host", extras.getString("host", ""))
                        .putLong("ts", System.currentTimeMillis())
                        .apply();
                out.putBoolean("ok", true);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
