package com.firekernel.musicplayer.playback;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.firekernel.musicplayer.source.MusicProvider;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.MediaIDHelper;
import com.firekernel.musicplayer.utils.QueueHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries, relying on a
 * given MusicProvider to provide the actual media metadata.
 */
public class QueueManager {
    private static final String TAG = FireLog.makeLogTag(QueueManager.class);

    private MusicProvider musicProvider;
    private MetadataUpdateListener metadataUpdateListener;

    // "Now playing" queue:
    private List<MediaSessionCompat.QueueItem> playingQueue;
    private int currentIndex;

    public QueueManager(@NonNull MusicProvider musicProvider,
                        @NonNull MetadataUpdateListener metadataUpdateListener) {
        this.musicProvider = musicProvider;
        this.metadataUpdateListener = metadataUpdateListener;

        playingQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        currentIndex = 0;
    }

    public boolean isSameBrowsingCategory(@NonNull String mediaId) {
        String[] newBrowseHierarchy = MediaIDHelper.getHierarchy(mediaId);
        MediaSessionCompat.QueueItem current = getCurrentMusic();
        if (current == null) {
            return false;
        }
        String[] currentBrowseHierarchy = MediaIDHelper.getHierarchy(
                current.getDescription().getMediaId());

        return Arrays.equals(newBrowseHierarchy, currentBrowseHierarchy);
    }

    private void setCurrentQueueIndex(int index) {
        if (index >= 0 && index < playingQueue.size()) {
            currentIndex = index;
            metadataUpdateListener.onCurrentQueueIndexUpdated(currentIndex);
        }
    }

    public boolean setCurrentQueueItem(long queueId) {
        // set the current index on queue from the queue Id:
        int index = QueueHelper.getMusicIndexOnQueue(playingQueue, queueId);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean setCurrentQueueItem(String mediaId) {
        // set the current index on queue from the music Id:
        int index = QueueHelper.getMusicIndexOnQueue(playingQueue, mediaId);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    public boolean skipQueuePosition(int amount) {
        int index = currentIndex + amount;
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0;
        } else {
            // skip forwards when in last song will cycle back to start of the queue
            index %= playingQueue.size();
        }
        if (!QueueHelper.isIndexPlayable(index, playingQueue)) {
            FireLog.e(TAG, "Cannot increment queue index by " + amount +
                    ". Current=" + currentIndex + " queue length=" + playingQueue.size());
            return false;
        }
        currentIndex = index;
        return true;
    }

    public void setQueueFromMusic(String mediaId) {
        FireLog.d(TAG, "(++) setQueueFromMusic: mediaId=" + mediaId);

        // The mediaId used here is not the unique musicId. This one comes from the
        // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
        // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
        // so we can build the correct playing queue, based on where the track was
        // selected from.
        boolean canReuseQueue = false;
        if (isSameBrowsingCategory(mediaId)) {
            canReuseQueue = setCurrentQueueItem(mediaId);
        }
        if (!canReuseQueue) {
            setCurrentQueue(QueueHelper.getPlayingQueue(mediaId, musicProvider), mediaId);
        }
        updateMetadata();
    }

    public MediaSessionCompat.QueueItem getCurrentMusic() {
        if (!QueueHelper.isIndexPlayable(currentIndex, playingQueue)) {
            return null;
        }
        return playingQueue.get(currentIndex);
    }

    protected void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue) {
        setCurrentQueue(newQueue, null);
    }

    protected void setCurrentQueue(List<MediaSessionCompat.QueueItem> newQueue,
                                   String initialMediaId) {
        playingQueue = newQueue;
        int index = 0;
        if (initialMediaId != null) {
            index = QueueHelper.getMusicIndexOnQueue(playingQueue, initialMediaId);
        }
        currentIndex = Math.max(index, 0);
        metadataUpdateListener.onQueueUpdated(newQueue);
    }

    public void updateMetadata() {
        MediaSessionCompat.QueueItem currentMusic = getCurrentMusic();
        if (currentMusic == null || currentMusic.getDescription() == null
                || currentMusic.getDescription().getMediaId() == null) {
            metadataUpdateListener.onMetadataRetrieveError();
            return;
        }
        final String musicId = MediaIDHelper.extractMusicIDFromMediaID(currentMusic.getDescription().getMediaId());
        MediaMetadataCompat metadata = musicProvider.getMusic(musicId);
        if (metadata == null) {
            throw new IllegalArgumentException("Invalid musicId " + musicId);
        }

        metadataUpdateListener.onMetadataChanged(metadata);

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
//        if (metadata.getDescription().getIconBitmap() == null &&
//                metadata.getDescription().getIconUri() != null) {
//            final String albumUri = metadata.getDescription().getIconUri().toString();
//            Glide.with(FireApplication.getInstance())
//                    .load(albumUri)
//                    .asBitmap()
//                    .into(new SimpleTarget<Bitmap>(200, 200) {
//                        @Override
//                        public void onResourceReady(final Bitmap resource, GlideAnimation glideAnimation) {
//                            musicProvider.updateMusicArt(musicId, resource, resource);
//
//                            // If we are still playing the same music, notify the listeners:
//                            MediaSessionCompat.QueueItem currentMusic = getCurrentMusic();
//                            if (currentMusic == null) {
//                                return;
//                            }
//                            String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
//                                    currentMusic.getDescription().getMediaId());
//                            if (musicId.equals(currentPlayingId)) {
//                                metadataUpdateListener.onMetadataChanged(musicProvider.getMusic(currentPlayingId));
//                            }
//
//                        }
//                    });
//
//        }
    }

    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);

        void onMetadataRetrieveError();

        void onCurrentQueueIndexUpdated(int queueIndex);

        void onQueueUpdated(List<MediaSessionCompat.QueueItem> newQueue);
    }
}
