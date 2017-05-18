package org.iskcon.icc.testbg.utils;

import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.util.Log;

import org.iskcon.icc.testbg.BuildConfig;
import org.iskcon.icc.testbg.MainActivity;
import org.iskcon.icc.testbg.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ashankar on 5/12/17.
 */

public class MusicProvider {
    private static final String TAG = "Ankush_MusicProvider";

    private List<MediaMetadata> chapterList;

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }


    private volatile State mCurrentState = State.NON_INITIALIZED;

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public MusicProvider() {
        chapterList = new ArrayList<>();
    }

    public boolean buildQueue() {
        //TODO : Implement
        //Set state to INITIALIZED once it is successful
        List<MediaMetadata> newChapterList = new ArrayList<>();
        Map<String, Integer> chapterMetadata = getChapterMetadataFromRaw();
        if (chapterMetadata.isEmpty()) {
            //WE couldnt build anything. CHECK if the logic is right.
            Log.d(TAG, "buildQueue - empty queue");
            return false;
        }

        for (Map.Entry<String, Integer> map : chapterMetadata.entrySet()) {
            String chapterRawName = map.getKey();
            Integer mediaId = map.getValue();
            newChapterList.add(buildChapterMetadata(chapterRawName, mediaId));
        }
        chapterList = newChapterList;
        Log.d(TAG, "buildQueue - chapterList is " + chapterList);
        mCurrentState = State.INITIALIZED;
        return true;
    }

    public Iterable<MediaMetadata> getChapters() {
        //TODO : Implement
        //getChapters will fetch the items from a cache (collections) variable from the musicProvider
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return chapterList;
    }

    private MediaMetadata buildChapterMetadata(String title, int mediaId) {
        //TODO : Get the title according to the MediaID from somewhere

        return new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, String.valueOf(mediaId))
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "")
                .build();
    }

    private Map<String, Integer> getChapterMetadataFromRaw() {
        Map<String, Integer> chapterMetadata = new HashMap<>();
        for (String chapterName : getChapterNamesFromRaw()) {
            //Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "res/raw/" + chapterName);
            int mediaId = MainActivity.context.getResources().getIdentifier(chapterName, "raw", MainActivity.context.getPackageName());
            Log.d(TAG, "getChapterMetadataFromRaw - mediaId = " + mediaId);
            chapterMetadata.put(chapterName, mediaId);
        }

        return chapterMetadata;
    }

    private List<String> getChapterNamesFromRaw(){
        List<String> chapterNames = new ArrayList<>();
        Field[] fields = R.raw.class.getFields();
        Pattern pattern = Pattern.compile("gita");
        Matcher matcher;
        for(int count=0; count < fields.length; count++){
            matcher = pattern.matcher(fields[count].getName());
            if (matcher.find()) {
                chapterNames.add(fields[count].getName());
            }
        }
        Log.d(TAG, "getChapterNames - " + chapterNames);
        return chapterNames;
    }
}
