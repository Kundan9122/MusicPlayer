package com.firekernel.musicplayer.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.playback.MusicPlayerService;
import com.firekernel.musicplayer.ui.widget.CircularSeekBar;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A Now playing Screen that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class NowPlayingActivity extends BaseActivity {
    private static final String TAG = FireLog.makeLogTag(NowPlayingActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 400;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;
    private final Handler mHandler = new Handler();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ImageView bgView;
    private CircularSeekBar circularSeekBar;
    private ImageView albumArt;
    private ProgressBar progressBar;
    private TextView titleView;
    private TextView subTitleView;
    private TextView startTv;
    private TextView endTv;
    private SeekBar seekBar;
    private ImageButton previousBtn;
    private ImageButton playPauseBtn;
    private ImageButton nextBtn;
    private Drawable pauseDrawable;
    private Drawable playDrawable;
    private MediaBrowserCompat mediaBrowser;
    private PlaybackStateCompat playbackState;
    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };
    private ScheduledFuture<?> scheduleFuture;
    private MediaDescriptionCompat mediaDescription;
    private String mArtUrl = "";
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            FireLog.d(TAG, "(++) MediaController.Callback.onPlaybackStateChanged state= " + state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            FireLog.d(TAG, "(++) MediaController.Callback.onMetadataChanged metadata= " + metadata);
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }
    };
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
    private CircularSeekBar.OnCircularSeekBarChangeListener onCircularSeekBarChangeListener = new CircularSeekBar.OnCircularSeekBarChangeListener() {
        @Override
        public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
            startTv.setText(DateUtils.formatElapsedTime(progress / 1000));
        }

        @Override
        public void onStopTrackingTouch(CircularSeekBar seekBar) {
            MediaControllerCompat.getMediaController(NowPlayingActivity.this).getTransportControls()
                    .seekTo(seekBar.getProgress());
            scheduleSeekbarUpdate();
        }

        @Override
        public void onStartTrackingTouch(CircularSeekBar seekBar) {

        }
    };
    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            startTv.setText(DateUtils.formatElapsedTime(progress / 1000));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            stopSeekbarUpdate();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            MediaControllerCompat.getMediaController(NowPlayingActivity.this).getTransportControls()
                    .seekTo(seekBar.getProgress());
            scheduleSeekbarUpdate();
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat.TransportControls controls = MediaControllerCompat
                    .getMediaController(NowPlayingActivity.this).getTransportControls();
            switch (v.getId()) {

                case R.id.previous:
                    controls.skipToPrevious();
                    break;
                case R.id.playPause:
                    PlaybackStateCompat state = MediaControllerCompat
                            .getMediaController(NowPlayingActivity.this).getPlaybackState();
                    if (state != null) {

                        switch (state.getState()) {
                            case PlaybackStateCompat.STATE_PLAYING: // fall through
                            case PlaybackStateCompat.STATE_BUFFERING:
                                controls.pause();
                                stopSeekbarUpdate();
                                break;
                            case PlaybackStateCompat.STATE_PAUSED:
                            case PlaybackStateCompat.STATE_STOPPED:
                                controls.play();
                                scheduleSeekbarUpdate();
                                break;
                            default:
                                FireLog.d(TAG, "onClick with state " + state.getState());
                        }
                    }
                    break;
                case R.id.next:
                    controls.skipToNext();
                    break;
            }

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bgView = (ImageView) findViewById(R.id.bgView);
        circularSeekBar = (CircularSeekBar) findViewById(R.id.circularProgressBar);
        circularSeekBar.setOnSeekBarChangeListener(onCircularSeekBarChangeListener);
        albumArt = (ImageView) findViewById(R.id.albumArt);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        titleView = (TextView) findViewById(R.id.title);
        subTitleView = (TextView) findViewById(R.id.subTitle);
        startTv = (TextView) findViewById(R.id.startText);
        endTv = (TextView) findViewById(R.id.endText);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        previousBtn = (ImageButton) findViewById(R.id.previous);
        previousBtn.setOnClickListener(onClickListener);
        playPauseBtn = (ImageButton) findViewById(R.id.playPause);
        playPauseBtn.setOnClickListener(onClickListener);
        nextBtn = (ImageButton) findViewById(R.id.next);
        nextBtn.setOnClickListener(onClickListener);

        pauseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_media_pause_circle);
        playDrawable = ContextCompat.getDrawable(this, R.drawable.ic_media_play_circle);

        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicPlayerService.class),
                mediaBrowserConnectionCallback, null);
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_now_playing, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_audio_effects:
                ActionHelper.startAudioEffectActivity(this);
                return true;
            case R.id.action_share:
                ActionHelper.shareTrack(this, getMediaDescription());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaBrowser != null) {
            mediaBrowser.disconnect();
        }

        if (MediaControllerCompat.getMediaController(NowPlayingActivity.this) != null) {
            MediaControllerCompat.getMediaController(NowPlayingActivity.this).unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        executorService.shutdown();
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(
                NowPlayingActivity.this, token);
        if (mediaController.getMetadata() == null) { // metadata = null when no media is playing
            finish();
            return;
        }
        MediaControllerCompat.setMediaController(NowPlayingActivity.this, mediaController);
        mediaController.registerCallback(mediaControllerCallback);

        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            MediaDescriptionCompat description = intent.getParcelableExtra(
                    ActionHelper.EXTRA_CURRENT_MEDIA_DESCRIPTION);
            if (description != null) {
                updateMediaDescription(description);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!executorService.isShutdown()) {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(updateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
    }

    private MediaDescriptionCompat getMediaDescription() {
        return mediaDescription;
    }

    private void setMediaDescription(MediaDescriptionCompat mediaDescription) {
        this.mediaDescription = mediaDescription;
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        FireLog.d(TAG, "(++) updateMediaDescription");
        if (description == null) {
            return;
        }
        setMediaDescription(description);

        titleView.setText(description.getTitle());
        subTitleView.setText(description.getSubtitle());

        // prevent multiple calls
        String artUrl = null;
        if (description.getIconUri() != null) {
            artUrl = description.getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            ImageHelper.loadArtAndBlurBg(this, albumArt, bgView, description);
        }
        ImageHelper.loadArtAndBlurBg(this, albumArt, bgView, description);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        FireLog.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        seekBar.setMax(duration);
        circularSeekBar.setMax(duration);
        endTv.setText(DateUtils.formatElapsedTime(duration / 1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        playbackState = state;

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                progressBar.setVisibility(INVISIBLE);
                playPauseBtn.setVisibility(VISIBLE);
                playPauseBtn.setImageDrawable(pauseDrawable);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                progressBar.setVisibility(INVISIBLE);
                playPauseBtn.setVisibility(VISIBLE);
                playPauseBtn.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                progressBar.setVisibility(INVISIBLE);
                playPauseBtn.setVisibility(VISIBLE);
                playPauseBtn.setImageDrawable(playDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                playPauseBtn.setVisibility(INVISIBLE);
                progressBar.setVisibility(VISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                FireLog.d(TAG, "Unhandled state " + state.getState());
        }

        previousBtn.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE);
        previousBtn.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE);
    }

    private void updateProgress() {
        if (playbackState == null) {
            return;
        }
        long currentPosition = playbackState.getPosition();
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            long timeDelta = SystemClock.elapsedRealtime() -
                    playbackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * playbackState.getPlaybackSpeed();
        }
        seekBar.setProgress((int) currentPosition);
        circularSeekBar.setProgress((int) currentPosition);
    }
}
