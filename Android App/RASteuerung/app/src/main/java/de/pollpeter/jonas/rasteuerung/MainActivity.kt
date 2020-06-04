package de.pollpeter.jonas.rasteuerung

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.move_controls.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException


class MainActivity : AppCompatActivity(), WifiHelper.IWifiHelper {

    private lateinit var wifiHelper: WifiHelper
    private lateinit var datagramSocket: DatagramSocket
    private lateinit var sharedPreferences: SharedPreferences
    private val moveCommandTemplate = "L%1dR%2d!"
    private val moveStop = "L0R0!"
    private val sharedPreferenceName = "appdata"
    private val locationPermissionRequestCode = 1
    private val connectSwitchOnChangeListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
            if (!checkGps())
                enableGpsProcess()
            view.isEnabled = false
            mainProgressBar.visibility = View.VISIBLE
            if (isChecked) wifiHelper.connectToCar()
            else {
                wifiHelper.disconnectCar()
                disconnectedByClick = true
            }
        }
    private val lightsSwitchOnChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            ignoreNextLightsChange = true
            toggleLights(isChecked)
        }

    private var speed: Int = 255
    private var carMode:Int = 1
    private var connectedToCar = false
    private var disconnectedByClick = false
    private var ignoreNextLightsChange:Boolean = false
    private var connectCarSwitch: SwitchCompat? = null

    companion object {
        const val TAG = "RASteuerung"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init general
        sharedPreferences = applicationContext.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE)
        wifiHelper = WifiHelper(this, this)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionRequestCode
            )
        }
        if (!checkGps()) {
            enableGpsProcess()
        }

        // init spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.car_modes,
            android.R.layout.simple_spinner_dropdown_item
        )
        modeSelectionSpinner.adapter = adapter
        modeSelectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                controlJoystick.visibility = if (position != 1) View.GONE else View.VISIBLE
                include.visibility = if (position == 1 || position == 3) View.GONE else View.VISIBLE
                if (position == 1) {
                    speedText.text = getString(R.string.speed_template).format(0)
                } else {
                    speedText.text = getString(R.string.speed_template).format( map(speed.toLong(), 0, 255, 0, 100))
                }
                carMode = position+1
                sendMessage("M$carMode.")
            }

        }

        // init toggle light switch
        toggleLightsSwitch.setOnCheckedChangeListener(lightsSwitchOnChangeListener)

        // init buttons
        val listener = View.OnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                when (view.id) {
                    R.id.buttonForward -> sendMessage(moveCommandTemplate.format(speed, speed))
                    R.id.buttonBackward -> sendMessage(moveCommandTemplate.format(-speed, -speed))
                    R.id.buttonLeft -> sendMessage(moveCommandTemplate.format(-speed, speed))
                    R.id.buttonRight -> sendMessage(moveCommandTemplate.format(speed, -speed))
                }
                view.alpha = 0.5F
            } else if (event.action == MotionEvent.ACTION_UP) {
                sendMessage(moveStop)
                view.alpha = 1F
            }
            return@OnTouchListener true
        }
        buttonForward.setOnTouchListener(listener)
        buttonBackward.setOnTouchListener(listener)
        buttonLeft.setOnTouchListener(listener)
        buttonRight.setOnTouchListener(listener)

        // init control joystick
        controlJoystick.setOnMoveListener({ angle, strength ->
            var speed = map(strength.toLong(), 0, 100, 100, 255)
            if (strength == 0) {
                speed = 0
            }
            if (angle > 180) {
                speed = -speed
            }
            sendMessage( "M" + moveCommandTemplate.format(speed, speed))
            speedText.text = getString(R.string.speed_template).format(strength)
        }, 75)

        // init speed control
        speedControl.progress = 100
        speedText.text = getString(R.string.speed_template).format(100) // TODO ist erst auf 255???
        speedControl.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                speedText.text = getString(R.string.speed_template).format(i)
                speed = map(i.toLong(), 0, 100, 0, 255).toInt()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // loop to send distance request and to check for available car
        val handler = Handler()
        val delay = 1000L
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (connectedToCar) {
                    sendMessage("D?")
                    val signalStrength = wifiHelper.getSignalStrength()
                    var imageResource = R.drawable.signal_strength_0
                    when(signalStrength){
                        0->imageResource = R.drawable.signal_strength_0
                        1->imageResource = R.drawable.signal_strength_1
                        2->imageResource = R.drawable.signal_strength_2
                        3->imageResource = R.drawable.signal_strength_3
                        4->imageResource = R.drawable.signal_strength_4

                    }
                    runOnUiThread {
                        signalStrengthImage.setImageResource(imageResource)
                    }
                } else if (!disconnectedByClick) {
                    wifiHelper.scanForCar()
                }
                handler.postDelayed(this, delay)
            }
        }, delay)

        // init udp receiver and receive packets
        try {
            datagramSocket = DatagramSocket(2000)
            datagramSocket.reuseAddress = true
            datagramSocket.soTimeout = 1000
        } catch (e: SocketException) {
            Log.d(TAG, "SocketException: " + e.message)
        }
        Thread {
            try {
                val message = ByteArray(2048)
                val packet = DatagramPacket(message, message.size)
                while (true) {
                    try {
                        datagramSocket.receive(packet)
                        val text = String(message, 0, packet.length)
                        runOnUiThread {
                            try {
                                if (text.startsWith("D")) {
                                    val distance = Integer.parseInt(
                                        text.replace(
                                            "D",
                                            ""
                                        ).replace(".", "")
                                    )
                                    val output =
                                        getString(R.string.distance).format(distance.toString())
                                    buttonForward.isEnabled = distance > 20
                                    buttonForward.alpha = if (buttonForward.isEnabled) 1F else 0.5F
                                    if (distance <= 20) {
                                        carFrontDistanceText.setTextColor(getColor(R.color.appRed))
                                    } else {
                                        carFrontDistanceText.setTextColor(Color.WHITE)
                                    }
                                    output.replace("%1\$s", "")
                                    carFrontDistanceText.text = output
                                } else if (text.startsWith("F")) {
                                    if(ignoreNextLightsChange){
                                        ignoreNextLightsChange = false
                                        return@runOnUiThread
                                    }
                                    val frontLightsOn = text.substring(
                                        text.indexOf("F") + 1,
                                        text.indexOf("R")
                                    ).toInt() == 1
                                    val rearLightsOn = text.substring(
                                        text.indexOf("R") + 1,
                                        text.indexOf("S")
                                    ).toInt() == 1
                                    val statusLightOn = text.substring(
                                        text.indexOf("S") + 1,
                                        text.length
                                    ).toInt() == 1
                                    toggleLightsSwitch.setOnCheckedChangeListener(null)
                                    toggleLightsSwitch.isChecked =
                                        frontLightsOn || rearLightsOn || statusLightOn
                                    toggleLightsSwitch.setOnCheckedChangeListener(lightsSwitchOnChangeListener)
                                }
                            } catch (e: Exception) { }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "SocketTimeoutException: " + e.message)
                    } catch (e: NullPointerException) {
                        Log.d(TAG, "NullPointerException: " + e.message)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Exception: " + e.message)
            }
        }.start()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == locationPermissionRequestCode) if (grantResults.isNotEmpty()) recreate()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val switchItem = menu?.findItem(R.id.connectCarSwitchMenuItem)
        switchItem?.setActionView(R.layout.switch_layout)
        connectCarSwitch = switchItem?.actionView?.findViewById(R.id.connectCarSwitch)
        connectCarSwitch?.setOnCheckedChangeListener(connectSwitchOnChangeListener)
        connectCarSwitch?.isChecked = wifiHelper.isConnectedToCar()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        wifiHelper.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        wifiHelper.unregisterReceiver()
    }

    fun map(
        x: Long,
        in_min: Long,
        in_max: Long,
        out_min: Long,
        out_max: Long
    ): Long {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    private fun enableGpsProcess() {
        GpsUtils(this).turnGPSOn { isGPSEnable: Boolean ->
            if (!isGPSEnable)
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.gps_dialog_title))
                    .setMessage(getString(R.string.gps_dialog_message))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .show()
        }
    }

    private fun checkGps(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return gpsEnabled
    }

    private fun sendMessage(message: String, view: View? = null) {
        mainProgressBar.visibility = View.VISIBLE
        if (view != null) {
            view.isEnabled = false
        }
        Thread {
            val udpSocket = DatagramSocket()
            val messageByteArray = message.toByteArray()
            val datagram = DatagramPacket(
                messageByteArray,
                messageByteArray.size,
                InetAddress.getByName("192.168.4.1"),
                2000
            )
            try {
                udpSocket.send(datagram)
            } catch (e: Exception) {
                Log.d(TAG, "ErrnoException: " + e.message)
            }
            runOnUiThread {
                if (view != null) {
                    view.isEnabled = true
                }
                mainProgressBar.visibility = View.INVISIBLE
            }
        }.start()
    }

    private fun toggleLights(lightsOn: Boolean) {
        if (lightsOn) sendMessage("F1R1S1$", toggleLightsSwitch)
        else sendMessage("F0R0S0$", toggleLightsSwitch)
    }

    private fun setControlButtonsEnabled(enabled: Boolean) {
        buttonForward.isEnabled = enabled
        buttonForward.alpha = if (enabled) 1F else 0.5F
        buttonBackward.isEnabled = enabled
        buttonBackward.alpha = if (enabled) 1F else 0.5F
        buttonLeft.isEnabled = enabled
        buttonLeft.alpha = if (enabled) 1F else 0.5F
        buttonRight.isEnabled = enabled
        buttonRight.alpha = if (enabled) 1F else 0.5F
    }

    private fun setConnectSwitchChecked(isChecked: Boolean) {
        connectCarSwitch?.setOnCheckedChangeListener(null)
        connectCarSwitch?.isChecked = isChecked
        connectCarSwitch?.setOnCheckedChangeListener(connectSwitchOnChangeListener)
    }

    override fun onConnectionAvailable() {
        if (!disconnectedByClick) {
            wifiHelper.connectToCar()
        } else {
            carAvailableStatus.text = getString(R.string.car_available)
            carAvailableStatus.setTextColor(getColor(R.color.appGreen))
        }
    }

    override fun onConnectionNotAvailable() {
        carAvailableStatus.text = getString(R.string.car_not_available)
        carAvailableStatus.setTextColor(getColor(R.color.appRed))
    }

    override fun onConnected() {
        runOnUiThread {
            connectedToCar = true
            disconnectedByClick = false
            carAvailableStatus.visibility = View.GONE
            mainProgressBar.visibility = View.INVISIBLE
            setConnectSwitchChecked(true)
            connectCarSwitch?.isEnabled = true
            toggleLightsSwitch.isEnabled = true
            speedControl.isEnabled = true
            modeSelectionSpinner.isEnabled = true
            setControlButtonsEnabled(true)
            sendMessage("L?") // request current lights status
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            connectedToCar = false
            modeSelectionSpinner.setSelection(0)
            carMode = 1
            carAvailableStatus.visibility = View.VISIBLE
            mainProgressBar.visibility = View.INVISIBLE
            setConnectSwitchChecked(false)
            connectCarSwitch?.isEnabled = true
            toggleLightsSwitch.isEnabled = false
            speedControl.isEnabled = false
            modeSelectionSpinner.isEnabled = false
            setControlButtonsEnabled(false)
            signalStrengthImage.setImageResource(R.drawable.signal_strength_0)
        }
    }
}
