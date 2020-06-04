package de.pollpeter.jonas.rasteuerung

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.Spanned
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    private lateinit var imageScaleAnimation: ObjectAnimator
    private val acceptedChipByteArray = arrayOf(
        226.toByte(),
        183.toByte(),
        136.toByte(),
        27.toByte()
    ) // bytes value from serial number of blue chip
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    NfcAdapter.EXTRA_ADAPTER_STATE,
                    NfcAdapter.STATE_OFF
                )
                when (state) {
                    NfcAdapter.STATE_OFF -> {
                        showNfcEnableToast()
                        statusImage.setImageResource(R.drawable.ic_nfc_off)
                    }
                    NfcAdapter.STATE_TURNING_OFF -> {
                    }
                    NfcAdapter.STATE_ON -> {
                        statusImage.setImageResource(R.drawable.ic_nfc)
                    }
                    NfcAdapter.STATE_TURNING_ON -> {
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!(nfcAdapter as NfcAdapter).isEnabled) {
            showNfcEnableToast()
            statusImage.setImageResource(R.drawable.ic_nfc_off)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            titleScan.text = Html.fromHtml(
                getString(R.string.tag_scannen),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ) as Spanned
        } else {
            titleScan.text = Html.fromHtml(getString(R.string.tag_scannen)) as Spanned
        }
        imageAnimation()
        registerNfcBroadcastReceiver()
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolveIntent(intent)
    }

    private fun registerNfcBroadcastReceiver() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        this.registerReceiver(mReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null) {
            if (!nfcAdapter!!.isEnabled) {
                showNfcEnableToast()
                registerNfcBroadcastReceiver()
            }
            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.unregisterReceiver(mReceiver)
    }

    private fun imageAnimation() {
        imageScaleAnimation = ObjectAnimator.ofPropertyValuesHolder(
            statusImage,
            PropertyValuesHolder.ofFloat("scaleX", 1.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1.3f)
        )
        imageScaleAnimation.duration = 2000
        imageScaleAnimation.repeatMode = ValueAnimator.REVERSE
        imageScaleAnimation.repeatCount = ValueAnimator.INFINITE
        imageScaleAnimation.start()
    }

    private fun resolveIntent(intent: Intent?) {
        val action = intent?.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val tagId = getIntent().getByteArrayExtra(NfcAdapter.EXTRA_ID)
            imageScaleAnimation.cancel()
            imageScaleAnimation.resume()
            if (tagId.toTypedArray().contentEquals(acceptedChipByteArray)) {
                statusImage.setImageResource(R.drawable.ic_check_true)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                statusImage.setImageResource(R.drawable.ic_check_false)
                val handler = Handler()
                handler.postDelayed({
                    statusImage.setImageResource(R.drawable.ic_nfc)
                    imageScaleAnimation.start()
                }, 1500)
            }
        }
    }

    private fun showNfcEnableToast() {
        Toast.makeText(applicationContext, "Bitte aktiviere NFC in den Einstellungen", Toast.LENGTH_LONG).show()
    }
}
