package com.firekernel.musicplayer.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.firekernel.musicplayer.FireApplication;
import com.firekernel.musicplayer.utils.FireLog;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

public class MediaPlayback implements Playback {

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    private static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    private static final float VOLUME_NORMAL = 1.0f;
    private static final String TAG = FireLog.makeLogTag(MediaPlayback.class);
    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final Context context;
    private final WifiManager.WifiLock wifiLock;
    private final AudioManager audioManager;
    private final IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final ExoPlayerEventListener exoPlayerEventListener = new ExoPlayerEventListener();
    private boolean playOnFocusGain;
    private Callback callback;
    private volatile boolean audioNoisyReceiverRegistered;
    private volatile String currentMediaId;
    // Type of audio focus we have:
    private int currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    private SimpleExoPlayer simpleExoPlayer;
    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                FireLog.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicPlayerService.class);
                    i.setAction(MusicPlayerService.ACTION_CMD);
                    i.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.CMD_PAUSE);
                    context.startService(i);
                }
            }
        }
    };

    private boolean exoPlayerNullIsStopped = false;
    private final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            currentAudioFocusState = AUDIO_FOCUSED;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                            currentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost audio focus, but will gain it back (shortly), so note whether
                            // playback should resume
                            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            playOnFocusGain = simpleExoPlayer != null && simpleExoPlayer.getPlayWhenReady();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Lost audio focus, probably "permanently"
                            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            break;
                    }

                    if (simpleExoPlayer != null) {
                        // Update the player state based on the change
                        configurePlayerState();
                    }
                }
            };

    public MediaPlayback(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        this.wifiLock = ((WifiManager) FireApplication.getInstance().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "fire_lock");
    }

    @Override
    public void start() {
        // Nothing to do
    }

    @Override
    public void stop(boolean notifyListeners) {
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        releaseResources(true);
    }

    @Override
    public int getState() {
        if (simpleExoPlayer == null) {
            return exoPlayerNullIsStopped
                    ? PlaybackStateCompat.STATE_STOPPED
                    : PlaybackStateCompat.STATE_NONE;
        }
        switch (simpleExoPlayer.getPlaybackState()) {
            case ExoPlayer.STATE_IDLE:
                return PlaybackStateCompat.STATE_PAUSED;
            case ExoPlayer.STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case ExoPlayer.STATE_READY:
                return simpleExoPlayer.getPlayWhenReady()
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;
            case ExoPlayer.STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    @Override
    public void setState(int state) {
        // Nothing to do (simpleExoPlayer holds its own state).
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return playOnFocusGain || (simpleExoPlayer != null && simpleExoPlayer.getPlayWhenReady());
    }

    @Override
    public long getCurrentStreamPosition() {
        return simpleExoPlayer != null ? simpleExoPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        // Nothing to do. Position maintained by ExoPlayer.
    }

    @Override
    public void play(QueueItem item) {
        playOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, currentMediaId);
        if (mediaHasChanged) {
            currentMediaId = mediaId;
        }

        if (mediaHasChanged || simpleExoPlayer == null) {
            releaseResources(false); // release everything except the player

            String source = null;
            if (item.getDescription().getMediaUri() != null)
                source = item.getDescription().getMediaUri().toString();
            if (source != null && (source.contains("www") || source.contains("http"))) {
                source = source.replaceAll(" ", "%20"); // Escape spaces for URLs
            }

            if (simpleExoPlayer == null) {
                simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
                        context, new DefaultTrackSelector(), new DefaultLoadControl());
                simpleExoPlayer.addListener(exoPlayerEventListener);
            }

            // Android "O" makes much greater use of AudioAttributes, especially
            // with regards to AudioFocus. All of UAMP's tracks are music, but
            // if your content includes spoken word such as audiobooks or podcasts
            // then the content type should be set to CONTENT_TYPE_SPEECH for those
            // tracks.
            final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .setUsage(USAGE_MEDIA)
                    .build();
            simpleExoPlayer.setAudioAttributes(audioAttributes);

            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory dataSourceFactory =
                    new DefaultDataSourceFactory(
                            context, Util.getUserAgent(context, "fire_play"), null);
            // Produces Extractor instances for parsing the media data.
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            // The MediaSource represents the media to be played.
            MediaSource mediaSource =
                    new ExtractorMediaSource(
                            Uri.parse(source), dataSourceFactory, extractorsFactory, null, null);

            // Prepares media to play (happens on background thread) and triggers
            // {@code onPlayerStateChanged} callback when the stream is ready to play.
            simpleExoPlayer.prepare(mediaSource);

            // If we are streaming from the internet, we want to hold a
            // Wifi lock, which prevents the Wifi radio from going to
            // sleep while the song is playing.
            wifiLock.acquire();
        }

        configurePlayerState();
    }

    @Override
    public void pause() {
        // Pause player and cancel the 'foreground service' state.
        if (simpleExoPlayer != null) {
            simpleExoPlayer.setPlayWhenReady(false);
        }
        // While paused, retain the player instance, but give up audio focus.
        releaseResources(false);
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(long position) {
        if (simpleExoPlayer != null) {
            registerAudioNoisyReceiver();
            simpleExoPlayer.seekTo(position);
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public String getCurrentMediaId() {
        return currentMediaId;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.currentMediaId = mediaId;
    }

    private void tryToGetAudioFocus() {
        FireLog.d(TAG, "(++) tryToGetAudioFocus");
        int result =
                audioManager.requestAudioFocus(
                        onAudioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AUDIO_FOCUSED;
        } else {
            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void giveUpAudioFocus() {
        FireLog.d(TAG, "(++) giveUpAudioFocus");
        if (audioManager.abandonAudioFocus(onAudioFocusChangeListener)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures the player according to audio focus settings and starts/restarts it. This method
     * starts/restarts the ExoPlayer instance respecting the current audio focus state. So if we
     * have focus, it will play normally; if we don't have focus, it will either leave the player
     * paused or set it to a low volume, depending on what is permitted by the current focus
     * settings.
     */
    private void configurePlayerState() {
        FireLog.d(TAG, "(++) configMediaPlayerState");
        if (currentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause();
        } else {
            registerAudioNoisyReceiver();

            if (currentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                simpleExoPlayer.setVolume(VOLUME_DUCK);
            } else {
                simpleExoPlayer.setVolume(VOLUME_NORMAL);
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                FireLog.d(TAG, "configMediaPlayerState startMediaPlayer");
                simpleExoPlayer.setPlayWhenReady(true);
                playOnFocusGain = false;
            }
        }
    }

    /**
     * Releases resources used by the service for playback, which is mostly just the WiFi lock for
     * local playback. If requested, the ExoPlayer instance is also released.
     *
     * @param releasePlayer Indicates whether the player should also be released
     */
    private void releaseResources(boolean releasePlayer) {

        // Stops and releases player (if requested and available).
        if (releasePlayer && simpleExoPlayer != null) {
            simpleExoPlayer.release();
            simpleExoPlayer.removeListener(exoPlayerEventListener);
            simpleExoPlayer = null;
            exoPlayerNullIsStopped = true;
            playOnFocusGain = false;
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            audioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver);
            audioNoisyReceiverRegistered = false;
        }
    }

    private final class ExoPlayerEventListener implements ExoPlayer.EventListener {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            // Nothing to do.
        }

        @Override
        public void onTracksChanged(
                TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Nothing to do.
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Nothing to do.
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                case ExoPlayer.STATE_BUFFERING:
                case ExoPlayer.STATE_READY:
                    if (callback != null) {
                        callback.onPlaybackStatusChanged(getState());
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    // The media player finished playing the current song.
                    if (callback != null) {
                        callback.onCompletion();
                    }
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            final String what;
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    what = error.getSourceException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    what = error.getRendererException().getMessage();
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                    what = error.getUnexpectedException().getMessage();
                    break;
                default:
                    what = "Unknown: " + error;
            }

            if (callback != null) {
                callback.onError("ExoPlayer error " + what);
            }
        }

        @Override
        public void onPositionDiscontinuity() {
            // Nothing to do.
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Nothing to do.
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            // Nothing to do.
        }
    }
}
