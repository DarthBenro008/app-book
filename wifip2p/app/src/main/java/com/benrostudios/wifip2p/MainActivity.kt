package com.benrostudios.wifip2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.benrostudios.wifip2p.adapters.withSimpleAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.peer_item.view.*

class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
    }
    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null
    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    val p2pList: WifiP2pManager.PeerListListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = WiFiDirectBroadcastReceiver(manager!!, channel, this)
        }

        button.setOnClickListener {
            manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Log.d(TAG,"Found Peers!")
                    manager?.requestPeers(channel) {
                        Log.d(TAG,it.toString())
                        inflatePeers(it)
                    }
                }

                override fun onFailure(reasonCode: Int) {
                    Log.d(TAG,"Didn't Find any peers!")
                }
            })


        }


    }
    override fun onResume() {
        super.onResume()
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    private fun connectToPeer(wifiP2pDevice: WifiP2pDevice){
        val device: WifiP2pDevice = wifiP2pDevice
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        channel?.also { channel ->
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Log.d(TAG,"Connected To Peer!!")
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG,"Failure Connecting to peer!")
                }
            }
            )}
    }

    fun inflatePeers(data: WifiP2pDeviceList){
        recyclerView.layoutManager = LinearLayoutManager(this)
        Log.d(TAG,"Found Peers? ${data.deviceList.toMutableList()}")
        recyclerView.withSimpleAdapter(data.deviceList.toMutableList(),R.layout.peer_item){dataDevice ->
            itemView.peer_displayName.text = dataDevice.deviceName
            itemView.peer_item_constraint.setOnClickListener {
                connectToPeer(dataDevice)
            }
        }
    }
}