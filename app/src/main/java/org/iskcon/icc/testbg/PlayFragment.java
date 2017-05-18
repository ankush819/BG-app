package org.iskcon.icc.testbg;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashankar on 5/12/17.
 */

public class PlayFragment extends Fragment {

    private static final String TAG = "Ankush" + PlayFragment.class.getSimpleName();
    private Button previousButton;
    private Button nextButton;
    private Button playButton;

    private PlaybackState playBackState;
    private MediaBrowser mediaBrowser;
    private MediaController.TransportControls transportControls;
    private MediaController mediaController;

    private PlayAdapater playAdapter;

    private MediaBrowser.ConnectionCallback connectionCallback = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");

            if (mediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session Token");
            }

            mediaController = new MediaController(getActivity(),
                    mediaBrowser.getSessionToken());
            transportControls = mediaController.getTransportControls();
            mediaController.registerCallback(sessionCallback);

            getActivity().setMediaController(mediaController);
            playBackState = mediaController.getPlaybackState();

            Log.d(TAG, "connectionCallback - onConnected - playbackState is " + playBackState);
            List<MediaSession.QueueItem> queue = mediaController.getQueue();
            if (queue != null) {
                playAdapter.clear();
                playAdapter.addAll(queue);
                playAdapter.notifyDataSetChanged();
            }
            onPlayBackStateChanged(playBackState);
        }
    };

    private MediaController.Callback sessionCallback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            if (state == null) {
                return;
            }
            Log.d(TAG, "sessionCallback - onPlaybackstatechanged - playBackState is " + state);
            playBackState = state;
            PlayFragment.this.onPlayBackStateChanged(state);
        }

        @Override
        public void onQueueChanged(@Nullable List<MediaSession.QueueItem> queue) {
            Log.d(TAG, "onQueueChanged");
        }
    };

    public static PlayFragment newInstance() {
        Log.d(TAG, "newInstance");
        return new PlayFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        previousButton = (Button) rootView.findViewById(R.id.skipToPrevious);
        previousButton.setEnabled(false);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToPrevious();
            }
        });

        playButton = (Button) rootView.findViewById(R.id.playPause);
        playButton.setEnabled(true);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "playPauseClicked");
                Log.d(TAG, "onClick - playPause - playbackState is " + playBackState);
                Log.d(TAG, "onClick - playPause - playback for STATE PLAYING is " + PlaybackState.STATE_PLAYING);
                //pauseMedia();
                int state = playBackState == null? PlaybackState.STATE_NONE : playBackState.getState();
                if (state == PlaybackState.STATE_NONE ||
                        state == PlaybackState.STATE_PAUSED ||
                        state == PlaybackState.STATE_STOPPED) {
                    playMedia();
                } else if (state == PlaybackState.STATE_PLAYING) {
                    pauseMedia();
                }
            }
        });

        nextButton = (Button) rootView.findViewById(R.id.skipToNext);
        nextButton.setEnabled(false);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToNext();
            }
        });

        playAdapter = new PlayAdapater(getActivity());

        ListView listView = (ListView) rootView.findViewById(R.id.chapterListView);
        listView.setAdapter(playAdapter);
        listView.setFocusable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaSession.QueueItem item = playAdapter.getItem(position);
                transportControls.skipToQueueItem(item.getQueueId());
            }
        });

        mediaBrowser = new MediaBrowser(getActivity(),
                new ComponentName(getActivity(), MusicService.class),
                connectionCallback, null);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mediaBrowser != null) {
            mediaBrowser.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    private void onPlayBackStateChanged(PlaybackState state) {
        //TODO Setting the active QueueItem ID
        Log.d(TAG, "in onPlayBackStateChanged and the state is " + state);

        previousButton.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        nextButton.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
    }

    private void playMedia() {
        Log.d(TAG, "playMedia");
        if (transportControls != null) {
            transportControls.play();
        }
    }

    private void pauseMedia() {
        Log.d(TAG, "pauseMedia");
        if (transportControls != null) {
            Log.d(TAG, "transportControls not null from pauseMedia");
            Log.d(TAG, "My playback state is " + playBackState);
            transportControls.pause();
        }
    }

    private void skipToPrevious() {
        Log.d(TAG, "skipToPrevious");
        transportControls.skipToPrevious();
    }

    private void skipToNext() {
        Log.d(TAG, "skipToNext");
        transportControls.skipToNext();
    }

    private static class PlayAdapater extends ArrayAdapter<MediaSession.QueueItem>  {

        public PlayAdapater(Context context) {
            super(context, R.layout.chapter_item, new ArrayList<MediaSession.QueueItem>());
            Log.d(TAG, "playAdapater newInstance");
        }

        static class ViewHolder {
            TextView mTitleView;
            TextView mDescriptionView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getView");
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.chapter_item, parent, false);
                holder = new ViewHolder();
                holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
                holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MediaSession.QueueItem item = getItem(position);
            holder.mTitleView.setText(item.getDescription().getTitle());
            if (item.getDescription().getDescription() != null) {
                holder.mDescriptionView.setText(item.getDescription().getDescription());
            }

            return convertView;
        }
    }
}
