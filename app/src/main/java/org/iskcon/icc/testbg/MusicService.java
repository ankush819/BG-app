package org.iskcon.icc.testbg;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.IntDef;
import android.util.Log;

import org.iskcon.icc.testbg.utils.MusicProvider;
import org.iskcon.icc.testbg.utils.QueueHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Created by ashankar on 5/11/17.
 */

public class MusicService extends MediaBrowserService implements PlayBack.Callback {

    private static final String TAG = "Ankush" + MusicService.class.getSimpleName();

    public static final String ACTION_CMD = "org.iskcon.icc.testbg.mediabrowserservice.ACTION_CMD";
    public static final String CMD_NAME = "CMD_NAME";
    public static final String CMD_PAUSE = "CMD_PAUSE";

    private MediaSession mediaSession;
    private PlayBack playBack;
    private List<MediaSession.QueueItem> queueItems;
    private List<MediaSession.QueueItem> playingQueue;
    private MusicProvider musicProvider;
    private int currentPlayingIndexOnQueue;
    private boolean serviceStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        playingQueue = new ArrayList<>();
        musicProvider = new MusicProvider();
        mediaSession = new MediaSession(this, "MusicService");
        setSessionToken(mediaSession.getSessionToken());
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        playBack = new PlayBack(this, musicProvider);
        playBack.setState(PlaybackState.STATE_NONE);
        playBack.setCallback(this);
        playBack.start();

        //TODO : Set Session Activity & notification manager

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(pi);

        updatePlaybackState(null);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    if (playBack != null && playBack.isPlaying()) {
                        handlePauseRequest();
                    }
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        //If we return null no one call connect to the service
        Log.d(TAG, "onGetRoot");
        return new BrowserRoot("", null);
    }

    @Override
    public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
        Log.d(TAG, "onLoadChildren MusicService class");
        if (!musicProvider.isInitialized()) {
            // buildMedia returns boolean. OnSuccess it caches the content in a collectionList in musicProvider
            // onSuccess we call the loadChildrenImpl which it turn uses a MusicProvider method to get stuff done
            musicProvider.buildQueue();
            loadChildrenImpl(result);
            /*if (musicProvider.buildQueue()) {
                Log.d(TAG, "musicProvider is not initialized. Called buildQueue and it returned true");
                loadChildrenImpl(result);
            } else {
                updatePlaybackState("Error no metadata");
                result.sendResult(Collections.<MediaItem>emptyList());
            }*/
        } else {
            //loadChildrenImpl(result);
        }

    }

    private void loadChildrenImpl(Result<List<MediaItem>> result) {
        Log.d(TAG, "loadChildrenImpl - Building the metadata and adding items");
        List<MediaItem> mediaItems = new ArrayList<>();

        for (MediaMetadata track : musicProvider.getChapters()) {
            MediaItem item = new MediaItem(track.getDescription(), MediaItem.FLAG_PLAYABLE);
            mediaItems.add(item);
        }
        Log.d(TAG, "loadChildrenImpl - My mediaItems are " + mediaItems);
        result.sendResult(mediaItems);
    }

    @Override
    public void onCompletion() {
        Log.d(TAG, "onCompletion from MusicService fired");
        Log.d(TAG, "the current Index is " + currentPlayingIndexOnQueue);
        Log.d(TAG, "The playing queue is " + playingQueue);
        if (playingQueue != null && !playingQueue.isEmpty()) {
            currentPlayingIndexOnQueue++;
            if (currentPlayingIndexOnQueue >= playingQueue.size()) {
                currentPlayingIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            handlePauseRequest();
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        Log.d(TAG, "onPlaybackStatusChanged");
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {

    }

    private final class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "onPlay MediaSessionCallBack");

            if (playingQueue == null || !playingQueue.isEmpty()) {
                //No playing items exist. Fetch it from the QueueHelper
                //TODO : Implement the logic for the same
            }

            if (playingQueue != null && !playingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.d(TAG, "onPlayFromMediaID");

            playingQueue = QueueHelper.getQueue(mediaId, musicProvider);
            mediaSession.setQueue(playingQueue);

            if(playingQueue != null && !playingQueue.isEmpty()) {
                currentPlayingIndexOnQueue = QueueHelper.getMusicIndexOnQueue(playingQueue, mediaId);
                if (currentPlayingIndexOnQueue < 0)  {
                    Log.d(TAG, "Cannot find the mediaId");
                } else {
                    handlePlayRequest();
                }
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "MediaSessionCallback class - pause");
            handlePauseRequest();
        }

        @Override
        public void onSkipToPrevious() {
            currentPlayingIndexOnQueue--;
            if (playingQueue != null && currentPlayingIndexOnQueue < 0) {
                currentPlayingIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(currentPlayingIndexOnQueue, playingQueue)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToNext() {
            currentPlayingIndexOnQueue++;
            if (playingQueue != null && currentPlayingIndexOnQueue >= playingQueue.size()) {
                currentPlayingIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(currentPlayingIndexOnQueue, playingQueue)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (playingQueue != null && !playingQueue.isEmpty()) {
                currentPlayingIndexOnQueue = QueueHelper.getMusicIndexOnQueue(playingQueue, String.valueOf(id));
                handlePlayRequest();
            }
        }
    }

    public void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest");

        if (!serviceStarted) {
            startService(new Intent(getApplicationContext(), MusicService.class));
            serviceStarted = true;
        }

        if(!mediaSession.isActive()) {
            mediaSession.setActive(true);
        }

        if(QueueHelper.isIndexPlayable(currentPlayingIndexOnQueue, playingQueue)) {
            //updateMetadata missing here. Barebones playback for now.
            playBack.play(this, playingQueue.get(currentPlayingIndexOnQueue));
        }
    }

    public void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest");
        playBack.pause();
    }

    private void handleStopRequest(String withError) {
        playBack.stop(true);
        // reset the delayed stop handler.

        //TODO : Implement delayed handler etc.,
        updatePlaybackState(withError);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        serviceStarted = false;
    }

    private void updatePlaybackState(String error) {
        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (playBack != null && playBack.isConnected()) {
            position = playBack.getCurrentStreamPosition();
        }

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());

        int state = playBack.getState();

        if (error != null) {
            stateBuilder.setErrorMessage(error);
            state = PlaybackState.STATE_ERROR;
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        if (QueueHelper.isIndexPlayable(currentPlayingIndexOnQueue, playingQueue)) {
            MediaSession.QueueItem item = playingQueue.get(currentPlayingIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mediaSession.setPlaybackState(stateBuilder.build());
        //TODO Start Notification
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (playingQueue == null || playingQueue.isEmpty()) {
            return actions;
        }
        if (playBack.isPlaying()) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (currentPlayingIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (currentPlayingIndexOnQueue < playingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }


}
