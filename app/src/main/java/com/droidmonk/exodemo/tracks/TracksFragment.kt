package com.droidmonk.exodemo.tracks


import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.droidmonk.exodemo.R
import com.droidmonk.exodemo.VideoPlayerActivity
import com.droidmonk.exodemo.audio.AudioPlayerActivity
import com.droidmonk.exodemo.audio.AudioService
import com.google.android.exoplayer2.util.MimeTypes.isAudio
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_audio_player.*
import kotlinx.android.synthetic.main.fragment_tracks.*
import java.io.File
import java.util.*


/**
 * A simple [Fragment] subclass.
 */
class TracksFragment : Fragment() {
    private val MY_PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1
    companion object{
        val TYPE_AUDIO_LOCAL=1
        val TYPE_VIDEO_LOCAL=2
        val TYPE_VIDEO_WEB=3
        val TYPE_AUDIO_WEB=4

        val KEY_TYPE="key"


        @JvmStatic
        fun newInstance(type:Int) =
            TracksFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_TYPE, type)
                }
            }
    }


    var trackType:Int=TYPE_AUDIO_LOCAL   //default

    private lateinit var mMediaBrowserCompat: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat


    private val mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            try {
                mMediaBrowserCompat.sessionToken.also{token->
                    mediaController = MediaControllerCompat(
                        activity, // Context
                        token
                    )
                }

                MediaControllerCompat.setMediaController(activity!!, mediaController)

            } catch (e: RemoteException) {
                Log.d("tag","Remote exception")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trackType= arguments?.getInt(KEY_TYPE)?: TYPE_AUDIO_LOCAL

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tracks, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        checkPermission()

        mMediaBrowserCompat = MediaBrowserCompat(
            activity, ComponentName(activity, AudioService::class.java),
            mediaBrowserConnectionCallback, activity?.intent?.extras
        )

        mMediaBrowserCompat.connect()


    }

    private fun setUpTrackList() {

        var adapter=TracksAdapter(getTrackList())
        adapter.setOnClickListener(object : TracksAdapter.OnClickListener{
            override fun onClickAddToPlaylist(track: Track) {

                mediaController.transportControls.playFromUri(Uri.parse(track.path),Bundle().apply { putBoolean("playlist",true) })
               }

            override fun onClick(track: Track) {

                if(isAudio(getMimeType(track.path)))
                {
                    activity?.let {startActivity( AudioPlayerActivity.getCallingIntent(it,track))}
                }
                else
                {
                    startActivity(Intent(activity, VideoPlayerActivity::class.java).putExtra("track",track))
                }
            }
        })
        list.layoutManager= LinearLayoutManager(activity)
        list.adapter=adapter
    }

    private fun getTrackList(): ArrayList<Track>
                = when(trackType)
                {
                    TYPE_AUDIO_LOCAL->getLocalAudio()
                    TYPE_VIDEO_LOCAL->getLocalVideo()
                    TYPE_VIDEO_WEB->getWebVideo()
                    TYPE_AUDIO_WEB->getWebAudio()
                    else->ArrayList()
                }

    private fun getWebVideo():ArrayList<Track> {

        var trackMP4=Track("1","Simple MP4","Artist 1", resources.getString(R.string.media_url_mp4),null,null)
        var trackDASH=Track("2","Simple DASH","Artist 1", resources.getString(R.string.media_url_dash),"mpd",null)

        return arrayListOf(trackMP4,trackDASH)
    }

    private fun getWebAudio():ArrayList<Track> {

        var trackMP4=Track(1,"Guitar solo","Unknown Artist", resources.getString(R.string.media_url_mp4),null,null)
        var trackDASH=Track(1,"Guitar fingerstyle","Unknown Artist", resources.getString(R.string.media_url_dash),null,null)
        var trackMP41=Track(1,"C progression","Unknown Artist", resources.getString(R.string.media_url_mp4),null,null)
        var trackDASH2=Track(1,"Do re mi","Unknown Artist", resources.getString(R.string.media_url_dash),null,null)

        return arrayListOf(trackMP4,trackDASH,trackMP41,trackDASH2)
    }

    private fun getLocalVideo():ArrayList<Track> {
        var trackList:ArrayList<Track> = ArrayList()

        val videoResolver = activity?.contentResolver
        val videoUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val videoCursor = videoResolver?.query(videoUri, null, null, null, null)

        if (videoCursor != null && videoCursor.moveToFirst()) {
            //get columns
            val titleColumn =
                videoCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val idColumn = videoCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val artistColumn =
                videoCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            val dataColumn =
                videoCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
            //add songs to list
            do {
                val thisId = videoCursor.getLong(idColumn)
                val thisTitle = videoCursor.getString(titleColumn)
                val thisArtist = videoCursor.getString(artistColumn)
                val thisPath = videoCursor.getString(dataColumn)
                trackList.add(Track(thisId, thisTitle, thisArtist, thisPath, null, null))
            } while (videoCursor.moveToNext())
        }

        return trackList
    }

    private fun getLocalAudio():ArrayList<Track> {
        var trackList:ArrayList<Track> = ArrayList()

        val musicResolver = activity?.contentResolver
        val musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val musicCursor = musicResolver?.query(musicUri, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            val dataColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
            val albumColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM)
            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)
                val thisPath=musicCursor.getString(dataColumn)

                var mediaDataRetriever= MediaMetadataRetriever()

                mediaDataRetriever.setDataSource(activity,Uri.parse(thisPath))

                var songImage:Bitmap?=null
                mediaDataRetriever.embeddedPicture?.let {

                    val albumArt:ByteArray=mediaDataRetriever.embeddedPicture
                    songImage = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size);
                }
                trackList.add(Track(thisId, thisTitle, thisArtist,thisPath,null,null))
            } while (musicCursor.moveToNext())
        }

        return trackList
    }

    fun checkPermission()
    {
        if (ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            setUpTrackList()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    setUpTrackList()

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    override fun onDestroy() {
        super.onDestroy()

      /*  if(mBound) {
            activity?.unbindService(connection)
            mBound = false
        }*/
    }

/*
    fun buildTransportControls() {
        btn_play.apply {
            setOnClickListener {
                // Since this is a play/pause button, you'll need to test the current state
                // and choose the action accordingly

                val pbState = mediaController.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    btn_play.background=resources.getDrawable(R.drawable.ic_pause)
                    mediaController.transportControls.pause()
                } else {
                    btn_play.background=resources.getDrawable(R.drawable.ic_play)
                    mediaController.transportControls.play()
                }
            }
        }

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }
*/

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {}

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}
    }

}
