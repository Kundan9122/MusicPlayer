package com.firekernel.musicplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.playback.MediaBrowserProvider;
import com.firekernel.musicplayer.source.SearchResultProvider;
import com.firekernel.musicplayer.ui.adapter.SearchAdapter;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements SearchView.OnQueryTextListener, View.OnTouchListener {
    public final static String TAG = FireLog.makeLogTag(SearchFragment.class);
    private SearchView searchView;
    private InputMethodManager mImm;
    private String query = "";
    private SearchAdapter searchAdapter;
    private final SearchResultProvider.SearchCallback searchCallback = new SearchResultProvider.SearchCallback() {
        @Override
        public void onSearchResult(@NonNull String query, Bundle bundle, @NonNull List<MediaItemWrapper> items) {
            FireLog.d(TAG, "(++) onSearchResult: query=" + query + "items size=" + items.size());
            if (!isDetached())
                searchAdapter.refreshData(items);
        }

        @Override
        public void onError(@NonNull String query, Bundle bundle) {
            FireLog.e(TAG, "(++) onError");
        }
    };
    private RecyclerView recyclerView;
    private SearchResultProvider searchResultProvider;
    private MediaBrowserProvider mediaBrowserProvider;
    private String mediaId = MediaIDHelper.MEDIA_ID_TRACKS; // hack for adding all tracks to music list

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        searchResultProvider = new SearchResultProvider();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        searchAdapter = new SearchAdapter(getContext());
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(searchAdapter);
        searchResultProvider.search(query, null, searchCallback);
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
    public void onStop() {
        super.onStop();
        FireLog.d(TAG, "(++) onStop");
        MediaBrowserCompat mediaBrowser = mediaBrowserProvider.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mediaId != null) {
            mediaBrowser.unsubscribe(mediaId);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);

        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search),
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        getActivity().finish();
                        return false;
                    }
                });

        menu.findItem(R.id.action_search).expandActionView();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        searchResultProvider.search(query, null, searchCallback);
        hideInputManager();
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        if (newText.equals(query)) {
            return true;
        }
        query = newText;

        if (query.trim().equals("")) {
            searchAdapter.refreshData(new ArrayList<MediaItemWrapper>());
        } else {
            searchResultProvider.search(query, null, searchCallback);
        }

        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideInputManager();
        return false;
    }

    public void hideInputManager() {
        if (searchView != null) {
            if (mImm != null) {
                mImm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
            searchView.clearFocus();
        }
    }

    public void onConnected() {
        if (isDetached()) {
            return;
        }
        if (mediaId == null) {
            mediaId = mediaBrowserProvider.getMediaBrowser().getRoot();
        }
        mediaBrowserProvider.getMediaBrowser().unsubscribe(mediaId);
        mediaBrowserProvider.getMediaBrowser().subscribe(mediaId, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                super.onChildrenLoaded(parentId, children);
            }
        });
    }
}
