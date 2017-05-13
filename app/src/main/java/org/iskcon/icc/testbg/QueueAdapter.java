package org.iskcon.icc.testbg;

import android.app.Activity;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ashankar on 5/10/17.
 */

public class QueueAdapter extends ArrayAdapter<MediaBrowser.MediaItem> {

    private static final String TAG = "Ankush_" + QueueAdapter.class.getSimpleName();

    public QueueAdapter(Activity context) {
        //TODO Populate the ArrayList with the Chapter Metadata information
        super(context, R.layout.chapter_item, new ArrayList<MediaBrowser.MediaItem>());
        Log.d(TAG, "QueueAdapter");
    }

    @Override
    public int getCount() {
        Log.d(TAG, "My get count is " + super.getCount());
        return super.getCount();
    }

    private static class ViewHolder {
        TextView titleView;
        TextView descriptionView;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView");
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.chapter_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.titleView = (TextView) convertView.findViewById(R.id.title);
            viewHolder.descriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        MediaBrowser.MediaItem item = getItem(position);
        viewHolder.titleView.setText(item.getDescription().getTitle());
        if(item.getDescription().getDescription() != null) {
            viewHolder.descriptionView.setText(item.getDescription().getDescription());
        }
        return convertView;
    }
}
