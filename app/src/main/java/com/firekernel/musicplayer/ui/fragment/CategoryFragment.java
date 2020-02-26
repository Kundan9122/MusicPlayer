package com.firekernel.musicplayer.ui.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firekernel.musicplayer.FireRecyclerItemClickListener;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.playback.MediaBrowserProvider;
import com.firekernel.musicplayer.ui.adapter.MainAdapter;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.FireUtils;

import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends BaseFragment {
    public static final String TAG = FireLog.makeLogTag(CategoryFragment.class);
    private static final String TITLE = "title";
    private static final String MEDIA_ID = "media_id";
    private RecyclerView recyclerView;
    private MainAdapter adapter;
    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    try {
                        FireLog.d(TAG, "(++) onChildrenLoaded, parentId=" + parentId + "  count=" + children.size());
                        adapter.refreshData(children);
                    } catch (Throwable t) {
                        FireLog.e(TAG, "Error on childrenloaded", t);
                    }
                }

                @Override
                public void onError(@NonNull String id) {
                    FireLog.e(TAG, "(++) onError, id=" + id);
                    Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                }
            };
    private String title;
    private String mediaId;
    private MediaBrowserProvider mediaBrowserProvider;
    private OnCategorySelectedListener onCategorySelectedListener;
    private List<MediaBrowserCompat.MediaItem> medias = new ArrayList<>();

    public CategoryFragment() {
        // Required empty public constructor
    }

    public static CategoryFragment newInstance(String title, String mediaId) {
        CategoryFragment fragment = new CategoryFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        FireLog.d(TAG, "(++) onAttach");
        if (context instanceof OnCategorySelectedListener) {
            onCategorySelectedListener = (OnCategorySelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCategorySelectedListener");
        }

        if (context instanceof MediaBrowserProvider) {
            mediaBrowserProvider = (MediaBrowserProvider) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MediaBrowserProvider");
        }
        super.onAttach(context);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        FireLog.d(TAG, "(++) onCreate");
        setHasOptionsMenu(true);
        title = getArguments().getString(TITLE);
        mediaId = getArguments().getString(MEDIA_ID);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FireLog.d(TAG, "(++) onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        adapter = new MainAdapter(getActivity(), medias);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity().getApplicationContext(), 3));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new FireRecyclerItemClickListener(getActivity(),
                recyclerView, new FireRecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                FireUtils.preventDoubleClick();
                onCategorySelectedListener.onCategorySelected(adapter.getItem(position));
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        // setting title of activity when user returns to CategoryFragment form MediaListFragment
        getActivity().setTitle(title);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // fetch browsing information to fill the listview:
        MediaBrowserCompat mediaBrowser = mediaBrowserProvider.getMediaBrowser();
        FireLog.d(TAG, "(++) onStart, mediaId=" + mediaId + "  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStop() {
        super.onStop();
        FireLog.d(TAG, "(++) onStop");
        MediaBrowserCompat mediaBrowser = mediaBrowserProvider.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mediaId != null) {
            mediaBrowser.unsubscribe(mediaId);
        }
    }

    @Override
    public void onDetach() {
        FireLog.d(TAG, "(++) onDetach");
        onCategorySelectedListener = null;
        super.onDetach();
    }


    public void onConnected() {
        if (isDetached()) {
            return;
        }
        if (mediaId == null) {
            mediaId = mediaBrowserProvider.getMediaBrowser().getRoot();
        }
        mediaBrowserProvider.getMediaBrowser().unsubscribe(mediaId);
        mediaBrowserProvider.getMediaBrowser().subscribe(mediaId, mSubscriptionCallback);
    }


    public interface OnCategorySelectedListener {
        void onCategorySelected(MediaBrowserCompat.MediaItem mediaItem);
    }

}
