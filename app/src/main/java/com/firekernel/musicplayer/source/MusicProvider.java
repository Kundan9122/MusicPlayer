package com.firekernel.musicplayer.source;

import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ALBUM;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ARTIST;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_FOLDER;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_GENRE;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_PLAYLIST;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_TRACKS;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_TRACKS_ALL;

/**
 * Simple data provider for music tracks. The actual metadata localSource is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 * MediaId = Category/SubCategory|musicId
 */
public class MusicProvider {
    private static final String TAG = FireLog.makeLogTag(MusicProvider.class);
    //only  playable music list, gets updated only when medialist is playable
    // used by updatemetadata while playing
    private final CopyOnWriteArrayList<MediaMetadataCompat> musicList;
    // media list contains browsable + playable media items
    private final CopyOnWriteArrayList<MediaMetadataCompat> mediaList;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MusicProviderSource localSource;
    private MusicProviderSource remoteSource;

    private MusicProvider() {
        // not following the adapter pattern
        this(new LocalSource(), new RemoteSource());
    }

    private MusicProvider(MusicProviderSource localSource, MusicProviderSource remoteSource) {
        this.localSource = localSource;
        this.remoteSource = remoteSource;
        musicList = new CopyOnWriteArrayList<>();
        mediaList = new CopyOnWriteArrayList<>();
    }

    public static MusicProvider getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final String mediaId, final Callback callback) {
        FireLog.d(TAG, "(++) retrieveMediaAsync");
        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return retrieveMedia(mediaId);
            }

            @Override
            protected void onPostExecute(Boolean initialized) {
                if (callback != null) {
                    callback.onMusicCatalogReady(initialized);
                }
            }
        }.executeOnExecutor(executorService);
    }

    private synchronized boolean retrieveMedia(String mediaId) {
        boolean initialized = false;
        mediaList.clear();
        try {
            Iterator<MediaMetadataCompat> tracks = localSource.iterator(mediaId);
            while (tracks.hasNext()) {
                MediaMetadataCompat item = tracks.next();
                mediaList.add(item);
            }
            initialized = true;
        } catch (Exception e) {
            FireLog.e(TAG, "Media Initialization failed", e);
        }
        return initialized;
    }

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId) {
        FireLog.d(TAG, "(++) getChildren, mediaId=" + mediaId);
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (mediaId.equals(MEDIA_ID_ROOT)) {
            // no item for root // root items are handled by Drawer

        } else if (mediaId.equals(MEDIA_ID_TRACKS)) {
            // fill the music List once and keep ever
            musicList.addAll(mediaList);

            for (MediaMetadataCompat metadata : getAllRetrievedMetadata()) {
                mediaItems.add(createTracksMediaItem(metadata));
            }
        } else if (mediaId.equals(MEDIA_ID_PLAYLIST) || mediaId.equals(MEDIA_ID_ALBUM)
                || mediaId.equals(MEDIA_ID_ARTIST) || mediaId.equals(MEDIA_ID_GENRE)
                || mediaId.equals(MEDIA_ID_FOLDER)) {
            String category = mediaId;
            for (MediaMetadataCompat metadata : getAllRetrievedMetadata()) {
                String subCategory = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                mediaItems.add(createBrowsableMediaItemForSubCategory(category, subCategory, metadata));
            }
        } else if (mediaId.startsWith(MEDIA_ID_PLAYLIST) || mediaId.startsWith(MEDIA_ID_ALBUM)
                || mediaId.startsWith(MEDIA_ID_ARTIST) || mediaId.startsWith(MEDIA_ID_GENRE)
                || mediaId.startsWith(MEDIA_ID_FOLDER)) {
            String category = MediaIDHelper.getHierarchy(mediaId)[0];
            String subCategory = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getAllRetrievedMetadata()) {
                mediaItems.add(createPlayableMediaItem(category, subCategory, metadata));
            }

        } else {
            FireLog.w(TAG, "unmatched mediaId: " + mediaId);
        }
        return mediaItems;
    }

    public List<MediaMetadataCompat> getAllRetrievedMetadata() {
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        for (MediaMetadataCompat track : mediaList) {
            result.add(track);
        }
        return result;
    }

    public List<MediaMetadataCompat> getAllRetrievedMusic() {
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        for (MediaMetadataCompat track : musicList) {
            result.add(track);
        }
        return result;
    }

    private MediaBrowserCompat.MediaItem createTracksMediaItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_TRACKS, MEDIA_ID_TRACKS_ALL);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

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

    public MediaMetadataCompat getMusic(String musicId) {
        for (MediaMetadataCompat metadataCompat : musicList) {
            if (musicId.equals(metadataCompat.getDescription().getMediaId())) {
                return metadataCompat;
            }
        }
        return null;
    }

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

//    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
//        MediaMetadataCompat metadata = getMusic(musicId);
//        metadata = new MediaMetadataCompat.Builder(metadata)
//
//                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
//                // example, on the lockscreen background when the media session is active.
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
//
//                // set small version of the album art in the DISPLAY_ICON. This is used on
//                // the MediaDescription and thus it should be small to be serialized if
//                // necessary
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
//
//                .build();
//
//        MutableMediaMetadata mutableMetadata = musicList.get(musicId);
//        if (mutableMetadata == null) {
//            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
//                    "MusicProvider");
//        }
//
//        mutableMetadata.setMetadata(metadata);
//    }

    private static class LazyHolder {
        public static final MusicProvider INSTANCE = new MusicProvider();
    }
}
