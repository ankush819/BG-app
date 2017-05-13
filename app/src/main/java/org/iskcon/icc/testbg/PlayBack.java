package org.iskcon.icc.testbg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import org.iskcon.icc.testbg.utils.MusicProvider;

import java.io.IOException;

import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;
import static android.media.session.MediaSession.QueueItem;

/**
 * Created by ashankar on 5/11/17.
 */

public class PlayBack implements AudioManager.OnAudioFocusChangeListener, OnCompletionListener,
        OnErrorListener, OnPreparedListener, OnSeekCompleteListener {

    private static final String TAG = "Ankush" + PlayBack.class.getSimpleName();
    private final MusicService musicService;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    public static final float VOLUME_DUCK = 0.2f;
    public static final float VOLUME_NORMAL = 1.0f;

    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;

    private IntentFilter audioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private boolean mAudioNoisyReceiverRegistered;
    private boolean playOnFocusGain;
    private MusicProvider musicProvider;
    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;

    //Broadcast receiver part : For headphones coming off
    private BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    musicService.startService(i);
                }
            }
        }
    };

    private int mState;
    private Callback mCallback;
    private String currentMediaId;
    private int currentPosition;


    public PlayBack(MusicService service, MusicProvider musicProvider) {
        Log.d(TAG, "playback instance");
        this.musicService = service;
        this.musicProvider = musicProvider;
        this.audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }

    public void start() {
        Log.d(TAG, "start from playBack");
    }

    public void stop() {
        //TODO : Implement the method by referring the Playback example
        Log.d(TAG, "stop from playback");
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return mState;
    }

    public boolean isConnected() {
        return true;
    }

    public boolean isPlaying() {
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    public int getCurrentStreamPosition() {
        return mediaPlayer != null ?
                mediaPlayer.getCurrentPosition() : currentPosition;
    }

    public void play(Context context, QueueItem queueItem) {
        //TODO Register Noisy receiver
        Log.d(TAG, "play from playback");
        playOnFocusGain = true;
        tryToGetAudioFocus();
        String mediaId = queueItem.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, currentMediaId);
        if (mediaHasChanged) {
            currentPosition = 0;
            currentMediaId = mediaId;
        }

        if (mState == PlaybackState.STATE_PAUSED && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackState.STATE_STOPPED;

            AssetFileDescriptor afd = context.getResources().openRawResourceFd(
                    Integer.parseInt(queueItem.getDescription().getMediaId()));
            if( afd == null ) {
                return;
            }
            try {
                createMediaPlayerIfNeeded();

                mState = PlaybackState.STATE_PLAYING; //TODO Check if this is right
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

                mediaPlayer.prepareAsync();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }
            } catch (IOException ex) {

            }
        }
    }

    public void pause() {
        if (mState == PlaybackState.STATE_PLAYING) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            //TODO : Call to relax resources
            giveUpAudioFocus();
        }
        mState = PlaybackState.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        //TODO : Call to unregister noisy receiver
    }

    //TODO : Implement seekTo method

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private void tryToGetAudioFocus() {
        if (audioFocus != AUDIO_FOCUSED) {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (audioFocus == AUDIO_FOCUSED) {
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    private void configMediaPlayerState() {
        Log.d(TAG, "configMediaPlayerState. mAudioFocus=" + audioFocus);
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    if (currentPosition == mediaPlayer.getCurrentPosition()) {
                        Log.d(TAG, "Playing music now");
                        mediaPlayer.start();
                        mState = PlaybackState.STATE_PLAYING;
                    } else {
                        mediaPlayer.seekTo(currentPosition);
                        mState = PlaybackState.STATE_BUFFERING;
                    }
                }
                playOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState); //TODO Need to implement this method
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //TODO : Implement this method
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //TODO : Implement this method
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion from playback");
        //When the media player finished playing the current song
        // we need to play the next song
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //TODO : Implement this method
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        configMediaPlayerState();
    }


    private void createMediaPlayerIfNeeded() {
        Log.d(TAG, "createMediaPlayerIfNeeded");
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(musicService.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mediaPlayer.reset();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            musicService.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            musicService.unregisterReceiver(audioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    //TODO : Implement Relax Resources method

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

    }
}
