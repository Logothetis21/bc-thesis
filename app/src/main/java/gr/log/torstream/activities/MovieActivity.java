package gr.log.torstream.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.ConcatAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import gr.log.torstream.adapters.HashAdapter;
import gr.log.torstream.adapters.MovieDashboardAdapter;
import gr.log.torstream.databinding.ActivityMovieDashboardBinding;
import gr.log.torstream.models.Hash;
import gr.log.torstream.models.Movie;
import gr.log.torstream.models.String_;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MovieActivity extends AppCompatActivity {
    private final String TAG = "MovieActivity";

    private static String[] trackers = new String[]{
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.demonii.com:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://explodie.org:6969/announce",
            "udp://tracker.0x7c0.com:6969/announce",
            "udp://tracker-udp.gbitt.info:80/announce",
            "udp://opentracker.io:6969/announce",
            "https://tracker.tamersunion.org:443/announce",
            "udp://tracker.dump.cl:6969/announce",
            "udp://tracker.bittor.pw:1337/announce",
            "udp://retracker01-msk-virt.corbina.net:80/announce",
            "udp://open.tracker.cl:1337/announce",
            "udp://open.free-tracker.ga:6969/announce",
            "udp://new-line.net:6969/announce",
            "udp://leet-tracker.moe:1337/announce",
            "udp://isk.richardsw.club:6969/announce",
            "udp://exodus.desync.com:6969/announce",
            "http://tracker.ipv6tracker.org:80/announce",
            "http://tr.kxmp.cf:80/announce"
    };

    private ActivityMovieDashboardBinding binding;
    private ArrayList<Hash> hash_list = new ArrayList<>();
    private MovieDashboardAdapter movie_details_adapter;
    private HashAdapter hashes_adapter;
    private Movie movie;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        binding = ActivityMovieDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String imdb = (String) getIntent().getSerializableExtra("movie_imbd_code");
        movie = (Movie) getIntent().getSerializableExtra("movie_object");

        movie_details_adapter = new MovieDashboardAdapter(movie, this);
        hashes_adapter = new HashAdapter(hash_list, this, h -> {
            Intent i = new Intent(this , StreamingActivity.class);
            i.putExtra("magnet" , fetch_magnet_link(h.hash));
            i.putExtra("fileIdx" , h.fileIndx);
            i.putExtra("logo_url" , ((Movie) getIntent().getSerializableExtra("movie_object")).logo_img);
            startActivity(i);
            overridePendingTransition(0,0);
        });

        fetch_background(imdb);

        new Thread(() ->{
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://caching.stremio.net/publicdomainmovies.now.sh/stream/movie/"+imdb+".json")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    JSONArray streams = new JSONObject(response.body().string()).getJSONArray("streams");
                    ArrayList<Hash> var_9273612 = new ArrayList<>();
                    for(int i=0; i<streams.length(); i++){
                        Hash var_72631263 = new Hash();
                        JSONObject var_ob2321 = streams.getJSONObject(i);
                        if(var_ob2321.isNull("fileIdx")) continue;
                        var_72631263.name = var_ob2321.getString("name");
                        var_72631263.title = var_ob2321.getString("title");
                        var_72631263.hash = var_ob2321.getString("infoHash");
                        var_72631263.fileIndx = var_ob2321.getInt("fileIdx");
                        var_9273612.add(var_72631263);
                    }
                    runOnUiThread(() -> {
                        hash_list.addAll(var_9273612);
                        hashes_adapter.notifyDataSetChanged();
                    });
                } else throw new IOException("Unexpected response code. " + response);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error on fetch: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

        }).start();
    }

    private void fetch_background(String id){
        String link = "https://v3-cinemeta.strem.io/meta/movie/"+id+".json";
        new Thread(() ->{
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(link)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    JSONObject obj = new JSONObject(response.body().string()).getJSONObject("meta");
                    movie.background_img = obj.getString("background");
                    movie.logo_img = obj.getString("logo");
                    ArrayList<String_> arr = movie.runtime_year_imdb;
                    arr.add(new String_(obj.getString("year") , false));
                } else throw new IOException("Unexpected response code. " + response);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error on fetch: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            runOnUiThread(() ->{
                ConcatAdapter concatAdapter = new ConcatAdapter(movie_details_adapter, hashes_adapter);
                binding.recycler.setAdapter(concatAdapter);
            });
        }).start();
    }

    public static String fetch_magnet_link(String hash) {
        StringBuilder link = new StringBuilder("magnet:?xt=urn:btih:" + hash);
        for (String tracker : trackers) link.append("&tr=").append(tracker);
        return link.toString();
    }
}
