package com.benrostudios.nearbyfiletransfer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets


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
            advertise()
        }

        recieve_button.setOnClickListener {
            discover()
        }

        pic_sender.setOnClickListener {
            showImageChooser(opponentEndpointId)
        }
        connectionsClient = Nearby.getConnectionsClient(this)

    }

    companion object {
        const val TAG = "Main"
        const val PICK_IMG_CODE = 3
        const val READ_REQUEST_CODE = 42
        const val ENDPOINT_ID_EXTRA = "com.benrostudios.nearbyfiletransfer.fileEndpointID"
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
            Nearby.getConnectionsClient(application).acceptConnection(endpointId, ReceiveWithProgressCallback(applicationContext))
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




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null
        ) {
            val endpointId: String? = data.getStringExtra(ENDPOINT_ID_EXTRA)

            // The URI of the file selected by the user.
            val uri: Uri? = data.data
            val filePayload: Payload
            filePayload = try {
                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd = contentResolver.openFileDescriptor(uri!!, "r")
                Payload.fromFile(pfd!!)
            } catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }

            // Construct a simple message mapping the ID of the file payload to the desired filename.
            val filenameMessage =
                filePayload.id.toString() + ":" + uri.lastPathSegment

            // Send the filename message as a bytes payload.
            val filenameBytesPayload =
                Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))
            connectionsClient?.sendPayload(opponentEndpointId, filenameBytesPayload)

            // Finally, send the file payload.
            connectionsClient?.sendPayload(opponentEndpointId, filePayload)
            ReceiveWithProgressCallback(applicationContext).sendPayload(opponentEndpointId,filePayload)
        }
    }

    private fun showImageChooser(endpointId: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }
}