package com.firekernel.musicplayer.playback;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;

/**
 * Interface representing either Local or Remote Playback. The {@link MusicPlayerService} works
 * directly with an instance of the Playback object to make the various calls such as
 * play, pause etc.
 */
public interface Playback {
    void start();

    void play(QueueItem item);

    void pause();

    void stop(boolean notifyListeners);

    int getState();

    void setState(int state);

    boolean isConnected();

    boolean isPlaying();

    void seekTo(long position);

    long getCurrentStreamPosition();

    void updateLastKnownStreamPosition();

    String getCurrentMediaId();

    void setCurrentMediaId(String mediaId);

    void setCallback(Callback callback);

    interface Callback {
        void onCompletion();

        void onPlaybackStatusChanged(int state);

        void onError(String error);

        void setCurrentMediaId(String mediaId);
    }
}
