package net.dcgoodridge.nmearecord;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;


public class NmeaListAdapter extends ArrayAdapter<String> {

    private static final int ARRAY_SIZE = 256;

    private int totalAdded = 0;

    private CircularArray<String> buffer = new CircularArray<>(ARRAY_SIZE);

    public NmeaListAdapter(Context context, List<String> objects) {
        super(context, 0, objects);
    }

    @Override
    public String getItem(int position) {
        int index = position % buffer.capacity();
        return buffer.get(index);
    }

    @Override
    public void add(String object) {
        buffer.add(object);
        totalAdded++;
    }

    @Override
    public int getCount() {
        return totalAdded;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        String nmea = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        View finalConvertView;
        if (convertView == null) {
            finalConvertView = LayoutInflater.from(getContext()).inflate(R.layout.item_nmea, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.nmea = (TextView) finalConvertView.findViewById(R.id.item_nmea_nmea);
            finalConvertView.setTag(holder);
        } else {
            finalConvertView = convertView;
        }
        ViewHolder holder = (ViewHolder) finalConvertView.getTag();

        // Populate the data into the template view using the data object
        if (nmea.isEmpty()) {
            holder.nmea.setText("-");
            holder.nmea.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.nmea.setText(nmea);
            holder.nmea.setTypeface(null, Typeface.NORMAL);
        }
        return finalConvertView;
    }

    static class ViewHolder {
        TextView nmea;
    }
}
