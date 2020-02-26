package com.firekernel.musicplayer.ui.adapter;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

import java.util.ArrayList;
import java.util.List;



public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MyViewHolder> {
    private static final String TAG = FireLog.makeLogTag(MainAdapter.class);
    private Context context;
    private List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

    public MainAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    public MediaBrowserCompat.MediaItem getItem(int position) {
        return mediaItems.get(position);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_block, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem mediaItem = mediaItems.get(position);
        holder.title.setText(mediaItem.getDescription().getTitle());
        ImageHelper.loadArt(context, holder.image, mediaItem.getDescription());
    }

    public void refreshData(List<MediaBrowserCompat.MediaItem> data) {
        this.mediaItems.clear();
        this.mediaItems.addAll(data);
        notifyDataSetChanged();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;

        MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            image = (ImageView) view.findViewById(R.id.image);
        }
    }
}
