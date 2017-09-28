package net.dcgoodridge.nmearecord;


import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileListActivity extends AppCompatActivity {

    private FileListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        adapter = new FileListAdapter(this, new ArrayList<NmeaFile>());
        refreshList();

        ListView my_listview = (ListView) findViewById(R.id.file_list_listview);
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNames);
        my_listview.setEmptyView(findViewById(R.id.empty_view));
        my_listview.setAdapter(adapter);

        my_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final NmeaFile nmeaFileSelected = adapter.getItem(position);

                // custom dialog
                final Dialog dialog = new Dialog(FileListActivity.this, R.style.AppCompatAlertDialogStyle);
                dialog.setContentView(R.layout.dialog_nmea_file);

                // set the custom dialog components - text, image and button
                LinearLayout llOpen = (LinearLayout) dialog.findViewById(R.id.dialog_nmea_file_open);
                LinearLayout llShare = (LinearLayout) dialog.findViewById(R.id.dialog_nmea_file_share);
                LinearLayout llDelete = (LinearLayout) dialog.findViewById(R.id.dialog_nmea_file_delete);
                TextView tvName = (TextView) dialog.findViewById(R.id.dialog_nmea_file_name);
                tvName.setText(nmeaFileSelected.getFile().getName());

                llOpen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startOpenFile(nmeaFileSelected);
                        dialog.dismiss();
                    }
                });

                llShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startShareFile(nmeaFileSelected);
                        dialog.dismiss();
                    }
                });

                llDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteNmeaFile(nmeaFileSelected);
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

    }


    private void deleteNmeaFile(NmeaFile nmeaFile) {
        boolean deleted = nmeaFile.getFile().delete();
        if (!deleted) Toast.makeText(FileListActivity.this, "File could not be deleted", Toast.LENGTH_SHORT).show();
        refreshList();
    }

    private void refreshList() {
        adapter.clear();
        adapter.addAll(computeNmeaFileListOrdered());
    }


    private void startOpenFile(NmeaFile nmeaFile) {
        Uri uri = FileProvider.getUriForFile(this, "net.dcgoodridge.nmearecord.fileprovider", nmeaFile.getFile());
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //intent.setDataAndType(uri, "application/pdf");
        intent.setDataAndType(uri, "text/plain");
        //intent.setDataAndType(uri, "*/*");
        startActivity(intent);
    }


    private void startOpenFile_old(String filePath) {
        Intent intent = new Intent();
        final Uri uri = FileProvider.getUriForFile(getApplicationContext(), "net.dcgoodridge.nmearecord.fileprovider", new File(filePath));
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        intent.setData(uri);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.setType("application/octet-stream");
        startActivity(intent);
    }


    private void startShareFile(NmeaFile nmeaFile) {
        File file = nmeaFile.getFile();
        Intent shareIntent = new Intent();
        final Uri uri = FileProvider.getUriForFile(getApplicationContext(), "net.dcgoodridge.nmearecord.fileprovider", file);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        //shareIntent.setData(uri);
        shareIntent.setType("application/octet-stream");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        final Intent intent = ShareCompat.IntentBuilder.from(this).setType("application/octet-stream").setSubject(file.getName()).setStream(uri).setChooserTitle(
                "Compartir " +
                file.getName()).createChooserIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);

    }


    private String[] computeFileListNames() {
        List<String> fileList = new ArrayList();
        File recordFolder = RecorderService.getRecordFolder(this);
        if (!recordFolder.exists()) {
            recordFolder.mkdirs();
        }
        File[] listOfFiles = recordFolder.listFiles();
        if (listOfFiles == null) return new String[]{};

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                fileList.add(listOfFiles[i].getName());
            }
        }
        String[] fileListNames = fileList.toArray(new String[fileList.size()]);
        return fileListNames;
    }


    private List<File> computeFileList() {
        List<File> fileList = new ArrayList();

        File recordFolder = RecorderService.getRecordFolder(this);
        if (!recordFolder.exists()) {
            recordFolder.mkdirs();
        }

        File[] listOfFiles = recordFolder.listFiles();
        if (listOfFiles == null) return fileList;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                fileList.add(listOfFiles[i]);
            }
        }
        return fileList;
    }

    private List<NmeaFile> computeNmeaFileList() {
        List<File> fileList = computeFileList();
        List<NmeaFile> nmeaFileList = new ArrayList<>();

        for (File file : fileList) {
            nmeaFileList.add(new NmeaFile(file));
        }

        return nmeaFileList;
    }

    private List<NmeaFile> computeNmeaFileListOrdered() {
        List<NmeaFile> nmeaFileList = computeNmeaFileList();
        Collections.sort(nmeaFileList);
        return nmeaFileList;
    }
}
