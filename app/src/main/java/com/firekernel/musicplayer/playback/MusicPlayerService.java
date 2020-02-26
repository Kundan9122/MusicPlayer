package com.firekernel.musicplayer.playback;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.source.MusicProvider;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.PackageValidator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_EMPTY_ROOT;
import static com.firekernel.musicplayer.utils.MediaIDHelper.MEDIA_ID_ROOT;

public class MusicPlayerService extends MediaBrowserServiceCompat implements
        PlaybackManager.MusicPlayerServiceCallback {

    public static final String ACTION_CMD = "com.firekernel.player.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";
    private static final String TAG = FireLog.makeLogTag(MusicPlayerService.class);
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 10 * 1000; //10 seconds
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    private MusicProvider musicProvider;
    private PlaybackManager playbackManager;
    private MediaSessionCompat session;
    private QueueManager.MetadataUpdateListener metadataUpdateListener = new QueueManager.MetadataUpdateListener() {

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            session.setMetadata(metadata);
        }

        @Override
        public void onMetadataRetrieveError() {
            playbackManager.updatePlaybackState(
                    getString(R.string.error_no_metadata));
        }

        @Override
        public void onCurrentQueueIndexUpdated(int queueIndex) {
            playbackManager.handlePlayRequest();
        }

        @Override
        public void onQueueUpdated(List<MediaSessionCompat.QueueItem> newQueue) {
            session.setQueue(newQueue);
        }
    };
    private MediaNotificationManager mediaNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        FireLog.d(TAG, "(++) onCreate");

        // Start a new MediaSession
        session = new MediaSessionCompat(this, MusicPlayerService.class.getSimpleName());
        setSessionToken(session.getSessionToken());

        musicProvider = MusicProvider.getInstance();

        QueueManager queueManager = new QueueManager(musicProvider, metadataUpdateListener);
        MediaPlayback playback = new MediaPlayback(this);
        playbackManager = new PlaybackManager(this, queueManager, playback);

        session.setCallback(playbackManager.getMediaSessionCallback());
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        playbackManager.updatePlaybackState(null);

        try {
            mediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        FireLog.d(TAG, "(++) onStartCommand, startIntent=" + startIntent + ", flags=" + flags + ", startId=" + startId);
        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    playbackManager.handlePauseRequest();
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(session, startIntent);
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        FireLog.d(TAG, "(++) onTaskRemoved");
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        FireLog.d(TAG, "(++) onDestroy");
        // Service is being killed, so make sure we release our resources
        playbackManager.handleStopRequest(null);
        mediaNotificationManager.stopNotification();

        delayedStopHandler.removeCallbacksAndMessages(null);
        session.release();
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 Bundle rootHints) {
        FireLog.d(TAG, "(++) onGetRoot: clientPackageName=" + clientPackageName +
                "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        // Returning null = no one can connect

        PackageValidator packageValidator = new PackageValidator(this);
        if (!packageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }

        return new BrowserRoot(MEDIA_ID_ROOT, null); // Name visible in Android Auto
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
        FireLog.d(TAG, "(++) onLoadChildren: parentMediaId=" + parentMediaId);
        if (MEDIA_ID_EMPTY_ROOT.equals(parentMediaId)) {
            result.sendResult(new ArrayList<MediaItem>());
        } else {
            // return results when the music library is retrieved
            result.detach();
            musicProvider.retrieveMediaAsync(parentMediaId, new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    result.sendResult(musicProvider.getChildren(parentMediaId));
                }
            });
        }
    }

    @Override
    public void onPlaybackStart() {
        FireLog.d(TAG, "(++) onPlaybackStart");
        session.setActive(true);

        delayedStopHandler.removeCallbacksAndMessages(null);

        startService(new Intent(getApplicationContext(), MusicPlayerService.class));
    }

    @Override
    public void onNotificationRequired() {
        FireLog.d(TAG, "(++) onNotificationRequired");
        mediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStop() {
        FireLog.d(TAG, "(++) onPlaybackStop");
        session.setActive(false);
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        FireLog.d(TAG, "(++) onPlaybackStateUpdated");
        session.setPlaybackState(newState);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicPlayerService> mWeakReference;

        private DelayedStopHandler(MusicPlayerService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicPlayerService service = mWeakReference.get();
            if (service != null && service.playbackManager.getPlayback() != null) {
                if (service.playbackManager.getPlayback().isPlaying()) {
                    FireLog.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                FireLog.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }
}
