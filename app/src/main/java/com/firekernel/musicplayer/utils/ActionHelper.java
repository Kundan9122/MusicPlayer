package com.firekernel.musicplayer.utils;

import android.app.Activity;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.widget.Toast;

import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.model.MediaItemWrapper;
import com.firekernel.musicplayer.ui.MediaCategoryActivity;
import com.firekernel.musicplayer.ui.NowPlayingActivity;
import com.firekernel.musicplayer.ui.SearchActivity;

import java.io.File;



public class ActionHelper {
    public static final String EXTRA_START_NOW_PLAYING = "com.firekernel.player.EXTRA_START_NOW_PLAYING";
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "com.firekernel.player.CURRENT_MEDIA_DESCRIPTION";
    private static final String TAG = ActionHelper.class.getSimpleName();

    public static void startNowPlayingActivityIfNeeded(Activity activity, Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_NOW_PLAYING, false)) {
            Intent nowPlayingIntent = new Intent(activity, NowPlayingActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                            intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            activity.startActivity(nowPlayingIntent);
        }
    }

    public static void startNowPlayingActivity(Activity activity) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
        MediaMetadataCompat metadata = controller.getMetadata();

        Intent intent = new Intent(activity, NowPlayingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (metadata != null) {
            intent.putExtra(ActionHelper.EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    metadata.getDescription());
        }
        activity.startActivity(intent);
    }

    public static void startSearchActivity(Activity activity) {
        Intent intent = new Intent(activity, SearchActivity.class);
        activity.startActivity(intent);
    }

    public static void returnToSearchActivity(Activity activity) {
        Intent intent = new Intent(activity, SearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }

    public static void startAudioEffectActivity(Activity activity) {
        try {
            Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
//            effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, activity.getMediaBrowser().getSessionToken());
            activity.startActivityForResult(effects, 5 << 5);
        } catch (Exception e) {
            Toast.makeText(activity, R.string.no_equalizer, Toast.LENGTH_LONG).show();
            FireLog.e(TAG, "", e);
        }
    }

    public static void shareTrack(Activity activity, MediaDescriptionCompat description) {
        try {

            Uri uri = Uri.fromFile(new File(description.getMediaUri().toString()));
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            activity.startActivity(Intent.createChooser(share, "Share Sound File"));
        } catch (Exception e) {
            FireLog.e(TAG, "", e);
        }
    }

    public static void startMediaCategoryActivity(Activity activity, MediaItemWrapper mediaItemWrapper) {
        Intent intent = new Intent(activity, MediaCategoryActivity.class);
        intent.putExtra(MediaCategoryActivity.EXTRA_MEDIA_ITEM_WRAPPER, mediaItemWrapper);
        activity.startActivity(intent);
    }

}
