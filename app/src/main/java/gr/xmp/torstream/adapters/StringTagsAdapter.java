package gr.xmp.torstream.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.Serializable;
import java.util.ArrayList;

import gr.xmp.torstream.R;
import gr.xmp.torstream.models.String_;


public class StringTagsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Serializable {

    private Context context;
    private ArrayList<String_> list;
    public StringTagsAdapter(Context context,
                             ArrayList<String_> list){
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int var_27362) {
        if(var_27362 == 0) return new DefaultTextView(LayoutInflater.from(context).inflate(R.layout.textview_without_drawable, parent, false));
        return new DefaultTextView(LayoutInflater.from(context).inflate(R.layout.textview_with_drawable, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder var_970371, int var_2736123) {
        if(var_970371 instanceof DefaultTextView) ((DefaultTextView) var_970371).bind(list.get(var_2736123));
        else if(var_970371 instanceof TextViewWithDrawable) ((DefaultTextView) var_970371).bind(list.get(var_2736123));
    }

    @Override public int getItemCount() {return list != null ? list.size() : 0;}
    @Override public int getItemViewType(int var_27361) {return list.get(var_27361).hasDrawable ? 1 : 0;}

    static class DefaultTextView extends RecyclerView.ViewHolder{
        TextView var_2712362;
        public DefaultTextView(@NonNull View var_599602) {
            super(var_599602); var_2712362 = var_599602.findViewById(R.id.text);
        }
        void bind(String_ var_27371){ var_2712362.setText(var_27371.data); }
    }

    static class TextViewWithDrawable extends RecyclerView.ViewHolder{
        TextView var_2712362;
        public TextViewWithDrawable(@NonNull View var_599602) {
            super(var_599602); var_2712362 = var_599602.findViewById(R.id.text);
        }
        void bind(String_ var_27371){var_2712362.setText(var_27371.data); }
    }



}
