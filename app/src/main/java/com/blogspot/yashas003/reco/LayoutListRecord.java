package com.blogspot.yashas003.reco;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;

public class LayoutListRecord extends BottomSheetDialogFragment {
    private ArrayList<Record> recordArraylist;
    private RecyclerView recyclerView;
    private TextView noRecords;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.record_list_layout, container, false);

        recordArraylist = new ArrayList<>();
        noRecords = v.findViewById(R.id.no_records);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        recyclerView = v.findViewById(R.id.record_list);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);

        fetchRecordings();
        return v;
    }

    private void fetchRecordings() {
        String path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/rec.o/Records/";
        File[] files = new File(path).listFiles();

        if (files != null) {
            for (File file : files) {

                String recordingUri = path + file.getName();
                Record record = new Record(recordingUri, file.getName(), false);
                recordArraylist.add(record);
            }
            RecordAdapter recordAdapter = new RecordAdapter(getActivity(), recordArraylist);
            recyclerView.setAdapter(recordAdapter);
            recyclerView.setVisibility(View.VISIBLE);
            noRecords.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            noRecords.setVisibility(View.VISIBLE);
        }
    }
}
