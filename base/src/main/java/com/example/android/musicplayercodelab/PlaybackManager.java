/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.musicplayercodelab;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;

import static android.media.MediaPlayer.OnCompletionListener;

/**
 * Handles media playback using a {@link MediaPlayer}.
 */
class PlaybackManager
        implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {

    private final Context mContext;
    private int mState;
    private boolean mPlayOnFocusGain;
    private volatile MediaMetadataCompat mCurrentMedia;

    private MediaPlayer mMediaPlayer;

    private final Callback mCallback;
    private final AudioManager mAudioManager;

    public PlaybackManager(Context context, Callback callback) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mCallback = callback;
    }

    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    public MediaMetadataCompat getCurrentMedia() {
        return mCurrentMedia;
    }

    public String getCurrentMediaId() {
        return mCurrentMedia == null ? null : mCurrentMedia.getDescription().getMediaId();
    }

    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    public void play(MediaMetadataCompat metadata) {
        String mediaId = metadata.getDescription().getMediaId();
        boolean mediaChanged = (mCurrentMedia == null || !getCurrentMediaId().equals(mediaId));

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setWakeMode(
                    mContext.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnCompletionListener(this);
        } else {
            if (mediaChanged) {
                mMediaPlayer.reset();
            }
        }

        if (mediaChanged) {
            mCurrentMedia = metadata;
            try {
                mMediaPlayer.setDataSource(
                        mContext.getApplicationContext(),
                        Uri.parse(MusicLibrary.getSongUri(mediaId)));
                mMediaPlayer.prepare();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (tryToGetAudioFocus()) {
            mPlayOnFocusGain = false;
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
            updatePlaybackState();
        } else {
            mPlayOnFocusGain = true;
        }
    }

    public void pause() {
        if (isPlaying()) {
            mMediaPlayer.pause();
            mAudioManager.abandonAudioFocus(this);
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        updatePlaybackState();
    }

    public void stop() {
        mState = PlaybackStateCompat.STATE_STOPPED;
        updatePlaybackState();
        // Give up Audio focus
        mAudioManager.abandonAudioFocus(this);
        // Relax all resources
        releaseMediaPlayer();
    }

    /**
     * Try to get the system audio focus.
     */
    private boolean tryToGetAudioFocus() {
        int result =
                mAudioManager.requestAudioFocus(
                        this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Called by AudioManager on audio focus changes. Implementation of {@link
     * AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        boolean gotFullFocus = false;
        boolean canDuck = false;
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            gotFullFocus = true;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
        }

        if (gotFullFocus || canDuck) {
            if (mMediaPlayer != null) {
                if (mPlayOnFocusGain) {
                    mPlayOnFocusGain = false;
                    mMediaPlayer.start();
                    mState = PlaybackStateCompat.STATE_PLAYING;
                    updatePlaybackState();
                }
                float volume = canDuck ? 0.2f : 1.0f;
                mMediaPlayer.setVolume(volume, volume);
            }
        } else if (mState == PlaybackStateCompat.STATE_PLAYING) {
            mMediaPlayer.pause();
            mState = PlaybackStateCompat.STATE_PAUSED;
            updatePlaybackState();
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        stop();
    }

    /**
     * Releases resources used by the service for playback.
     */
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        if (isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    private void updatePlaybackState() {
        if (mCallback == null) {
            return;
        }
        PlaybackStateCompat.Builder stateBuilder =
                new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        stateBuilder.setState(
                mState, getCurrentStreamPosition(), 1.0f, SystemClock.elapsedRealtime());
        mCallback.onPlaybackStatusChanged(stateBuilder.build());
    }

    public interface Callback {
        void onPlaybackStatusChanged(PlaybackStateCompat state);
    }
}
