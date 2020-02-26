package com.firekernel.musicplayer.source;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.firekernel.musicplayer.FireApplication;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.model.Category;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class SearchResultProvider {
    public static final String SEARCH_LIMIT = "limit";
    private static final String TAG = FireLog.makeLogTag(SearchResultProvider.class);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int limit = 10;
    private AsyncTask asyncTask;
    private ArrayList<MediaItemWrapper> results = new ArrayList<>();
    private LocalSource source;
    private String query;
    private Bundle bundle;
    private SearchCallback searchCallback;

    public SearchResultProvider() {
        this(new LocalSource());
    }

    public SearchResultProvider(LocalSource source) {
        this.source = source;
    }

    public void search(@NonNull String query, Bundle bundle, @NonNull SearchCallback searchCallback) {
        this.query = query;
        this.bundle = bundle;
        this.searchCallback = searchCallback;
        if (bundle != null && bundle.containsKey(SEARCH_LIMIT)) {
            limit = bundle.getInt(SEARCH_LIMIT, limit);
        }
        searchAsync(query);
    }

    public void searchAsync(final @NonNull String query) {
        FireLog.d(TAG, "(++) searchAsync");
        if (asyncTask != null) {
            asyncTask.cancel(false);
            asyncTask = null;
        }
        asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return retrieveMedia(query);
            }

            @Override
            protected void onPostExecute(Boolean initialized) {
                if (initialized && searchCallback != null) {
                    searchCallback.onSearchResult(query, bundle, results);
                }
            }
        }.executeOnExecutor(executorService);
    }

    private synchronized boolean retrieveMedia(@NonNull String query) {
        boolean initialized = false;
        results.clear();
        try {
            Iterator<MediaMetadataCompat> iterator = source.searchTracks(query, limit).iterator();
            if (iterator.hasNext()) {
                results.add(new MediaItemWrapper(FireApplication.getInstance().getResources().getString(R.string.nav_menu_tracks)));
                while (iterator.hasNext()) {
                    MediaMetadataCompat metadata = iterator.next();
                    String category = MediaIDHelper.MEDIA_ID_TRACKS;
                    String subCategory = MediaIDHelper.MEDIA_ID_TRACKS_ALL;
                    results.add(new MediaItemWrapper(Category.TRACK,
                            createPlayableMediaItem(category, subCategory, metadata)));
                }
            }

            iterator = source.searchAlbums(query, limit).iterator();
            if (iterator.hasNext()) {
                results.add(new MediaItemWrapper(FireApplication.getInstance().getResources().getString(R.string.nav_menu_albums)));
                while (iterator.hasNext()) {
                    MediaMetadataCompat metadata = iterator.next();
                    String category = MediaIDHelper.MEDIA_ID_ALBUM;
                    String subCategory = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    results.add(new MediaItemWrapper(Category.ALBUM,
                            createBrowsableMediaItemForSubCategory(category, subCategory, metadata)));
                }
            }

            iterator = source.searchArtists(query, limit).iterator();
            if (iterator.hasNext()) {
                results.add(new MediaItemWrapper(FireApplication.getInstance().getString(R.string.nav_menu_artists)));
                while (iterator.hasNext()) {
                    MediaMetadataCompat metadata = iterator.next();
                    String category = MediaIDHelper.MEDIA_ID_ARTIST;
                    String subCategory = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    results.add(new MediaItemWrapper(Category.ARTIST,
                            createBrowsableMediaItemForSubCategory(category, subCategory, metadata)));
                }
            }
            if (results.size() == 0) {
                results.add(new MediaItemWrapper(FireApplication.getInstance().getResources().getString(R.string.nothing_found)));
            }
            initialized = true;
        } catch (Exception e) {
            FireLog.e(TAG, "Media Initialization failed", e);
        }
        return initialized;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForSubCategory(String category,
                                                                                String subCategory,
                                                                                MediaMetadataCompat metadata) {
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(null, category, subCategory);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();

        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createPlayableMediaItem(String category, String subCategory,
                                                                 MediaMetadataCompat metadata) {
        String musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(musicId, category, subCategory);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();

        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

    public interface SearchCallback {
        void onSearchResult(@NonNull String query, Bundle bundle, @NonNull List<MediaItemWrapper> items);

        void onError(@NonNull String query, Bundle bundle);
    }

}
