package com.firekernel.musicplayer.ui.adapter;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v4.media.MediaBrowserCompat;
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
import com.firekernel.musicplayer.model.Category;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MyViewHolder> {
    private static final String TAG = FireLog.makeLogTag(SearchAdapter.class);
    private Context context;
    private List<MediaItemWrapper> mediaItemWrappers = new ArrayList<>();
    private ItemClickListener itemClickListener;
    private FirePopupMenuSelectedListener popupMenuSelectedListener;

    public SearchAdapter(Context context) {
        this.context = context;
        if (context instanceof ItemClickListener) {
            itemClickListener = (ItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ItemClickListener");
        }
        if (context instanceof FirePopupMenuSelectedListener) {
            popupMenuSelectedListener = (FirePopupMenuSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement FirePopupMenuSelectedListener");
        }
    }

    @Override
    public int getItemCount() {
        return mediaItemWrappers.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mediaItemWrappers.get(position).getCategory() == Category.TRACK)
            return Category.TRACK;
        if (mediaItemWrappers.get(position).getCategory() == Category.ALBUM)
            return Category.ALBUM;
        if (mediaItemWrappers.get(position).getCategory() == Category.ARTIST)
            return Category.ARTIST;
        return Category.NONE;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case Category.TRACK:
            case Category.ALBUM:
            case Category.ARTIST:
            default:
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_row, parent, false);
                return new MyViewHolder(itemView);
            case Category.NONE:
                View itemView2 = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
                return new MyViewHolder(itemView2);
        }
    }

    @Override
    public void onBindViewHolder(final MyViewHolder myViewHolder, int position) {
        MediaItemWrapper mediaItemWrapper = mediaItemWrappers.get(position);
        MediaBrowserCompat.MediaItem mediaItem = mediaItemWrapper.getMediaItem();
        switch (getItemViewType(position)) {
            case Category.TRACK:
                myViewHolder.title.setText(mediaItem.getDescription().getTitle());
                myViewHolder.detail.setText(mediaItem.getDescription().getSubtitle());
                myViewHolder.popupMenuBtn.setVisibility(View.VISIBLE);
                setOnItemClickListener(myViewHolder, mediaItemWrapper);
                setOnPopupMenuListener(myViewHolder, mediaItem);
                ImageHelper.loadArt(context, myViewHolder.albumArt, mediaItem.getDescription());
                break;
            case Category.ALBUM:
                myViewHolder.title.setText(mediaItem.getDescription().getTitle());
                myViewHolder.detail.setText(mediaItem.getDescription().getSubtitle());
                myViewHolder.popupMenuBtn.setVisibility(View.GONE);
                setOnItemClickListener(myViewHolder, mediaItemWrapper);
                ImageHelper.loadArt(context, myViewHolder.albumArt, mediaItem.getDescription());
                break;
            case Category.ARTIST:
                myViewHolder.title.setText(mediaItem.getDescription().getTitle());
                myViewHolder.popupMenuBtn.setVisibility(View.GONE);
                setOnItemClickListener(myViewHolder, mediaItemWrapper);
                ImageHelper.loadArt(context, myViewHolder.albumArt, mediaItem.getDescription());
                break;
            case Category.NONE:
                myViewHolder.sectionHeader.setTextColor(context.getResources().getColor(R.color.white));
                myViewHolder.sectionHeader.setText(mediaItemWrapper.getTypeTitle());
        }
    }

    public void refreshData(List<MediaItemWrapper> searchResults) {
        this.mediaItemWrappers.clear();
        this.mediaItemWrappers.addAll(searchResults);
        notifyDataSetChanged();
    }

    private void setOnItemClickListener(MyViewHolder viewHolder, final MediaItemWrapper mediaItemWrapper) {
        viewHolder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClick(mediaItemWrapper);
            }
        });
    }

    private void setOnPopupMenuListener(MyViewHolder viewHolder, final MediaBrowserCompat.MediaItem mediaItem) {
        viewHolder.popupMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu menu = new PopupMenu(context, v);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
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

    public interface ItemClickListener {
        void onItemClick(MediaItemWrapper mediaItemWrapper);
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout mainLayout;
        private ImageView albumArt;
        private TextView title;
        private TextView detail;
        private ImageButton popupMenuBtn;
        private TextView sectionHeader;

        MyViewHolder(View view) {
            super(view);
            mainLayout = (ConstraintLayout) view.findViewById(R.id.mainLayout);
            albumArt = (ImageView) view.findViewById(R.id.albumArt);
            title = (TextView) view.findViewById(R.id.title);
            detail = (TextView) view.findViewById(R.id.detail);
            popupMenuBtn = (ImageButton) view.findViewById(R.id.popupMenuBtn);
            sectionHeader = (TextView) view.findViewById(android.R.id.text1);
        }
    }

}





