package gr.xmp.torstream.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.io.Serializable;

import gr.xmp.torstream.R;
import gr.xmp.torstream.activities.MainActivity;
import gr.xmp.torstream.models.Movie;

public class MovieDashboardAdapter extends RecyclerView.Adapter<MovieDashboardAdapter.default_holder> {
    private Context context;
    private Movie movieObj; // only one object

    public MovieDashboardAdapter(Movie movieObj, Context context){
        this.context = context;
        this.movieObj = movieObj;
    }

    @NonNull @Override public MovieDashboardAdapter.default_holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new default_holder(LayoutInflater.from(context).inflate(R.layout.movie_dashboard, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MovieDashboardAdapter.default_holder holder, int position) {
        if(holder instanceof default_holder) ((default_holder) holder).bind(movieObj);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override public int getItemCount() {return movieObj == null ? 0 : 1;}

    class default_holder extends RecyclerView.ViewHolder implements Serializable{

        ImageView background, logo;
        ImageButton back;
        RecyclerView ryi,genre,director,cast,writer;
        TextView description;

        TextView var_01,var_02,var_03;
        public default_holder(@NonNull View i) {
            super(i);
            background = i.findViewById(R.id.movie_background_adapter);
            back = i.findViewById(R.id.goBack);
            logo = i.findViewById(R.id.logo_img);
            ryi  = i.findViewById(R.id.year_runtime_imdb);
            description = i.findViewById(R.id.description);
            genre = i.findViewById(R.id.genre);
            director = i.findViewById(R.id.director);
            cast = i.findViewById(R.id.cast);
            writer = i.findViewById(R.id.writer);
            var_01 = i.findViewById(R.id.var_01);
            var_02 = i.findViewById(R.id.var_02);
            var_03 = i.findViewById(R.id.var_03);
        }
        public void bind(Movie m){
            Glide.with(context).load(m.background_img).into(background);
            Glide.with(context).load(m.logo_img).into(logo);
            ryi.setLayoutManager(var_635481c());
            ryi.setAdapter(new StringTagsAdapter(context, m.runtime_year_imdb));
            genre.setLayoutManager(var_635481c());
            genre.setAdapter(new StringTagsAdapter(context, m.genres));
            if(m.director != null && !m.director.isEmpty()){
                director.setLayoutManager(var_635481l());
                director.setAdapter(new StringTagsAdapter(context, m.director));
            } else var_01.setVisibility(View.GONE);

            if(m.cast != null && !m.cast.isEmpty()){
                cast.setLayoutManager(var_635481l());
                cast.setAdapter(new StringTagsAdapter(context, m.cast));
            } else var_02.setVisibility(View.GONE);

            if(m.writer != null && !m.writer.isEmpty()){
                writer.setLayoutManager(var_635481l());
                writer.setAdapter(new StringTagsAdapter(context, m.writer));
            } else var_03.setVisibility(View.GONE);


            description.setText(m.description);
            back.setOnClickListener(v ->{
                if(context instanceof Activity){
                    Intent i = new Intent(context , MainActivity.class);
                    ((Activity) context).startActivity(i);
                    ((Activity) context).overridePendingTransition(0, 0);
                }
            });
        }
    }

    FlexboxLayoutManager var_635481c(){
        FlexboxLayoutManager var_2u482136l = new FlexboxLayoutManager(context);
        var_2u482136l.setFlexDirection(FlexDirection.ROW);
        var_2u482136l.setJustifyContent(JustifyContent.SPACE_EVENLY);
        var_2u482136l.setItemPrefetchEnabled(true);
        var_2u482136l.setFlexWrap(FlexWrap.WRAP);
        var_2u482136l.setRecycleChildrenOnDetach(true);
        return var_2u482136l;
    }
    FlexboxLayoutManager var_635481l(){
        FlexboxLayoutManager var_2u482136l = new FlexboxLayoutManager(context);
        var_2u482136l.setFlexDirection(FlexDirection.ROW);
        var_2u482136l.setJustifyContent(JustifyContent.FLEX_START);
        var_2u482136l.setItemPrefetchEnabled(true);
        var_2u482136l.setFlexWrap(FlexWrap.WRAP);
        var_2u482136l.setRecycleChildrenOnDetach(true);
        return var_2u482136l;
    }



}
