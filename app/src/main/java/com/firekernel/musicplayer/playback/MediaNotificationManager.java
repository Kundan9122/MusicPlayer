package com.firekernel.musicplayer.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firekernel.musicplayer.FireApplication;
import com.firekernel.musicplayer.R;
import com.firekernel.musicplayer.ui.MainActivity;
import com.firekernel.musicplayer.utils.ActionHelper;
import com.firekernel.musicplayer.utils.FireLog;
import com.firekernel.musicplayer.utils.ResourceHelper;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotificationManager extends BroadcastReceiver {
    public static final String ACTION_PAUSE = "com.firekernel.player.pause";
    public static final String ACTION_PLAY = "com.firekernel.player.play";
    public static final String ACTION_PREV = "com.firekernel.player.prev";
    public static final String ACTION_NEXT = "com.firekernel.player.next";
    public static final String ACTION_STOP = "com.firekernel.player.stop";
    private static final String TAG = FireLog.makeLogTag(MediaNotificationManager.class);

    private static final String CHANNEL_ID = "com.firekernel.player.MUSIC_CHANNEL_ID";

    private static final int NOTIFICATION_ID = 1 << 2;
    private static final int REQUEST_CODE = 1 << 3;
    private final MusicPlayerService service;
    private final NotificationManager notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;
    private final PendingIntent stopIntent;

    private final int notificationColor;
    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat controller;
    private MediaControllerCompat.TransportControls transportControls;
    private PlaybackStateCompat playbackState;
    private MediaMetadataCompat metadata;
    private boolean mStarted = false;
    private final MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            playbackState = state;
            FireLog.d(TAG, "Received new playback state" + state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED ||
                    state.getState() == PlaybackStateCompat.STATE_NONE) {
                stopNotification();
            } else {
                Notification notification = createNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            MediaNotificationManager.this.metadata = metadata;
            FireLog.d(TAG, "Received new metadata " + metadata);
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            FireLog.d(TAG, "Session was destroyed, resetting to the new session token");
            try {
                updateSessionToken();
            } catch (RemoteException e) {
                FireLog.e(TAG, "could not connect media controller", e);
            }
        }
    };

    public MediaNotificationManager(MusicPlayerService service) throws RemoteException {
        this.service = service;
        updateSessionToken();

        notificationColor = ResourceHelper.getThemeColor(this.service, R.attr.colorPrimary,
                Color.DKGRAY);

        notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = this.service.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(this.service, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(this.service, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(this.service, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(this.service, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        stopIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            metadata = controller.getMetadata();
            playbackState = controller.getPlaybackState();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                controller.registerCallback(callback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                service.registerReceiver(this, filter);

                service.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            controller.unregisterCallback(callback);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            service.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        FireLog.d(TAG, "Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            default:
                FireLog.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() throws RemoteException {
        MediaSessionCompat.Token freshToken = service.getSessionToken();
        if (sessionToken == null && freshToken != null ||
                sessionToken != null && !sessionToken.equals(freshToken)) {
            if (controller != null) {
                controller.unregisterCallback(callback);
            }
            sessionToken = freshToken;
            if (sessionToken != null) {
                controller = new MediaControllerCompat(service, sessionToken);
                transportControls = controller.getTransportControls();
                if (mStarted) {
                    controller.registerCallback(callback);
                }
            }
        }
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent openUI = new Intent(service, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openUI.putExtra(ActionHelper.EXTRA_START_NOW_PLAYING, true);
        if (description != null) {
            openUI.putExtra(ActionHelper.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        }
        return PendingIntent.getActivity(service, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Notification createNotification() {
        FireLog.d(TAG, "updateNotificationMetadata. metadata=" + metadata);
        if (metadata == null || playbackState == null) {
            return null;
        }

        MediaDescriptionCompat description = metadata.getDescription();

        Bitmap art = BitmapFactory.decodeResource(service.getResources(),
                R.drawable.ic_default_art);

        // Notification channels are only supported on Android O+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(service, CHANNEL_ID);

        final int playPauseButtonPosition = addActions(notificationBuilder);
        notificationBuilder
                .setStyle(new MediaStyle()
                        // show only play/pause in compact view
                        .setShowActionsInCompactView(playPauseButtonPosition)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopIntent)
                        .setMediaSession(sessionToken))
                .setDeleteIntent(stopIntent)
                .setColor(notificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        setNotificationPlaybackState(notificationBuilder);

        // load album art async
        if (description.getIconUri() != null) {
            String artUrl = description.getIconUri().toString();
            fetchBitmapAsync(artUrl, notificationBuilder);
        }

        return notificationBuilder.build();
    }

    private int addActions(final NotificationCompat.Builder notificationBuilder) {
        FireLog.d(TAG, "addActions");

        int playPauseButtonPosition = 0;
        // If skip to previous action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            notificationBuilder.addAction(R.drawable.ic_media_previous_notification,
                    service.getString(R.string.label_previous), previousIntent);

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1;
        }

        // Play or pause button, depending on the current state.
        final String label;
        final int icon;
        final PendingIntent intent;
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = service.getString(R.string.label_pause);
            icon = R.drawable.ic_media_pause_notification;
            intent = pauseIntent;
        } else {
            label = service.getString(R.string.label_play);
            icon = R.drawable.ic_media_play_notification;
            intent = playIntent;
        }
        notificationBuilder.addAction(new NotificationCompat.Action(icon, label, intent));

        // If skip to next action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            notificationBuilder.addAction(R.drawable.ic_media_next_notification,
                    service.getString(R.string.label_next), nextIntent);
        }

        return playPauseButtonPosition;
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        FireLog.d(TAG, "updateNotificationPlaybackState. playbackState=" + playbackState);
        if (playbackState == null || !mStarted) {
            FireLog.d(TAG, "updateNotificationPlaybackState. cancelling notification!");
            service.stopForeground(true);
            return;
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }

    private void fetchBitmapAsync(final String bitmapUrl,
                                  final NotificationCompat.Builder builder) {
        Glide.with(FireApplication.getInstance())
                .load(bitmapUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(100, 100) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        builder.setLargeIcon(resource);
//                        addActions(builder);
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                    }
                });
    }

    /**
     * Creates Notification Channel. This is required in Android O+ to display notifications.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            service.getString(R.string.notification_channel),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(
                    service.getString(R.string.notification_channel_description));

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
