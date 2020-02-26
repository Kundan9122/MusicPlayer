package com.firekernel.musicplayer.playback;

import android.app.Service;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import com.firekernel.musicplayer.utils.FireLog;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = FireLog.makeLogTag(PlaybackManager.class);
    // Action to thumbs up a media item

    private MusicPlayerServiceCallback serviceCallback;
    private QueueManager queueManager;
    private Playback playback;
    private MediaSessionCallback mediaSessionCallback;

    public PlaybackManager(MusicPlayerServiceCallback serviceCallback,
                           QueueManager queueManager,
                           Playback playback) {
        this.serviceCallback = serviceCallback;
        this.queueManager = queueManager;
        this.playback = playback;
        mediaSessionCallback = new MediaSessionCallback();
        this.playback.setCallback(this);
    }

    public Playback getPlayback() {
        return playback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    public void handlePlayRequest() {
        FireLog.d(TAG, "(++) handlePlayRequest: mState=" + playback.getState());
        MediaSessionCompat.QueueItem currentMusic = queueManager.getCurrentMusic();
        if (currentMusic != null) {
            serviceCallback.onPlaybackStart();
            playback.play(currentMusic);
        }
    }

    public void handlePauseRequest() {
        FireLog.d(TAG, "(++) handlePauseRequest: mState=" + playback.getState());
        if (playback.isPlaying()) {
            playback.pause();
            serviceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError) {
        FireLog.d(TAG, "(++) handleStopRequest: mState=" + playback.getState() + " error=" + withError);
        playback.stop(true);
        serviceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error) {
        FireLog.d(TAG, "(++) updatePlaybackState: playback state=" + playback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        int state = playback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentMusic = queueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        FireLog.i(TAG, "actions=" + actions);
        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        FireLog.i(TAG, "actions2=" + actions);
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (queueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            queueManager.updateMetadata();
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        FireLog.d(TAG, "setCurrentMediaId" + mediaId);
        queueManager.setQueueFromMusic(mediaId);
    }

    public interface MusicPlayerServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            FireLog.d(TAG, "(++) onPlay");
            if (queueManager.getCurrentMusic() == null) {
//                queueManager.setRandomQueue();
                Toast.makeText(((Service) serviceCallback).getApplicationContext(), "no queue", Toast.LENGTH_LONG).show();
            }
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            FireLog.d(TAG, "(++) onSkipToQueueItem:" + queueId);
            queueManager.setCurrentQueueItem(queueId);
            queueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            FireLog.d(TAG, "(++) onSeekTo:" + position);
            playback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            FireLog.d(TAG, "(++) onPlayFromMediaId mediaId:" + mediaId + "  extras=" + extras);
            queueManager.setQueueFromMusic(mediaId);
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            FireLog.d(TAG, "(++) onPause, current state=" + playback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            FireLog.d(TAG, "(++) onStop, current state=" + playback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            FireLog.d(TAG, "(++) onSkipToNext");
            if (queueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            queueManager.updateMetadata();
        }

        @Override
        public void onSkipToPrevious() {
            FireLog.d(TAG, "(++) onSkipToPrevious");
            if (queueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            queueManager.updateMetadata();
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            FireLog.d(TAG, "(++) onCustomAction: action=" + action);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            FireLog.d(TAG, "(++) onPlayFromSearch,  query=" + query + " extras=" + extras);
        }
    }
}
