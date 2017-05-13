package org.iskcon.icc.testbg.utils;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.util.Log;

import org.iskcon.icc.testbg.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashankar on 5/12/17.
 */

public class QueueHelper {

    private static final String TAG = "Ankush" + QueueHelper.class.getSimpleName();

    public static List<MediaSession.QueueItem> getQueue(String mediaId, MusicProvider musicProvider) {
        //TODO Implement logic to get playing Queue
        //Talks to MusicProvider to get the task done
        //I guess MusicProvider already has fetched the music and some method will make it available for getQueue
        //SAMPLE METHOD ALERT
        Log.d(TAG, "getQueue");
        int mId1 = R.raw.dun_dun_dun;
        int mId2 = R.raw.rainbow_dun_dun;
        MediaSession.QueueItem queueItem1 = new MediaSession.QueueItem(
                new MediaDescription.Builder()
                        .setTitle("Chapter1").setMediaId(String.valueOf(mId1)).setDescription("ABC").build(),
                MediaBrowser.MediaItem.FLAG_PLAYABLE
        );
        MediaSession.QueueItem queueItem2 = new MediaSession.QueueItem(
                new MediaDescription.Builder()
                        .setTitle("Chapter2").setMediaId(String.valueOf(mId2)).setDescription("ABC").build(),
                MediaBrowser.MediaItem.FLAG_PLAYABLE
        );
        List<MediaSession.QueueItem> queueItems = new ArrayList<>();
        queueItems.add(queueItem1);
        queueItems.add(queueItem2);
        return queueItems;
    }

    public static int getMusicIndexOnQueue(List<MediaSession.QueueItem> queueItems, String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem queueItem : queueItems) {
            if(mediaId.equals(queueItem.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    public static List<MediaBrowser.MediaItem> getSampleTestQueue() {
        //This is a sample method. The real method should get the Music List to popluate the array adapter
        //in the QueueFragment. It should get this music list by a method in Music Provider

        int mId1 = R.raw.dun_dun_dun;
        int mId2 = R.raw.rainbow_dun_dun;
        MediaBrowser.MediaItem queueItem1 = new MediaBrowser.MediaItem(
                new MediaDescription.Builder()
                        .setTitle("Chapter1").setMediaId(String.valueOf(mId1)).setDescription("ABC").build(),
                MediaBrowser.MediaItem.FLAG_PLAYABLE
        );

        MediaBrowser.MediaItem queueItem2 = new MediaBrowser.MediaItem(
                new MediaDescription.Builder()
                        .setTitle("Chapter2").setMediaId(String.valueOf(mId2)).setDescription("XYZ").build(),
                MediaBrowser.MediaItem.FLAG_PLAYABLE
        );
        List<MediaBrowser.MediaItem> queue = new ArrayList<MediaBrowser.MediaItem>();
        queue.add(queueItem1);
        queue.add(queueItem2);
        return queue;
    }

}
