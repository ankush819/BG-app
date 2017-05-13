package org.iskcon.icc.testbg;

import android.app.Activity;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity implements QueueFragment.FragmentDataHelper {

    private static final String TAG = "Ankush_" + MainActivity.class.getSimpleName();

    public static Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        context = getApplicationContext();
        setContentView(R.layout.activity_main);
        if(savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, QueueFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem mediaItem) {
        Log.d(TAG, "onMediaItemSelected. It is a user defined function.");
        Log.d(TAG, "My media ID is " + mediaItem.getMediaId());
        getMediaController().getTransportControls().playFromMediaId(mediaItem.getMediaId(), null);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, PlayFragment.newInstance())
                .commit();
    }
}
