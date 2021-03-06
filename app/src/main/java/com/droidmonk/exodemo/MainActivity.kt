package com.droidmonk.exodemo

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        player=ExoPlayerFactory.newSimpleInstance(this,DefaultTrackSelector())
        player_view.player=player


        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            "ExoDemo")

        val mediaSource:MediaSource=ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(resources.getString(R.string.media_url_mp4)))

        player?.prepare(mediaSource)
        player?.setPlayWhenReady(true)
    }
}
