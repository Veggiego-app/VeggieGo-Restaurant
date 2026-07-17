package com.veggiego.restaurant

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log

class NewOrderPopupActivity : Activity() {

    private var player: MediaPlayer? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        Log.d(
            "POPUP_TEST",
            "POPUP OPENED"
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {

            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)

        } else {

            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            60,
            120,
            60,
            120
        )

        val title =
            TextView(this)

        title.text =
            "🍔 NEW ORDER"

        title.textSize = 30f

        val accept =
            Button(this)

        accept.text =
            "ACCEPT"

        val reject =
            Button(this)

        reject.text =
            "REJECT"

        layout.addView(title)
        layout.addView(accept)
        layout.addView(reject)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContentView(layout)

        player =
            MediaPlayer.create(
                this,
                R.raw.new_order
            )

        player?.isLooping = true
        player?.start()

        accept.setOnClickListener {

            stopSound()
            finish()
        }

        reject.setOnClickListener {

            stopSound()
            finish()
        }
    }

    private fun stopSound() {

        player?.stop()
        player?.release()
        player = null
    }

    override fun onDestroy() {

        stopSound()

        super.onDestroy()
    }
}