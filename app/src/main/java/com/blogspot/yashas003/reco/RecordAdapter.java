package com.blogspot.yashas003.reco;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rexprog.audiowaveseekbar.AudioWaveSeekBar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Record> recordArrayList;
    private MediaPlayer mPlayer;
    private boolean isPlaying = false;
    private int last_index = -1;

    RecordAdapter(Context context, ArrayList<Record> recordArrayList) {
        this.context = context;
        this.recordArrayList = recordArrayList;
    }

    @NonNull
    @Override
    public RecordAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.record_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.ViewHolder holder, int position) {
        setUpData(holder, position);
    }

    private void setUpData(ViewHolder holder, final int position) {

        final Record record = recordArrayList.get(position);

        if (record.isPlaying()) {
            holder.button.setImageResource(R.drawable.ic_pause_light);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.VISIBLE);
            holder.separate.setVisibility(View.GONE);
            holder.seekUpdation(holder);
        } else {
            holder.button.setImageResource(R.drawable.ic_play_light);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.GONE);
            holder.separate.setVisibility(View.VISIBLE);
        }

        holder.audioName.setText(record.getFileName());
        holder.manageSeekBar(holder);

        setDuration(record.getUri(), holder);

        holder.viewToDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                deleteRecord(record.getUri(), position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        FloatingActionButton button;
        AudioWaveSeekBar waveSeekBar;
        ConstraintLayout viewToDelete;
        View separate;
        SeekBar seekBar;
        TextView audioName, duration;
        int lastProgress = 0;
        Handler mHandler = new Handler();
        String recordUri;
        ViewHolder holder;

        ViewHolder(View itemView) {
            super(itemView);

            separate = itemView.findViewById(R.id.seperater);
            button = itemView.findViewById(R.id.play_btn);
            seekBar = itemView.findViewById(R.id.seekBar);
            audioName = itemView.findViewById(R.id.audio_name);
            duration = itemView.findViewById(R.id.duration);
            waveSeekBar = itemView.findViewById(R.id.audioSeekBar);
            viewToDelete = itemView.findViewById(R.id.view_to_delete);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Record record = recordArrayList.get(position);
                    recordUri = record.getUri();

                    if (isPlaying) {
                        stopPlaying();
                        if (position == last_index) {
                            record.setPlaying(false);
                            stopPlaying();
                            notifyItemChanged(position);
                        } else {
                            markAllPaused();
                            record.setPlaying(true);
                            notifyItemChanged(position);
                            startPlaying(record, position);
                            last_index = position;
                        }

                    } else {
                        if (record.isPlaying()) {
                            record.setPlaying(false);
                            stopPlaying();
                        } else {
                            startPlaying(record, position);
                            record.setPlaying(true);
                            seekBar.setMax(mPlayer.getDuration());
                        }
                        notifyItemChanged(position);
                        last_index = position;
                    }
                }
            });
        }

        void manageSeekBar(ViewHolder holder) {

            holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mPlayer != null && fromUser) {
                        mPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        private void markAllPaused() {
            for (int i = 0; i < recordArrayList.size(); i++) {
                recordArrayList.get(i).setPlaying(false);
                recordArrayList.set(i, recordArrayList.get(i));
            }
            notifyDataSetChanged();
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                seekUpdation(holder);
            }
        };

        private void seekUpdation(ViewHolder holder) {
            this.holder = holder;
            if (mPlayer != null) {
                int mCurrentPosition = mPlayer.getCurrentPosition();
                holder.seekBar.setMax(mPlayer.getDuration());
                holder.seekBar.setProgress(mCurrentPosition);
                lastProgress = mCurrentPosition;
            }
            mHandler.postDelayed(runnable, 5);
        }

        private void stopPlaying() {
            try {
                mPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPlayer = null;
            isPlaying = false;
        }

        private void startPlaying(final Record audio, final int position) {
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(recordUri);
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                Log.e("LOG_TAG", "prepare() failed");
            }

            seekBar.setMax(mPlayer.getDuration());
            waveSeekBar.setDuration(mPlayer.getDuration());
            isPlaying = true;

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    audio.setPlaying(false);
                    notifyItemChanged(position);
                }
            });
        }
    }

    @SuppressLint("DefaultLocale")
    private void setDuration(String uri, ViewHolder holder) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, Uri.parse(uri));
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int s = Integer.parseInt(durationStr);
        holder.duration.setText(String.format("%02d:%02d:%02d", (s / 1000) / 3600, (s / 1000) / 60, (s / 1000)));

        holder.waveSeekBar.setColors(Color.parseColor("#eeeeeeee"), Color.parseColor("#eeeeeeee"), Color.parseColor("#eeeeeeee"));
        holder.waveSeekBar.setWaveform((new ByteSamples().getAlphaNumericString(s)).getBytes());
    }

    private void deleteRecord(String uri, int position) {
        File deleteFile = new File(Objects.requireNonNull(Uri.parse(uri).getPath()));

        if (deleteFile.exists()) {

            if (deleteFile.delete()) {
                recordArrayList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount());
                Toast.makeText(context, "Record Deleted :)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Could not delete this record :(", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
