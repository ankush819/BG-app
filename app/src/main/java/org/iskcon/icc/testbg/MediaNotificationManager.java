package org.iskcon.icc.testbg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by ashankar on 5/14/17.
 */

public class MediaNotificationManager extends BroadcastReceiver {

    private static final String TAG = "Ankush" + MediaNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "org.iskcon.icc.testbg.pause";
    public static final String ACTION_PLAY = "org.iskcon.icc.testbg.play";
    public static final String ACTION_PREV = "org.iskcon.icc.testbg.prev";
    public static final String ACTION_NEXT = "org.iskcon.icc.testbg.next";

    private final MusicService musicService;
    private MediaSession.Token sessionToken;
    private MediaController mediaController;
    private MediaController.TransportControls transportControls;

    private PlaybackState playbackState;
    private MediaMetadata metaData;

    private NotificationManager notificationManager;

    private PendingIntent pauseIntent;
    private PendingIntent playIntent;
    private PendingIntent previousIntent;
    private PendingIntent nextIntent;

    private int notificationColor;

    private boolean started = false;

    public MediaNotificationManager(MusicService musicService) {
        this.musicService = musicService;
        updateSessionToken();

        //Set color

        notificationManager = (NotificationManager) musicService.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = musicService.getPackageName();
        pauseIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(musicService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        notificationManager.cancelAll();
    }

    public void startNotification() {
        if (!started) {
            metaData = mediaController.getMetadata();
            playbackState = mediaController.getPlaybackState();

            Notification notification = createNotification();
            if (notification != null) {
                mediaController.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                musicService.registerReceiver(this, filter);

                musicService.startForeground(NOTIFICATION_ID, notification);
                started = true;
            }
        }
    }

    public void stopNotification() {
        if (started) {
            started = false;
            mediaController.unregisterCallback(mCb);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                musicService.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {

            }
            musicService.stopForeground(true);
        }
    }

    private void updateSessionToken() {
        MediaSession.Token freshToken = musicService.getSessionToken();
        if (sessionToken == null || !sessionToken.equals(freshToken)) {
            if (mediaController != null) {
                mediaController.unregisterCallback(mCb);
            }
            sessionToken = freshToken;
            mediaController = new MediaController(musicService, sessionToken);
            transportControls = mediaController.getTransportControls();
            if (started) {
                mediaController.registerCallback(mCb);
            }
        }
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }
    };

    private Notification createNotification() {
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
