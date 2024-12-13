package gr.log.torstream.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import gr.log.torstream.databinding.ActivityMainBinding;

import gr.log.torstream.models.Movie;
import gr.log.torstream.models.String_;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import gr.log.torstream.adapters.MovieLabelAdapter;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = "APP";
    private ArrayList<Movie> list = new ArrayList<>();
    private MovieLabelAdapter adapter;

    ActivityMainBinding binding;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new MovieLabelAdapter(this, list, m -> {
            Intent i = new Intent(this , MovieActivity.class);
            i.putExtra("movie_object" , m);
            i.putExtra("movie_imbd_code" , m.imbd_code);
            startActivity(i);
            overridePendingTransition(0,0);
        });
        binding.metas.setLayoutManager(FlexLayoutManagerFunc());
        binding.metas.setAdapter(adapter);
        fetch_meta();
    }


    void fetch_meta(){
        new Thread(() ->{
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://caching.stremio.net/publicdomainmovies.now.sh/catalog/movie/publicdomainmovies.json")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    JSONArray metas = new JSONObject(response.body().string()).getJSONArray("metas"); //gg
                    for(int i=0; i<metas.length(); i++){
                        Movie movie_Object = new Movie();
                        JSONObject movie = metas.getJSONObject(i);
                        movie_Object.name = !movie.isNull("name") ? movie.getString("name") : "";
                        movie_Object.imbd_code = !movie.isNull("id") ? movie.getString("id") : "";
                        movie_Object.poster_img = !movie.isNull("poster") ? movie.getString("poster") : "";
                        movie_Object.background_img = !movie.isNull("background") ? movie.getString("background") : "";
                        movie_Object.logo_img = !movie.isNull("logo") ? movie.getString("logo") : "";
                        movie_Object.description = !movie.isNull("description") ? movie.getString("description") : "";
                        movie_Object.runtime_year_imdb = fetch_runtime_year_imdb(movie);
                        movie_Object.genres = fetch_genre(movie);
                        movie_Object.director = fetch_director(movie);
                        movie_Object.cast = fetch_cast(movie);
                        movie_Object.writer = fetch_writer(movie);
                        list.add(movie_Object);
                    }
                    runOnUiThread(() -> { adapter.notifyDataSetChanged(); });
                } else throw new IOException("Unexpected response code. " + response);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error on fetch: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                });

            }
        }).start();
    }
    ArrayList<String_> fetch_runtime_year_imdb(JSONObject movie) throws JSONException {
        ArrayList<String_> var_23762 = new ArrayList<>();
        if(!movie.isNull("runtime")) var_23762.add(new String_(movie.getString("runtime") , false));
        if(!movie.isNull("year")) var_23762.add(new String_(movie.getString("year") , false));
        if(!movie.isNull("imdbRating")) var_23762.add(new String_(movie.getString("imdbRating") , true));
        return var_23762;
    }
    ArrayList<String_> fetch_genre(JSONObject movie) throws JSONException {
        ArrayList<String_> var_23762 = new ArrayList<>();
        if(!movie.isNull("genre")){
            JSONArray genres = movie.getJSONArray("genre");
            for(int i=0; i<genres.length(); i++)
                var_23762.add(new String_(genres.getString(i) , false));
        }
        return var_23762;
    }
    ArrayList<String_> fetch_director(JSONObject movie) throws JSONException {
        ArrayList<String_> var_23762 = new ArrayList<>();
        if(!movie.isNull("director")){
            JSONArray genres = movie.getJSONArray("director");
            for(int i=0; i<genres.length(); i++)
                var_23762.add(new String_(genres.getString(i) , false));
        }
        return var_23762;
    }
    ArrayList<String_> fetch_cast(JSONObject movie) throws JSONException {
        ArrayList<String_> var_23762 = new ArrayList<>();
        if(!movie.isNull("cast")){
            JSONArray genres = movie.getJSONArray("cast");
            for(int i=0; i<genres.length(); i++)
                var_23762.add(new String_(genres.getString(i) , false));
        }
        return var_23762;
    }
    ArrayList<String_> fetch_writer(JSONObject movie) throws JSONException {
        ArrayList<String_> var_23762 = new ArrayList<>();
        if(!movie.isNull("writer")){
            JSONArray genres = movie.getJSONArray("writer");
            for(int i=0; i<genres.length(); i++)
                var_23762.add(new String_(genres.getString(i) , false));
        }
        return var_23762;
    }

    FlexboxLayoutManager FlexLayoutManagerFunc(){
        FlexboxLayoutManager var_2u482136l = new FlexboxLayoutManager(this);
        var_2u482136l.setFlexDirection(FlexDirection.ROW);
        var_2u482136l.setJustifyContent(JustifyContent.CENTER);
        var_2u482136l.setItemPrefetchEnabled(true);
        var_2u482136l.setFlexWrap(FlexWrap.WRAP);
        var_2u482136l.setRecycleChildrenOnDetach(true);
        return var_2u482136l;
    }

}