package de.pollpeter.jonas.rasteuerung

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager


class WifiHelper(private val activity: Activity, private val iWifiHelper: IWifiHelper? = null) :
    BroadcastReceiver() {
    private val carWifiSSID: String = "RoboterAuto"
    private val carWifiPassword: String = "JonasRoboterProjekt"

    private var wifiManager: WifiManager =
        activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    init {
        registerReceiver()
    }

    fun registerReceiver(){
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        activity.registerReceiver(this, intentFilter)
    }

    fun scanForCar(){
        wifiManager.isWifiEnabled = true
        wifiManager.startScan()
    }

    fun unregisterReceiver(){
        activity.unregisterReceiver(this)
    }

    fun connectToCar() {
        wifiManager.isWifiEnabled = true
        Thread {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = java.lang.String.format("\"%s\"", carWifiSSID)
            wifiConfig.preSharedKey = String.format("\"%s\"", carWifiPassword)

            var netId: Int? = wifiManager.addNetwork(wifiConfig)
            if (netId == -1) {
                netId = wifiManager.configuredNetworks?.let { it ->
                    it.firstOrNull { it.SSID.trim('"') == carWifiSSID.trim('"') }?.networkId ?: -1
                }
            }
            wifiManager.enableNetwork(netId!!, true)
            Thread.sleep(5000)
            if(!isConnectedToCar()) {
                disconnectCar()
                iWifiHelper?.onDisconnected()
            } else{
                iWifiHelper?.onConnected()
            }
        }.start()
    }

    fun disconnectCar() {
        wifiManager.disconnect()
    }

    fun isConnectedToCar(): Boolean {
        val info = wifiManager.connectionInfo
        return info.ssid.trim('"') == carWifiSSID
    }

    fun getSignalStrength(): Int{
        val rssi = wifiManager.connectionInfo.rssi
        return if (rssi <= 0 && rssi >= -50) {
            4
        } else if (rssi < -50 && rssi >= -70) {
            3
        } else if (rssi < -70 && rssi >= -80) {
            2
        } else if (rssi < -80 && rssi >= -100) {
            1
        } else {
            0
        }
    }

    interface IWifiHelper {
        fun onConnectionAvailable()
        fun onConnectionNotAvailable()
        fun onConnected()
        fun onDisconnected()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            val netInfo: NetworkInfo? = intent?.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
            if (ConnectivityManager.TYPE_WIFI == netInfo?.type){
                if (isConnectedToCar()) iWifiHelper?.onConnected()
                else iWifiHelper?.onDisconnected()
            }
        } else if (intent?.action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            val mScanResults: List<ScanResult> = wifiManager.scanResults
            var carAvailable = false
            for(result in mScanResults){
                if(result.SSID == carWifiSSID){
                    carAvailable = true
                }
            }
            if(carAvailable) iWifiHelper!!.onConnectionAvailable()
            else iWifiHelper!!.onConnectionNotAvailable()
        }
    }
}