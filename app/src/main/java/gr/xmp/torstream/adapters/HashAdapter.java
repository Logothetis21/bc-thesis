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
import gr.xmp.torstream.models.Hash;

public class HashAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private ArrayList<Hash> list; //
    private OnItemClickListener listener;


    public HashAdapter(ArrayList<Hash> list, Context context,OnItemClickListener listener){
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new default_holder(LayoutInflater.from(context).inflate(R.layout.hash_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof default_holder) ((default_holder) holder).bind(list.get(position));
    }

    @Override public int getItemCount() {return list == null ? 0 : list.size();}
    class default_holder extends RecyclerView.ViewHolder implements Serializable, View.OnClickListener {

        TextView name,title;
        public default_holder(@NonNull View i) {
            super(i); name = i.findViewById(R.id.name);
            title = i.findViewById(R.id.title);
            i.setOnClickListener(this);
        }
        void bind(Hash h){
            name.setText(h.name);
            title.setText(h.title);
            itemView.setTag(h);
        }
        @Override  public void onClick(View v) { if (listener != null) listener.onItemClick((Hash) itemView.getTag()); }
    }

    public interface OnItemClickListener {void onItemClick(Hash h);}



}
