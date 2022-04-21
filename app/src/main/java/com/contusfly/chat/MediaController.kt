package com.contusfly.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.contus.flycommons.Constants
import com.contus.flycommons.LogMessage
import com.contusfly.R
import com.contusfly.utils.ChatMessageUtils.getFormattedTime
import com.contusfly.utils.MediaDetailUtils.getMediaDuration
import com.contusflysdk.utils.Utils
import java.io.File
import java.util.*

/**
 * The Class MediaController which used to care of the medias in the chat view
 *
 * @author ContusTeam <developers></developers>@contus.in>
 * @version 2.0
 */
class MediaController(private val context: Context) {
    /*
     * AudioManager
     */
    private val mAudioManager: AudioManager
    private var isSender = false

    /**
     * Local file path
     */
    private var filePath: String? = null

    /**
     * Local file duration
     */
    private var duration: Long? = null

    /**
     * Play button view
     */
    private var imgPlay: ImageView? = null
    private var doesSentAudio = false

    /**
     * The seek bar to display in the Audio play.
     */
    private var seekBar: SeekBar? = null

    /**
     * Current position of the seek bar
     */
    private var currentPosition = 0

    /**
     * Current play position
     */
    private var timeConsumed = 0

    /**
     * The updated progress.
     */
    private var updatedProgress = 0

    private var progressMilliSeconds = 100 // progress bar will be updated for every 100 ms

    /**
     * The media player to display in the chat view for play the audio.
     */
    var mediaPlayer: MediaPlayer? = null
        private set

    /**
     * The handler to play the audio.
     */
    private var mHandler: Handler? = null

    /**
     * The txt timer of the audio player.
     */
    private var txtTimer: TextView? = null
    private val mPlayedTime: HashMap<String?, Int>?

    /**
     * The listener of the seek bar.
     */
    private val listener: SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
            updatedProgress = i
            updateSeekBarProgress(i)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            //Override method
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            timeConsumed = updatedProgress
            if (mediaPlayer != null) {
                mediaPlayer!!.seekTo(timeConsumed * progressMilliSeconds)
                currentPosition = timeConsumed * progressMilliSeconds
                txtTimer!!.text = getFormattedTime(timeConsumed / 10)
            }
            seekBar.progress = timeConsumed
            timeConsumed++
        }
    }

    /**
     * The song handler to run continuously when playing the media.
     */
    private val songHandler: ThreadLocal<Runnable> = object : ThreadLocal<Runnable>() {
        override fun initialValue(): Runnable {
            return Runnable {
                timeConsumed = mediaPlayer!!.currentPosition / progressMilliSeconds
                seekBar!!.progress = timeConsumed
                txtTimer!!.text = getFormattedTime(timeConsumed / 10)
                mHandler!!.postDelayed(this.get()!!, progressMilliSeconds.toLong())
            }
        }
    }

    /**
     * Sets the position of the current playing audio from the chat adapter view.
     *
     * message in the chat adapter view.
     */
    var currentAudioPosition = -1

    /**
     * The file path of the last view media.
     */
    private var lastUsedMedia: String? = Constants.EMPTY_STRING

    /**
     * The image view for the last used and play images
     */
    private var imgLastUsed: ImageView? = null
    private val focusChangeListener: AudioManager.OnAudioFocusChangeListener

    /**
     * Sets the media resource. Media path and player
     *
     * @param filePath        the file path
     * @param duration        the file duration
     * @param imgPlayer       the img player
     * @param doesSentMessage Boolean to identify whether the audio message is posted in the
     * chat activity.
     */
    fun setMediaResource(filePath: String?, duration: Long?, imgPlayer: ImageView?, doesSentMessage: Boolean) {
        this.filePath = filePath
        imgPlay = imgPlayer
        this.duration = duration
        doesSentAudio = doesSentMessage
    }

    /**
     * Gets the seeker time in seconds
     * @param filePath
     * @return
     */
    fun getPlayedTimeOfTrackInSecs(filePath: String?): Int {
        return if (mPlayedTime != null && mPlayedTime.containsKey(filePath)) {
            mPlayedTime[filePath]!!
        } else 0
    }

    /**
     * Sets the media seek bar.
     *
     * @param seekBar the new media seek bar
     */
    fun setMediaSeekBar(seekBar: SeekBar?) {
        this.seekBar = seekBar
    }

    /**
     * Sets the media timer.
     *
     * @param txtTimer Media timer text view
     */
    fun setMediaTimer(txtTimer: TextView?) {
        this.txtTimer = txtTimer
    }

    /**
     * Get the file path from the player
     *
     * @param doesSentMessage Boolean to identify whether the audio message is posted in the
     * chat activity.
     */
    fun handlePlayer(doesSentMessage: Boolean) {
        try {
            if (filePath!!.isNotEmpty()) {
                isSender = doesSentMessage
                val file = File(filePath)
                if (file.exists() && mediaPlayer != null) {
                    handlePlayerOperations()
                }
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(context, Uri.parse(filePath))
                    mHandler = Handler(Looper.getMainLooper())
                }
                if (requestAudioFocus(focusChangeListener, AudioManager.USE_DEFAULT_STREAM_TYPE,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)) {
                    mediaPlayerListener()
                }
            }
        } catch (e: Exception) {
            LogMessage.e(Constants.TAG, e)
        }
    }

    /**
     * Checks the player is playing or not
     *
     * @param imgPlay         the img play
     * @param seekBar         the seek bar
     * @param txtTimer        the txt timer
     * @param position        the position
     */
    fun checkStateOfPlayer(imgPlay: ImageView, seekBar: SeekBar, txtTimer: TextView?,
                           position: Int) {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying
                && position == currentAudioPosition) {
                this.imgPlay = imgPlay
                this.seekBar = seekBar
                this.txtTimer = txtTimer
               imgPlay.setImageResource(R.drawable.ic_pause_audio_recipient)
                seekBar.max = (mediaPlayer!!.duration / progressMilliSeconds)
                seekBar.setOnSeekBarChangeListener(listener)
                if (mHandler != null && songHandler.get() != null) {
                    mHandler!!.removeCallbacks(songHandler.get()!!)
                    mHandler!!.post(songHandler.get()!!)
                }
            }
        } catch (e: Exception) {
            LogMessage.e(Constants.TAG, e)
        }
    }

    /**
     * Stop the player of the Media player.
     */
    fun stopPlayer() {
        try {
            if (imgPlay != null) {
                imgPlay!!.setImageResource(R.drawable.ic_play_audio_recipient)
            }
            if (txtTimer != null) txtTimer!!.text = Utils.returnEmptyStringIfNull(getMediaDuration(context, duration))
            currentPosition = 0
            timeConsumed = 0
            updatedProgress = 0
            abandonAudioFocus()
            if (seekBar != null) seekBar!!.progress = 0
            if (mediaPlayer != null) {
                mediaPlayer!!.reset()
                mediaPlayer!!.release()
                mediaPlayer = null
            }
            if (mHandler != null && songHandler.get() != null) mHandler!!.removeCallbacks(songHandler.get()!!)
        } catch (e: Exception) {
            LogMessage.e(Constants.TAG, e)
        }
    }

    fun pausePlayer() {
        if (mediaPlayer!!.isPlaying && lastUsedMedia.equals(filePath, ignoreCase = true)) {
            currentPosition = mediaPlayer!!.currentPosition
            mediaPlayer!!.pause()
            imgPlay!!.setImageResource(R.drawable.ic_play_audio_recipient)
            mHandler!!.removeCallbacks(songHandler.get()!!)
        }
    }

    /**
     * Handle the player operations => pause => start => release
     */
    @SuppressLint("NewApi", "ClickableViewAccessibility")
    private fun handlePlayerOperations() {
        if (mediaPlayer!!.isPlaying && lastUsedMedia.equals(filePath, ignoreCase = true)) {
            currentPosition = mediaPlayer!!.currentPosition
            mediaPlayer!!.pause()
            abandonAudioFocus()
            imgPlay!!.setImageResource(R.drawable.ic_play_audio_recipient)
            if (txtTimer != null) txtTimer!!.text = Utils.returnEmptyStringIfNull(getMediaDuration(context, duration))
            mHandler!!.removeCallbacks(songHandler.get()!!)
        } else if (!mediaPlayer!!.isPlaying && lastUsedMedia.equals(filePath, ignoreCase = true)) {
            if (requestAudioFocus(focusChangeListener, AudioManager.USE_DEFAULT_STREAM_TYPE,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)) {
                mHandler!!.post(songHandler.get()!!)
                mediaPlayer!!.start()
                mediaPlayer!!.seekTo(currentPosition)
                mediaPlayer!!.seekTo((mediaPlayer!!.currentPosition + Constants.ONE_SECOND).toInt())
                imgPlay!!.setImageResource(R.drawable.ic_pause_audio_recipient)
            }
        } else {
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
            timeConsumed = 0
            mHandler!!.removeCallbacks(songHandler.get()!!)
        }
    }

    /**
     * mediaPlayerListener Check the media player listener
     *
     */
    private fun mediaPlayerListener() {
        try {
            mediaPlayer!!.setOnPreparedListener { mp: MediaPlayer ->
                lastUsedMedia = filePath
                if (imgLastUsed != null) imgLastUsed!!.setImageResource(R.drawable.ic_play_audio_sender)
                mHandler!!.post(songHandler.get()!!)
                imgPlay!!.setImageResource(R.drawable.ic_pause_audio_recipient)
                getActivity(context)!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val maxDuration = mediaPlayer!!.duration / progressMilliSeconds
                timeConsumed = if (seekBar!!.max == 100 ) seekBar!!.progress * (maxDuration/100) else seekBar!!.progress
                seekBar!!.max = maxDuration
                imgLastUsed = imgPlay
                mediaPlayer!!.seekTo((timeConsumed * progressMilliSeconds))
                currentPosition = mediaPlayer!!.currentPosition
                seekBar!!.setOnSeekBarChangeListener(listener)
                mp.start()
            }
            mediaPlayer!!.setOnCompletionListener { mp: MediaPlayer ->
                mp.release()
                resetAudioPlayer()
            }
        } catch (e: Exception) {
            LogMessage.e(e)
        }
    }

    /**
     * Reset the audio player to the initial state and releases the resources
     * currently associated with this MediaPlayer object.
     */
    fun resetAudioPlayer() {
        if (mediaPlayer != null) mediaPlayer!!.release()
        mediaPlayer = null
        lastUsedMedia = Constants.EMPTY_STRING
        imgLastUsed = null
        imgPlay!!.setImageResource(R.drawable.ic_play_audio_recipient)
        seekBar!!.progress = 0
        mHandler!!.removeCallbacks(songHandler.get()!!)
        abandonAudioFocus()
        timeConsumed = 0
        txtTimer!!.text = Utils.returnEmptyStringIfNull(getMediaDuration(context, duration))
        currentAudioPosition = -1
    }

    /**
     * Request audio focus.
     * Send a request to obtain the audio focus
     *
     * @param focusChangeListener the listener to be notified of audio focus changes
     * @param streamType          the main audio stream type affected by the focus request
     * @param audioFocusGain      the focus gain
     * @return true if AUDIOFOCUS_REQUEST_GRANTED otherwise false
     */
    private fun requestAudioFocus(focusChangeListener: AudioManager.OnAudioFocusChangeListener,
                                  streamType: Int, audioFocusGain: Int): Boolean {
        val r: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.requestAudioFocus(AudioFocusRequest.Builder(audioFocusGain)
                .setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(streamType).build())
                .setOnAudioFocusChangeListener(focusChangeListener).build())
        } else mAudioManager.requestAudioFocus(focusChangeListener, streamType, audioFocusGain)
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(focusChangeListener)
    }

    /**
     * Used to updated file progress based on file path
     * @param seekBarProgress
     */
    fun updateSeekBarProgress(seekBarProgress: Int) {
        if (mPlayedTime != null) {
            mPlayedTime[filePath] = seekBarProgress
            Log.d("TAG", "mPlayedTime: filePath: $filePath")
        }
    }

    companion object {

        private fun getActivity(context: Context?): Activity? {
            if (context == null) return null
            if (context is Activity) return context
            return if (context is ContextWrapper) getActivity(context.baseContext) else null
        }
    }

    /**
     * Instantiates a new media controller.
     */
    init {
        mPlayedTime = HashMap()
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusChangeListener = AudioManager.OnAudioFocusChangeListener { }
    }
}