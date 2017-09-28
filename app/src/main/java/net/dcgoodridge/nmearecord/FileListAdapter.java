package net.dcgoodridge.nmearecord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class FileListAdapter extends ArrayAdapter<NmeaFile> {

    private SimpleDateFormat simpleDateFormat;

    public FileListAdapter(Context context, List<NmeaFile> objects) {
        super(context, 0, objects);
        simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }


    private String computeFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int pos = fileName.lastIndexOf('.');
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        NmeaFile nmeaFile = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        View finalConvertView;
        if (convertView == null) {
            finalConvertView = LayoutInflater.from(getContext()).inflate(R.layout.item_nmea_file, parent, false);
        } else {
            finalConvertView = convertView;
        }
        // Lookup view for data population
        TextView tvName = (TextView) finalConvertView.findViewById(R.id.item_nmea_file_name);
        TextView tvDate = (TextView) finalConvertView.findViewById(R.id.item_nmea_file_date);
        TextView tvSize = (TextView) finalConvertView.findViewById(R.id.item_nmea_file_size);


        long dateModifiedTimestamp = nmeaFile.getFile().lastModified();
        String dateModifiedString = simpleDateFormat.format(new Date(dateModifiedTimestamp));

        // Populate the data into the template view using the data object
        tvName.setText(nmeaFile.getFile().getName());
        tvDate.setText(dateModifiedString);
        tvSize.setText(nmeaFile.getReadableFileSize());
        // Return the completed view to render on screen
        return finalConvertView;
    }


}
