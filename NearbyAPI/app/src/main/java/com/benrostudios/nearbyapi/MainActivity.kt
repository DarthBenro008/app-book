package com.benrostudios.nearbyapi

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        advertise()
        discover()
    }

    companion object {
        const val TAG = "Main"
    }

    fun advertise() {
        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        "Device A",
                        "com.benrostudios.nearbyAPI",
                        connectionLifecycleCallback,
                        AdvertisingOptions.Builder()
                                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                                .build()
                )
                .addOnSuccessListener {
                    Log.d(TAG, "ADVERTISING!!")
                }
                .addOnFailureListener {
                    Log.d(TAG, "FAIL TO ADVERTISE")
                }
    }

    fun discover() {
        Nearby.getConnectionsClient(this)
                .startDiscovery(
                        "com.benrostudios.nearbyAPI",
                        endpointDiscoveryCallback,
                        DiscoveryOptions.Builder()
                                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                                .build())
                .addOnFailureListener {
                    Log.d(TAG,"Discovery : $it")
                }
                .addOnSuccessListener {
                    Log.d(TAG,"DISCOVERING!!")
                }
    }


    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            Nearby.getConnectionsClient(application)
                    .requestConnection("Device A", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d(TAG, "Requested Connection?")
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "$it")
                    }
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            Nearby.getConnectionsClient(application).acceptConnection(endpointId, ReceiveBytesPayloadListener())
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG,"STATUS OK")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG,"STATUS rejected")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG,"STATUS error")
                }
                else -> {
                    Log.d(TAG,"STATUS else ")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            Log.d(TAG,"disconnected")
        }
    }

    internal class ReceiveBytesPayloadListener : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val receivedBytes = payload.asBytes()
            Log.d(TAG,"Payload!! $receivedBytes")
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

}