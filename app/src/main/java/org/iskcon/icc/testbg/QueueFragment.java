package org.iskcon.icc.testbg;

import android.app.Fragment;
import android.content.ComponentName;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.iskcon.icc.testbg.utils.QueueHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashankar on 5/10/17.
 */

public class QueueFragment extends Fragment {

    private static final String TAG = "Ankush_" + QueueFragment.class.getSimpleName();
    private QueueAdapter queueAdapter;
    private MediaBrowser mediaBrowser;

    private String mediaId;

    public static interface FragmentDataHelper {
        void onMediaItemSelected(MediaBrowser.MediaItem mediaItem);
    }

    private MediaBrowser.SubscriptionCallback subscriptionCallback = new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowser.MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded");
            queueAdapter.clear();
            queueAdapter.notifyDataSetInvalidated();
            for (MediaBrowser.MediaItem item : children) {
                queueAdapter.add(item);
            }
            queueAdapter.notifyDataSetChanged();
        }
    };

    private MediaBrowser.ConnectionCallback connectionCallback = new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");

            if (mediaId == null) {
                mediaId = mediaBrowser.getRoot();
            }
            mediaBrowser.subscribe(mediaId, subscriptionCallback);

            if (mediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session Token");
            }

            //MediaController mediaController = null;
            //TODO Fix the getSampleTest Queue to use the real method once it is created
            //queueAdapter.addAll(QueueHelper.getSampleTestQueue());
            MediaController mediaController = new MediaController(
                        getActivity(), mediaBrowser.getSessionToken());
            getActivity().setMediaController(mediaController);
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
            Log.d(TAG, "Connection Failed because ");
        }
    };

    public static QueueFragment newInstance() {
        Log.d(TAG, "newInstance");
        QueueFragment queueFragment = new QueueFragment();
        return queueFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        View mediaControls = rootView.findViewById(R.id.mediaControls);
        mediaControls.setVisibility(View.GONE);

        queueAdapter = new QueueAdapter(getActivity());
        ListView listView = (ListView) rootView.findViewById(R.id.chapterListView);
        listView.setAdapter(queueAdapter);
        listView.setFocusable(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaBrowser.MediaItem mediaItem = queueAdapter.getItem(position);
                try {
                    FragmentDataHelper listener = (FragmentDataHelper) getActivity();
                    listener.onMediaItemSelected(mediaItem);
                } catch (ClassCastException ex){
                    Log.e(TAG, "Exception when casting to FragmentDataHelper");
                }
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
        if(mediaBrowser != null) {
            mediaBrowser.connect();
        }
        Log.d(TAG, "onResume " + mediaBrowser.isConnected());
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
