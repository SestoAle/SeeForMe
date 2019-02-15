package com.example.ale.tesi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Ale on 18/07/16.
 */

public class DBManager {

    /*
        Classe che serve per gestire il database di tipo DBHepler creato al primo avvio
        dell'applicazione. Viene implementato un solo metodo chiamato quey (in quanto non è
        necessario modificare il database contente le informazioni delle opere dinamicamente). Il
        metodo verrà usato dalla classe MainActivity per recuperare le informazioni: viene chiesto
        in ingresso una stringa, e viene ricercata all'interno del database la riga che ha come
        _id la stringa passata.
     */

    private DBHelper dbHelper;

    public DBManager(Context context){
        dbHelper = new DBHelper(context);
    }

    public Cursor query(String id)
    {
        Cursor crs=null;
        try
        {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            crs=db.query(DBHelper.TBL_NAME, null, "_id = " + "'" + id + "'", null, null, null, null, null);
        }
        catch(SQLiteException sqle)
        {
            return crs;
        }
        return crs;
    }

    public Cursor queryName(String id)
    {
        Cursor crs=null;
        try
        {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            crs=db.query(DBHelper.TBL_NAME, null, "_id = " + "'" + id + "'", null, null, null, null, null);
        }
        catch(SQLiteException sqle)
        {
            return crs;
        }
        return crs;
    }

}
