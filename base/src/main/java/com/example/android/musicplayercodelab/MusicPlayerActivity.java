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

import android.app.Activity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** An Activity to browse and play media. */
public class MusicPlayerActivity extends AppCompatActivity {

    private BrowseAdapter mBrowserAdapter;
    private ImageButton mPlayPause;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mAlbumArt;
    private ViewGroup mPlaybackControls;

    private MediaMetadataCompat mCurrentMetadata;
    private PlaybackStateCompat mCurrentState;

    // TODO: [1] Remove the following line for playback in a Service
    private PlaybackManager mPlaybackManager;

    // TODO: [1] Uncomment the following block for playback in a Service
    /*
    private MediaBrowserCompat mMediaBrowser;

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    mMediaBrowser.subscribe(mMediaBrowser.getRoot(), mSubscriptionCallback);
                    try {
                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(
                                        MusicPlayerActivity.this, mMediaBrowser.getSessionToken());
                        updatePlaybackState(mediaController.getPlaybackState());
                        updateMetadata(mediaController.getMetadata());
                        mediaController.registerCallback(mMediaControllerCallback);
                        MediaControllerCompat.setMediaController(
                                MusicPlayerActivity.this, mediaController);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    updateMetadata(metadata);
                    mBrowserAdapter.notifyDataSetChanged();
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updatePlaybackState(state);
                    mBrowserAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSessionDestroyed() {
                    updatePlaybackState(null);
                    mBrowserAdapter.notifyDataSetChanged();
                }
            };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(
                        String parentId, List<MediaBrowserCompat.MediaItem> children) {
                    onMediaLoaded(children);
                }
            };
    */

    private void onMediaLoaded(List<MediaBrowserCompat.MediaItem> media) {
        mBrowserAdapter.clear();
        mBrowserAdapter.addAll(media);
        mBrowserAdapter.notifyDataSetChanged();
    }

    private void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        if (item.isPlayable()) {
            // TODO: [2] Remove the following lines for playback in a Service
            MediaMetadataCompat metadata = MusicLibrary.getMetadata(this, item.getMediaId());
            mPlaybackManager.play(metadata);
            updateMetadata(metadata);

            // TODO: [2] Uncomment the following block for playback in a Service
            /*
            MediaControllerCompat.getMediaController(this)
                    .getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
            */
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setTitle(getString(R.string.app_name));
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mBrowserAdapter = new BrowseAdapter(this);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);
                        onMediaItemSelected(item);
                    }
                });

        // Playback controls configuration:
        mPlaybackControls = (ViewGroup) findViewById(R.id.playback_controls);
        mPlayPause = (ImageButton) findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mPlaybackButtonListener);

        mTitle = (TextView) findViewById(R.id.title);
        mSubtitle = (TextView) findViewById(R.id.artist);
        mAlbumArt = (ImageView) findViewById(R.id.album_art);
    }

    @Override
    public void onStart() {
        super.onStart();

        // TODO: [3] Remove the following lines for playback in a Service
        mPlaybackManager =
                new PlaybackManager(
                        this,
                        new PlaybackManager.Callback() {
                            @Override
                            public void onPlaybackStatusChanged(PlaybackStateCompat state) {
                                mBrowserAdapter.notifyDataSetChanged();
                                updatePlaybackState(state);
                            }
                        });
        onMediaLoaded(MusicLibrary.getMediaItems());

        // TODO: [3] Uncomment the following block for playback in a Service
        /*
        mMediaBrowser =
                new MediaBrowserCompat(
                        this,
                        new ComponentName(this, MusicService.class),
                        mConnectionCallback,
                        null);
        mMediaBrowser.connect();
        */
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: [4] Remove the following line for playback in a Service
        mPlaybackManager.stop();

        // TODO: [4] Uncomment the following block for playback in a Service
        /*
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(this);
        if (controller != null) {
            controller.unregisterCallback(mMediaControllerCallback);
        }
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            if (mCurrentMetadata != null) {
                mMediaBrowser.unsubscribe(mCurrentMetadata.getDescription().getMediaId());
            }
            mMediaBrowser.disconnect();
        }
        */
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        mCurrentState = state;
        if (state == null
                || state.getState() == PlaybackStateCompat.STATE_PAUSED
                || state.getState() == PlaybackStateCompat.STATE_STOPPED) {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_black_36dp));
        } else {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_pause_black_36dp));
        }
        mPlaybackControls.setVisibility(state == null ? View.GONE : View.VISIBLE);
    }

    private void updateMetadata(MediaMetadataCompat metadata) {
        mCurrentMetadata = metadata;
        mTitle.setText(metadata == null ? "" : metadata.getDescription().getTitle());
        mSubtitle.setText(metadata == null ? "" : metadata.getDescription().getSubtitle());
        mAlbumArt.setImageBitmap(
                metadata == null
                        ? null
                        : MusicLibrary.getAlbumBitmap(
                                this, metadata.getDescription().getMediaId()));
        mBrowserAdapter.notifyDataSetChanged();
    }

    // An adapter for showing the list of browsed MediaItem's
    private class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        public BrowseAdapter(Activity context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaBrowserCompat.MediaItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaBrowserCompat.MediaItem item = getItem(position);
            int itemState = MediaItemViewHolder.STATE_NONE;
            if (item.isPlayable()) {
                String itemMediaId = item.getDescription().getMediaId();
                int playbackState = PlaybackStateCompat.STATE_NONE;
                if (mCurrentState != null) {
                    playbackState = mCurrentState.getState();
                }
                if (mCurrentMetadata != null
                        && itemMediaId.equals(mCurrentMetadata.getDescription().getMediaId())) {
                    if (playbackState == PlaybackStateCompat.STATE_PLAYING
                            || playbackState == PlaybackStateCompat.STATE_BUFFERING) {
                        itemState = MediaItemViewHolder.STATE_PLAYING;
                    } else if (playbackState != PlaybackStateCompat.STATE_ERROR) {
                        itemState = MediaItemViewHolder.STATE_PAUSED;
                    }
                }
            }
            return MediaItemViewHolder.setupView(
                    (Activity) getContext(), convertView, parent, item.getDescription(), itemState);
        }
    }

    private final View.OnClickListener mPlaybackButtonListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int state =
                            mCurrentState == null
                                    ? PlaybackStateCompat.STATE_NONE
                                    : mCurrentState.getState();
                    if (state == PlaybackStateCompat.STATE_PAUSED
                            || state == PlaybackStateCompat.STATE_STOPPED
                            || state == PlaybackStateCompat.STATE_NONE) {

                        if (mCurrentMetadata == null) {
                            mCurrentMetadata =
                                    MusicLibrary.getMetadata(
                                            MusicPlayerActivity.this,
                                            MusicLibrary.getMediaItems().get(0).getMediaId());
                            updateMetadata(mCurrentMetadata);
                        }

                        // TODO: [5] Remove the following line for playback in a Service
                        mPlaybackManager.play(mCurrentMetadata);

                        // TODO: [5] Uncomment the following block for playback in a Service
                        /*
                        MediaControllerCompat.getMediaController(MusicPlayerActivity.this)
                                .getTransportControls()
                                .playFromMediaId(
                                        mCurrentMetadata.getDescription().getMediaId(), null);
                        */
                    } else {
                        // TODO: [6] Remove the following line for playback in a Service
                        mPlaybackManager.pause();

                        // TODO: [6] Uncomment the following block for playback in a Service
                        /*
                        MediaControllerCompat.getMediaController(MusicPlayerActivity.this)
                                .getTransportControls()
                                .pause();
                        */
                    }
                }
            };
}
