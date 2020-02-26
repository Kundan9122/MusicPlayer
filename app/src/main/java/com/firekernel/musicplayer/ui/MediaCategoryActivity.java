package com.firekernel.musicplayer.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;

import com.firekernel.musicplayer.FirePopupMenuSelectedListener;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.ui.fragment.MediaListFragment;
import com.firekernel.musicplayer.ui.fragment.PlaybackControlsFragment;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

public class MediaCategoryActivity extends PlaybackBaseActivity implements
        MediaListFragment.OnMediaItemSelectedListener,
        FirePopupMenuSelectedListener {
    public static final String TAG = FireLog.makeLogTag(MediaCategoryActivity.class);
    public static final String EXTRA_MEDIA_ITEM_WRAPPER = "media_item";

    private MediaItemWrapper mediaItemWrapper;
    private ImageView bgView;
    private String mArtUrl = ""; // don't set null

    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            FireLog.d(TAG, "(++) onPlaybackStateChanged state= " + state.getState());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            FireLog.d(TAG, "(++) onMetadataChanged mediaId=" + metadata.getDescription().getMediaId() + " song=" + metadata.getDescription().getTitle());
            MediaCategoryActivity.this.onMetadataChanged(metadata); // always use this context
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_category);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bgView = (ImageView) findViewById(R.id.bgView);
//        ImageHelper.loadBlurBg(this, bgView);

        if (getIntent().getExtras() == null) {
            FireLog.e(TAG, "Extras are null");
            finish();
        }
        mediaItemWrapper = getIntent().getExtras().getParcelable(EXTRA_MEDIA_ITEM_WRAPPER);
        if (mediaItemWrapper == null) {
            FireLog.e(TAG, "mediaItemWrapper is null");
            finish();
        }
        String title = mediaItemWrapper.getMediaItem().getDescription().getTitle() + "";
        Fragment fragment = MediaListFragment.newInstance(title, mediaItemWrapper.getMediaItem().getMediaId());
        String tag = MediaListFragment.TAG;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContent, fragment, tag)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_search:
                ActionHelper.returnToSearchActivity(this);
//                ActionHelper.startSearchActivity(this);
                return true;
            case R.id.action_audio_effects:
                ActionHelper.startAudioEffectActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onMediaControllerConnected() {
        super.onMediaControllerConnected();
        FireLog.d(TAG, "(++) onMediaControllerConnected");

        // connect MediaListFragment
        Fragment fragment = getMediaListFragment();
        if (fragment != null) {
            ((MediaListFragment) fragment).onConnected();
        }

        Fragment fragmentControl = getControlFragment();
        if (fragmentControl != null) {
            ((PlaybackControlsFragment) fragmentControl).onConnected();
        }

        this.onConnected();
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

    public void onConnected() {
        FireLog.d(TAG, "onConnected");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            MediaCategoryActivity.this.onMetadataChanged(controller.getMetadata());
            controller.registerCallback(mediaControllerCallback);
        }
    }

    public void onMetadataChanged(MediaMetadataCompat metadata) {
        FireLog.d(TAG, "onMetadataChanged " + metadata);

        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (metadata == null) {
            ImageHelper.loadBlurBg(this, bgView);
            return;
        }
        // prevent multiple calls
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            ImageHelper.loadBlurBg(this, bgView, metadata.getDescription());
        }

    }

    private MediaListFragment getMediaListFragment() {
        return (MediaListFragment) getSupportFragmentManager().findFragmentByTag(MediaListFragment.TAG);
    }

    private PlaybackControlsFragment getControlFragment() {
        return (PlaybackControlsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_playback_controls);
    }
}
