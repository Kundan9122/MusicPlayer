package com.firekernel.musicplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.playback.MediaBrowserProvider;
import com.firekernel.musicplayer.ui.adapter.MediaListAdapter;
import com.firekernel.musicplayer.utils.FireLog;

import java.util.ArrayList;
import java.util.List;

public class MediaListFragment extends Fragment {
    public static final String TAG = FireLog.makeLogTag(MediaListFragment.class);
    private static final String TITLE = "title";
    private static final String EXTRA_MEDIA_ID = "media_id";

    private MediaListAdapter adapter;
    private final MediaBrowserCompat.SubscriptionCallback subscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            try {
                FireLog.d(TAG, "(++) onChildrenLoaded, parentId=" + parentId + "  count=" + children.size());
                loadView(children);
            } catch (Throwable t) {
                FireLog.e(TAG, "Error onChildrenLoaded", t);
            }
        }

        @Override
        public void onError(@NonNull String id) {
            FireLog.e(TAG, "(++) onError, id=" + id);
            Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
        }
    };
    private List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
    private MediaBrowserProvider mediaBrowserProvider;
    private String title;
    private String mediaId;

    public MediaListFragment() {
        // Required empty public constructor
    }

    public static MediaListFragment newInstance(String title, String mediaId) {
        MediaListFragment fragment = new MediaListFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(EXTRA_MEDIA_ID, mediaId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        FireLog.d(TAG, "(++) onAttach");

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
        setHasOptionsMenu(true);
        title = getArguments().getString(TITLE);
        mediaId = getArguments().getString(EXTRA_MEDIA_ID);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FireLog.d(TAG, "(++) onCreateView");
        View view = inflater.inflate(R.layout.fragment_media_list, container, false);

        adapter = new MediaListAdapter(getContext(), mediaItems);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext().getApplicationContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
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
        super.onDetach();
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        if (isDetached()) {
            return;
        }
        if (mediaId == null) {
            mediaId = mediaBrowserProvider.getMediaBrowser().getRoot();
        }

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mediaBrowserProvider.getMediaBrowser().unsubscribe(mediaId);

        mediaBrowserProvider.getMediaBrowser().subscribe(mediaId, subscriptionCallback);
    }

    private void loadView(List<MediaBrowserCompat.MediaItem> mediaItems) {
        // not taking care of recyclerView visibility
        if (mediaItems == null || mediaItems.size() == 0) {
            getView().findViewById(R.id.error_view).setVisibility(View.VISIBLE);
        } else {
            getView().findViewById(R.id.error_view).setVisibility(View.GONE);
        }
        adapter.refreshData(mediaItems);
    }

    public interface OnMediaItemSelectedListener {
        void onMediaItemSelected(MediaBrowserCompat.MediaItem item);
    }

}
