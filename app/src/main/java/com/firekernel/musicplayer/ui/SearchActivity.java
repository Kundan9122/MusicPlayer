package com.firekernel.musicplayer.ui;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;

import com.firekernel.musicplayer.FirePopupMenuSelectedListener;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.playback.MediaBrowserProvider;
import com.firekernel.musicplayer.playback.MusicPlayerService;
import com.firekernel.musicplayer.ui.adapter.SearchAdapter;
import com.firekernel.musicplayer.ui.fragment.MediaListFragment;
import com.firekernel.musicplayer.ui.fragment.SearchFragment;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

public class SearchActivity extends BaseActivity implements MediaBrowserProvider,
        SearchAdapter.ItemClickListener, MediaListFragment.OnMediaItemSelectedListener, FirePopupMenuSelectedListener {
    private static final String TAG = FireLog.makeLogTag(SearchActivity.class);
    private ImageView bgView;
    private final MediaControllerCompat.Callback mediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    FireLog.d(TAG, "(++) onPlaybackStateChanged");
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    if (metadata == null) {
                        FireLog.e(TAG, "(++) MediaController.Callback.onMetadataChanged");
                        return;
                    }
                    SearchActivity.this.onMetadataChanged(metadata);
                }
            };
    private MediaBrowserCompat mediaBrowser;
    private final MediaBrowserCompat.ConnectionCallback mediaBrowserConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            FireLog.d(TAG, "(++) MediaBrowser.ConnectionCallback.onConnected");
            try {
                connectToSession(mediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                FireLog.e(TAG, "could not connect media controller", e);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bgView = (ImageView) findViewById(R.id.bgView);
//        ImageHelper.loadBlurBg(this, bgView);

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicPlayerService.class),
                mediaBrowserConnectionCallback,
                null);
        mediaBrowser.connect();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContent, SearchFragment.newInstance(), SearchFragment.TAG)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            connectToSession(mediaBrowser.getSessionToken());
        } catch (IllegalStateException | RemoteException e) {
            FireLog.e(TAG, "could not connect media controller");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FireLog.d(TAG, "onStop");

        if (MediaControllerCompat.getMediaController(this) != null) {
            MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaBrowser != null)
            mediaBrowser.disconnect();
        super.onDestroy();
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowser;
    }

    @Override
    public void onItemClick(MediaItemWrapper mediaItemWrapper) {
        FireLog.d(TAG, "(++) onItemClick");
        if (mediaItemWrapper == null)
            return;

        if (mediaItemWrapper.getMediaItem() != null && mediaItemWrapper.getMediaItem().isPlayable()) {
            onPlaySelected(mediaItemWrapper.getMediaItem());
        } else {
            ActionHelper.startMediaCategoryActivity(this, mediaItemWrapper);
        }
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        onPlaySelected(item);
    }

    @Override
    public void onPlaySelected(MediaBrowserCompat.MediaItem item) {
        FireLog.d(TAG, "(++) onPlaySelected");
        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
        }
    }

    @Override
    public void onShareSelected(MediaBrowserCompat.MediaItem item) {
        ActionHelper.shareTrack(this, item.getDescription());
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(this, token);
        MediaControllerCompat.setMediaController(this, mediaController);
        onMediaControllerConnected();
    }

    protected void onMediaControllerConnected() {
        // call mediaList onConnected
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SearchFragment.TAG);
        if (fragment != null) {
            ((SearchFragment) fragment).onConnected();
        }
        // connect activity to receive callback
        onConnected();
    }

    public void onConnected() {
        FireLog.d(TAG, "(++) onConnected");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            SearchActivity.this.onMetadataChanged(controller.getMetadata());
            controller.registerCallback(mediaControllerCallback);
        }
    }

    public void onMetadataChanged(MediaMetadataCompat metadata) {
        FireLog.d(TAG, "(++) onMetadataChanged " + metadata);

        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (metadata == null) {
            ImageHelper.loadBlurBg(this, bgView);
            return;
        }
        ImageHelper.loadBlurBg(this, bgView, metadata.getDescription());
    }
}
