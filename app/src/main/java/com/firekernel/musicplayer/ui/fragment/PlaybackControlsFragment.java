package com.firekernel.musicplayer.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ImageHelper;

public class PlaybackControlsFragment extends Fragment {

    private static final String TAG = FireLog.makeLogTag(PlaybackControlsFragment.class);
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
            PlaybackStateCompat stateObj = controller.getPlaybackState();
            final int state = stateObj == null ?
                    PlaybackStateCompat.STATE_NONE : stateObj.getState();
            FireLog.d(TAG, "onClick, in state " + state);
            switch (v.getId()) {
                case R.id.playPause:
                    FireLog.d(TAG, "onClick Play, in state " + state);
                    if (state == PlaybackStateCompat.STATE_PAUSED ||
                            state == PlaybackStateCompat.STATE_STOPPED ||
                            state == PlaybackStateCompat.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                            state == PlaybackStateCompat.STATE_BUFFERING ||
                            state == PlaybackStateCompat.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
            }
        }
    };
    private ImageView bgView;
    private ImageButton playPause;
    private TextView title;
    private TextView subtitle;
    private ImageView albumArt;
    private String mArtUrl = ""; // cant set null as url returned by description may be null
    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                FireLog.e(TAG, "(++) MediaController.Callback.onMetadataChanged: metadata is null");
                return;
            }
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FireLog.d(TAG, "(++) onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        bgView = (ImageView) rootView.findViewById(R.id.bgView);
        playPause = (ImageButton) rootView.findViewById(R.id.playPause);
        playPause.setEnabled(true);
        playPause.setOnClickListener(onClickListener);

        title = (TextView) rootView.findViewById(R.id.title);
        subtitle = (TextView) rootView.findViewById(R.id.artist);
        albumArt = (ImageView) rootView.findViewById(R.id.albumArt);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionHelper.startNowPlayingActivity(getActivity());
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FireLog.d(TAG, "(++) onStart");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
//            onConnected(); // fixing memory leak(call of onMetadataChanged)
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Fix of (state loss )Bug on API Level > 11.
    }

    @Override
    public void onStop() {
        super.onStop();
        FireLog.d(TAG, "(++) onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            FireLog.d(TAG, "Unregister callback=" + mediaControllerCallback);
            controller.unregisterCallback(mediaControllerCallback);
        }
    }

    // Called form base activity as well
    public void onConnected() {
        FireLog.d(TAG, "(++) onConnected");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            PlaybackControlsFragment.this.onPlaybackStateChanged(controller.getPlaybackState());
            PlaybackControlsFragment.this.onMetadataChanged(controller.getMetadata());
            FireLog.d(TAG, "Register callback=" + mediaControllerCallback);
            controller.registerCallback(mediaControllerCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        FireLog.d(TAG, "(++) onMetadataChanged " + metadata);
        if (getActivity() == null) {
            FireLog.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }

        if (metadata == null) {
            return;
        }

        FireLog.i(TAG, "title=" + metadata.getDescription().getTitle().toString());

        title.setText(metadata.getDescription().getTitle());
        subtitle.setText(metadata.getDescription().getSubtitle());

        // prevent multiple calls
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        FireLog.d(TAG, "mArtUrl=" + mArtUrl + ", artUrl=" + artUrl);
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
//            ImageHelper.loadArt(getContext(), albumArt, metadata.getDescription());
//            ImageHelper.loadBlurBg(getContext(), bgView, metadata.getDescription());
            ImageHelper.loadArtAndBlurBg(getContext(), albumArt, bgView, metadata.getDescription());
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        FireLog.d(TAG, "(++) onPlaybackStateChanged " + state);
        if (getActivity() == null) {
            FireLog.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                FireLog.e(TAG, "error playbackstate: " + state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            playPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
        } else {
            playPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_pause));
        }

    }

    private void playMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }
}
