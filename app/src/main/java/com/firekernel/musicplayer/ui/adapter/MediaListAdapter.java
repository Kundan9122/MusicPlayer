package com.firekernel.musicplayer.ui.adapter;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.firekernel.musicplayer.FirePopupMenuSelectedListener;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.ui.fragment.MediaListFragment;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for media list
 */

public class MediaListAdapter extends RecyclerView.Adapter<MediaListAdapter.MyViewHolder> {
    private static final String TAG = FireLog.makeLogTag(MediaListAdapter.class);
    private Context context;
    private List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
    private FirePopupMenuSelectedListener popupMenuSelectedListener;
    private MediaListFragment.OnMediaItemSelectedListener onMediaItemSelectedListener;

    public MediaListAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
        if (context instanceof MediaListFragment.OnMediaItemSelectedListener) {
            onMediaItemSelectedListener = (MediaListFragment.OnMediaItemSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMediaItemSelectedListener");
        }
        if (context instanceof FirePopupMenuSelectedListener) {
            popupMenuSelectedListener = (FirePopupMenuSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FirePopupMenuSelectedListener");
        }
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem mediaItem = mediaItems.get(position);
        MediaDescriptionCompat description = mediaItem.getDescription();
        holder.title.setText(description.getTitle());
        holder.detail.setText(description.getSubtitle());
        ImageHelper.loadArt(context, holder.albumArt, description);
        setItemClickListener(holder, mediaItem);
        setOnPopupMenuListener(holder, mediaItem);
    }

    public void refreshData(List<MediaBrowserCompat.MediaItem> data) {
        mediaItems.clear();
        mediaItems.addAll(data);
        notifyDataSetChanged();
    }

    private void setItemClickListener(MyViewHolder myViewHolder, final MediaBrowserCompat.MediaItem mediaItem) {
        myViewHolder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMediaItemSelectedListener.onMediaItemSelected(mediaItem);
            }
        });
    }

    private void setOnPopupMenuListener(MyViewHolder myViewHolder, final MediaBrowserCompat.MediaItem mediaItem) {
        myViewHolder.popupMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(context, v);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.popup_song_play:
                                popupMenuSelectedListener.onPlaySelected(mediaItem);
                                break;
                            case R.id.popup_share:
                                popupMenuSelectedListener.onShareSelected(mediaItem);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
                menu.inflate(R.menu.menu_popup);
                menu.show();
            }
        });
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout mainLayout;
        private ImageView albumArt;
        private TextView title;
        private TextView detail;
        private ImageButton popupMenuBtn;

        MyViewHolder(View view) {
            super(view);
            mainLayout = (ConstraintLayout) view.findViewById(R.id.mainLayout);
            albumArt = (ImageView) view.findViewById(R.id.albumArt);
            title = (TextView) view.findViewById(R.id.title);
            detail = (TextView) view.findViewById(R.id.detail);
            popupMenuBtn = (ImageButton) view.findViewById(R.id.popupMenuBtn);
        }
    }
}
