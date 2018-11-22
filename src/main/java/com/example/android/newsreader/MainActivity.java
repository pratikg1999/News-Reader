package com.example.android.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles;
    ArrayList<String> content;
    ListView listView;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("start", "onCreate: Program started");
        titles = new ArrayList<>();
        content = new ArrayList<>();
        titles.add("test");
        content.add("this is the content of the test");
        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        articleDB = openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/askstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent  = new Intent(MainActivity.this, article.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });

        updateListView();
    }

    public  void updateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
                Log.i("title", "updateListView: "+titles.get(titles.size()-1));
                Log.i("content", "updateListView: "+content.get(content.size()-1));
            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream stream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(stream);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray array = new JSONArray(result);
                int numberofItems = 2;
                if (array.length() < numberofItems) {
                    numberofItems = array.length();
                }
                articleDB.execSQL("DELETE FROM articles");
                Log.i("delete table", "doInBackground: delted table");
                int working =0;
                for (int i = 0; ; i++) {
                    String articleId = array.getString(i);
                    URL url1 = new URL("https://hacker-news.firebaseio.com/v0/item/" + i + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url1.openConnection();
                    stream = urlConnection.getInputStream();
                    reader = new InputStreamReader(stream);
                    data = reader.read();
                    String info = "";
                    while (data != -1) {
                        char current = (char) data;
                        info += current;
                        data = reader.read();
                    }
                    try {
                        JSONObject jsonObject = new JSONObject(info);
                        if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                            working++;
                            if(working >numberofItems){
                                break;
                            }
                            String articleTitle = jsonObject.getString("title");
                            String articleUrl = jsonObject.getString("url");

                            url = new URL(articleUrl);
                            urlConnection = (HttpURLConnection)url.openConnection();
                            stream = urlConnection.getInputStream();
                            reader = new InputStreamReader(stream);
                            data = reader.read();
                            String articleContent = "";
                            while (data!=-1){
                                char current = (char)data;
                                articleContent += current;
                                data = reader.read();
                            }
                            //Log.i("Html", articleContent);
                            String sql = "INSERT INTO articles(articleId, title, content) VALUES (?,?,?)";
                            SQLiteStatement statement = articleDB.compileStatement(sql);
                            statement.bindString(1,articleId);
                            statement.bindString(2, articleTitle);
                            statement.bindString(3, articleContent);
                            statement.execute();
                            Log.i("articletitle", "doInBackground: "+articleTitle);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                Log.i("Url result", result);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}