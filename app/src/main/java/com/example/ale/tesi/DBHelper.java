package com.example.ale.tesi;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
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

public class DBHelper extends SQLiteOpenHelper{

    /*
        Classe che serve per costruire il database al primo avvio dell'applicazione. Viene creata una
        tabella con solamente 2 colonne: la prima definita dall'attributo "_id" che identifica l'id
        dell'opera; la seconda è definita dall'attributo "data" che contiente il json formato da
        tutte le informazioni presenti/necessarie riguardante la singola opera. Viene fatto un
        parsing del json salvato in memoria con il nome dati_opere e vengono ricercate le
        informazioni necessarie per creare la tabella.
     */

    public static final String FIELD_ID="_id";
    public static final String FIELD_DATA="data";
    public static final String NAME = "name";
    public static final String TBL_NAME="artworks1";
    public static final String DBNAME="MOBILE_RECOGNIZER_DATABASE1";
    private Context context;

    public DBHelper(Context context){

        super(context, DBNAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String exec = "DROP TABLE IF EXISTS '" + TBL_NAME + "'";

        db.execSQL(exec);

        exec ="CREATE TABLE "+ TBL_NAME +
                " ( _id TEXT," +
                FIELD_DATA + " TEXT," + NAME + " TEXT)";

        db.execSQL(exec);

        try {
            JSONArray jsonArray = new JSONArray(inputJson());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                ContentValues cv = new ContentValues();
                cv.put(DBHelper.FIELD_ID, jsonObject.getString("uri"));
                cv.put(DBHelper.FIELD_DATA, jsonObject.getString("data"));
                Log.d("Database", "Yeah1");
                JSONObject jsonObjectData = new JSONObject(jsonObject.getString("data"));
                Log.d("Database", "Yeah2");
                Log.d("Database", jsonObjectData.toString());
                Log.d("Database", jsonObjectData.getString("title"));
                cv.put(DBHelper.NAME, jsonObjectData.getString("title"));

                try
                {
                    db.insert(DBHelper.TBL_NAME, null,cv);
                }
                catch (SQLiteException sqle)
                {

                }

            }
        } catch (JSONException e) {
            Log.d("Database", e.toString());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public String inputJson(){
        String msg = "";
        InputStreamReader inputStreamReader = new InputStreamReader(context.getResources().openRawResource(R.raw.dati_opere));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                msg += line;
            }
        } catch (IOException e) {
            Toast.makeText(context, "Errore di Lettura", Toast.LENGTH_SHORT).show();
        }
        return msg;
    }

}
