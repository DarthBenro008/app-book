package com.benrostudios.nearbyfiletransfer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var uri: Uri
    private var imageName: String? = null
    private var uriString: String? = null
    private var connectionsClient: ConnectionsClient? = null
    private lateinit var opponentEndpointId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        send_button.setOnClickListener {
            getImage()
        }

        recieve_button.setOnClickListener {
            discover()
        }

        pic_sender.setOnClickListener {
            sendPic()
        }
        connectionsClient = Nearby.getConnectionsClient(this)

    }

    companion object {
        const val TAG = "Main"
        const val PICK_IMG_CODE = 3
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
                    conn_status.text = "ADVERTISING!!"
                }
                .addOnFailureListener {
                    Log.d(TAG, "FAIL TO ADVERTISE")
                    conn_status.text = "FAIL TO ADVERTISE : $it"
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
                    conn_status.text = "Discovery : $it"
                }
                .addOnSuccessListener {
                    Log.d(TAG,"DISCOVERING!!")
                    conn_status.text = "Discovering!"
                }
    }


    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            Nearby.getConnectionsClient(application)
                    .requestConnection("Device A", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d(TAG, "Requested Connection?")
                        conn_status.text = "Requested Connection?"
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "$it")
                        conn_status.text = "Requested Connection Failure"
                    }
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
            conn_status.text = "Device Too FAR"
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            Nearby.getConnectionsClient(application).acceptConnection(endpointId, ReceiveBytesPayloadListener())
            opponentEndpointId = endpointId
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG,"STATUS OK")
                    conn_status.text = "STATUS OK"
                    connectionsClient?.stopAdvertising()
                    connectionsClient?.stopDiscovery()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG,"STATUS rejected")
                    conn_status.text = "STATUS Rejected"
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG,"STATUS error")
                    conn_status.text = "STATUS Error"
                }
                else -> {
                    Log.d(TAG,"STATUS else ")
                    conn_status.text = "STATUS else"
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
            val payloadFile: File? = payload.asFile()?.asJavaFile()
            payloadFile?.renameTo(File(payloadFile.parentFile, "tp.jpeg"))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
            if(update.status == PayloadTransferUpdate.Status.SUCCESS){
                Log.d(TAG,"Success Transfer!!")
            }
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun getImage() {
        val intent = Intent()
        intent.type = "image/jpeg"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), Companion.PICK_IMG_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMG_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            if (data.data != null) {
                uri = data.data!!
                uriString = uri.toString()
                val myFile = File(uriString!!)
                imageName = null

                if (uriString!!.startsWith("content://")) {
                    var cursor: Cursor? = null
                    try {
                        cursor = this.contentResolver?.query(uri, null, null, null, null)
                        if (cursor != null && cursor.moveToFirst()) {
                            imageName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        }
                    } finally {
                        cursor?.close();
                    }
                }
                else if (uriString!!.startsWith("file://")) {
                    imageName = myFile.name;
                }
            }
            advertise()
        }
        else {
            if(imageName.isNullOrEmpty())
                Toast.makeText(this, "No file chosen", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendPic(){
        val pfd = contentResolver.openFileDescriptor(uri, "r")
        val filePayload = Payload.fromFile(pfd!!)
        connectionsClient?.sendPayload(opponentEndpointId, filePayload)
    }
}