package com.blogspot.yashas003.reco;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton record, pause;
    ImageView folder;
    TextView recordState;
    Chronometer chronometer;

    MediaRecorder mRecorder;
    MediaPlayer mPlayer;
    String fileName = null;

    Handler mHandler = new Handler();

    int lastProgress = 0;
    long timeWhenStopped = 0;
    int RECORD_AUDIO_REQUEST_CODE = 123;

    @SuppressLint({"RestrictedApi", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getPermissionToRecordAudio();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.parseColor("#111111"));
        }

        folder = findViewById(R.id.folder);
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               LayoutListRecord listRecord = new LayoutListRecord();
               listRecord.show(getSupportFragmentManager(), null);
            }
        });

        record = findViewById(R.id.record);
        recordState = findViewById(R.id.record_state);

        pause = findViewById(R.id.pause);
        pause.setVisibility(View.GONE);

        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/quicksand_bold.ttf");
        chronometer = findViewById(R.id.timer_count);
        chronometer.setTypeface(font, Typeface.BOLD);

        record.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                String recordTag = record.getTag().toString();

                if (recordTag.equals("record")) {
                    record.setTag("recording");
                    record.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop));
                    recordState.setText("Recording");

                    pause.setVisibility(View.VISIBLE);
                    startRecording();
                } else {
                    record.setTag("record");
                    record.setImageDrawable(getResources().getDrawable(R.drawable.ic_record));
                    recordState.setText("Record");

                    pause.setTag("pause");
                    pause.setVisibility(View.GONE);
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));

                    stopRecording();
                }
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pauseTag = pause.getTag().toString();

                if (pauseTag.equals("pause")) {
                    pause.setTag("play");
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                    recordState.setText("Paused");

                    pauseRecording();
                } else {
                    pause.setTag("pause");
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
                    recordState.setText("Recording");

                    resumeRecording();
                }
            }
        });
    }

    //Recording Functions===========================================================================
    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/rec.o/Records");
        if (!file.exists()) {
            file.mkdirs();
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM.dd" + "@" + "hh.mm.ss");
        String date = simpleDateFormat.format(new Date());

        fileName =  root.getAbsolutePath() + "/rec.o/Records/" + date + ".mp3";
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(96000);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        lastProgress = 0;
        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        chronometer.start();
    }

    private void pauseRecording() {
        try{
            mPlayer.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mPlayer = null;
        timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
        chronometer.stop();
    }

    private void stopRecording() {
        try{
            mRecorder.stop();
            mRecorder.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mRecorder = null;

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        timeWhenStopped = 0;

        Toast.makeText(this, "Recording saved :)", Toast.LENGTH_SHORT).show();
    }

    private void resumeRecording() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("LOG_TAG", "prepare() failed");
        }

        mPlayer.seekTo(lastProgress);
        seekUpdation();

        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        chronometer.start();
    }


    //Utils=========================================================================================
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seekUpdation();
        }
    };

    private void seekUpdation() {
        if(mPlayer != null){
            int mCurrentPosition = mPlayer.getCurrentPosition() ;
            lastProgress = mCurrentPosition;
        }
        mHandler.postDelayed(runnable, 100);
    }


    //Request for permission========================================================================
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO_REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {

            if (grantResults.length == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "WELCOME :)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "You must give permission to use this app :(", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }
    }
}
