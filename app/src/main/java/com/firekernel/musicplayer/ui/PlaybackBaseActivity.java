package com.firekernel.musicplayer.ui;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.playback.MediaBrowserProvider;
import com.firekernel.musicplayer.playback.MusicPlayerService;
import com.firekernel.musicplayer.ui.fragment.PlaybackControlsFragment;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.PermissionHelper;
import com.firekernel.musicplayer.utils.ResourceHelper;

/**
 * Created by Ashish on 6/27/2017.
 * Connects with MediaBrowser + capability to show hide MediaControlFragment
 */

public class PlaybackBaseActivity extends BaseActivity implements MediaBrowserProvider {
    private static final String TAG = FireLog.makeLogTag(PlaybackBaseActivity.class);
    private MediaBrowserCompat mediaBrowser;
    private PlaybackControlsFragment controlsFragment;
    // Callback that ensures that we are showing the controls
    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    FireLog.d(TAG, "(++) onPlaybackStateChanged");
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        FireLog.d(TAG, "mediaControllerCallback.onPlaybackStateChanged: " +
                                "hiding controls because state is " + state.getState());
                        hidePlaybackControls();
                    }
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    if (shouldShowControls()) {
                        showPlaybackControls();
                    } else {
                        FireLog.d(TAG, "mediaControllerCallback.onMetadataChanged: " +
                                "hiding controls because metadata is null");
                        hidePlaybackControls();
                    }
                }
            };
    private final MediaBrowserCompat.ConnectionCallback mediaBrowserConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    FireLog.d(TAG, "MediaBrowser.ConnectionCallback.onConnected");
                    try {
                        connectToSession(mediaBrowser.getSessionToken());
                    } catch (RemoteException e) {
                        FireLog.e(TAG, "could not connect media controller", e);
                        hidePlaybackControls();
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FireLog.d(TAG, "(++) onCreate");

        if (Build.VERSION.SDK_INT >= 21) {
            // Since our app icon has the same color as colorPrimary, our entry in the Recent Apps
            // list gets weird. We need to change either the icon or the color
            // of the TaskDescription.
            ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
                    getTitle().toString(),
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                    ResourceHelper.getThemeColor(this, R.attr.colorPrimary,
                            android.R.color.darker_gray));
            setTaskDescription(taskDesc);
        }

        PermissionHelper.requestPermission(this);

        // Connect a media browser just to get the media session token. There are other ways
        // this can be done, for example by sharing the session token directly.
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicPlayerService.class),
                mediaBrowserConnectionCallback,
                null);
        mediaBrowser.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FireLog.d(TAG, "(++) onStart");

        controlsFragment = (PlaybackControlsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        if (controlsFragment == null) {
            throw new IllegalStateException("Missing fragment with id 'controls'. Cannot continue.");
        }

        hidePlaybackControls();

//      mediaBrowser.connect();

        try {
            connectToSession(mediaBrowser.getSessionToken());
        } catch (IllegalStateException | RemoteException e) {
            FireLog.e(TAG, "could not connect media controller");
            hidePlaybackControls();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FireLog.d(TAG, "(++) onStop");

        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);
        }
//      mediaBrowser.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaBrowser != null)
            mediaBrowser.disconnect();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    protected void onMediaControllerConnected() {
        // empty implementation, can be overridden by clients.
    }

    protected void showPlaybackControls() {
        FireLog.d(TAG, "(++) showPlaybackControls");
        getSupportFragmentManager()
                .beginTransaction()
                .show(controlsFragment)
                .commitAllowingStateLoss();
    }

    protected void hidePlaybackControls() {
        FireLog.d(TAG, "(++) hidePlaybackControls");
        if (isFinishing() || isDestroyed()) {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .hide(controlsFragment)
                .commitAllowingStateLoss();
    }

    /**
     * Check if the MediaSession is active and in a "playback-able" state
     * (not NONE and not STOPPED).
     *
     * @return true if the MediaSession's state requires playback controls to be visible.
     */
    protected boolean shouldShowControls() {
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if (mediaController == null ||
                mediaController.getMetadata() == null ||
                mediaController.getPlaybackState() == null) {
            return false;
        }
        switch (mediaController.getPlaybackState().getState()) {
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                return false;
            default:
                return true;
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        if (shouldShowControls()) {
            showPlaybackControls();
        } else {
            FireLog.d(TAG, "connectionCallback.onConnected: " +
                    "hiding controls because metadata is null");
            hidePlaybackControls();
        }

        onMediaControllerConnected();
    }
}
