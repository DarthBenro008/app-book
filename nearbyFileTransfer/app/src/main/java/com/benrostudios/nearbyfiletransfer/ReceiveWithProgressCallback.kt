package com.benrostudios.nearbyfiletransfer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.SimpleArrayMap
import androidx.core.app.NotificationCompat
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets


class ReceiveWithProgressCallback(private val context: Context) : PayloadCallback() {
    private val incomingPayloads =
        SimpleArrayMap<Long, NotificationCompat.Builder>()
    private val outgoingPayloads =
        SimpleArrayMap<Long, NotificationCompat.Builder>()

    private val incomingFilePayloads =
        SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads =
        SimpleArrayMap<Long, Payload?>()
    private val filePayloadFilenames =
        SimpleArrayMap<Long, String>()

    private var notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    val chan1 = NotificationChannel(
        "deafault",
        "lol", NotificationManager.IMPORTANCE_DEFAULT
    )


    fun sendPayload(endpointId: String, payload: Payload) {
        if (payload.type == Payload.Type.BYTES) {
            // No need to track progress for bytes.
            return
        }

        // Build and start showing the notification.
        val notification =
            buildNotification(payload,  /*isIncoming=*/false)
        notificationManager.notify(payload.id.toInt(), notification.build())

        // Add it to the tracking list so we can update it.
        outgoingPayloads.put(payload.id, notification)
    }


    @SuppressLint("NewApi")
    private fun buildNotification(
        payload: Payload,
        isIncoming: Boolean
    ): NotificationCompat.Builder {
        notificationManager.createNotificationChannel(chan1)
        val notification = NotificationCompat.Builder(context, "deafault")
            .setContentTitle(if (isIncoming) "Receiving..." else "Sending...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentText("Transferring your files")
            .setChannelId("deafault")
        var indeterminate = false
        if (payload.type == Payload.Type.STREAM) {
            // We can only show indeterminate progress for stream payloads.
            indeterminate = true
        }
        notification.setProgress(100, 0, true)
        return notification
    }

    override fun onPayloadReceived(
        endpointId: String,
        payload: Payload
    ) {
        if (payload.type == Payload.Type.BYTES) {
            // No need to track progress for bytes.
            val payloadFilenameMessage =
                String(payload.asBytes()!!, StandardCharsets.UTF_8)
            val payloadId = addPayloadFilename(payloadFilenameMessage)
            processFilePayload(payloadId)
            return
        }

        // Build and start showing the notification.
        val notification = buildNotification(payload, true /*isIncoming*/)
        notificationManager.notify(payload.id.toInt(), notification.build())

        // Add it to the tracking list so we can update it.
        incomingPayloads.put(payload.id, notification)
        if (payload.type == Payload.Type.FILE) {
            incomingFilePayloads.put(payload.id, payload)
        }
    }

    override fun onPayloadTransferUpdate(
        endpointId: String,
        update: PayloadTransferUpdate
    ) {
        val payloadId = update.payloadId
        var notification: NotificationCompat.Builder? = null
        if (incomingPayloads.containsKey(payloadId)) {
            notification = incomingPayloads[payloadId]
            if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                // This is the last update, so we no longer need to keep track of this notification.
                incomingPayloads.remove(payloadId)
            }
        } else if (outgoingPayloads.containsKey(payloadId)) {
            notification = outgoingPayloads[payloadId]
            if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                // This is the last update, so we no longer need to keep track of this notification.
                outgoingPayloads.remove(payloadId)
            }
        }
        if (notification == null) {
            return
        }
        when (update.status) {
            PayloadTransferUpdate.Status.IN_PROGRESS -> {
                val size = update.totalBytes
                if (size == -1L) {
                    // This is a stream payload, so we don't need to update anything at this point.
                    return
                }
                val percentTransferred =
                    (100.0 * (update.bytesTransferred / update.totalBytes.toDouble())).toInt()
                notification.setProgress(100, percentTransferred,  /* indeterminate= */false)
            }
            PayloadTransferUpdate.Status.SUCCESS -> {
                val payloadId2 = update.payloadId
                val payload = incomingFilePayloads.remove(payloadId2)
                completedFilePayloads.put(payloadId2, payload)
                if (payload?.type == Payload.Type.FILE) {
                    processFilePayload(payloadId2)
                    Log.d("Reciever", "Going to Processing File")
                }
                notification
                    .setProgress(100, 100,  /* indeterminate= */false)
                    .setContentText("Transfer complete!");
            }
            PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> notification.setProgress(
                0,
                0,
                false
            ).setContentText("Transfer failed")
            else -> {
            }
        }
        notificationManager.notify(payloadId.toInt(), notification.build())
    }

    private fun processFilePayload(payloadId: Long) {
        val filePayload = completedFilePayloads[payloadId]
        val filename = filePayloadFilenames[payloadId]
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId)
            filePayloadFilenames.remove(payloadId)
            val payloadFile: File? = filePayload.asFile()!!.asJavaFile()
            // Rename the file.
            payloadFile?.renameTo(File(payloadFile.parentFile, "$filename.jpeg"))
            Log.d("Recieevr", "Done")
        }
    }

    private fun addPayloadFilename(payloadFilenameMessage: String): Long {
        val parts =
            payloadFilenameMessage.split(":".toRegex()).toTypedArray()
        val payloadId = parts[0].toLong()
        val filename = parts[1]
        filePayloadFilenames.put(payloadId, filename)
        return payloadId
    }

    companion object {
        /** Copies a stream from one location to another.  */
        @Throws(IOException::class)
        private fun copyStream(inputStream: InputStream, out: OutputStream) {
            try {
                val buffer = ByteArray(1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
            } finally {
                inputStream.close()
                out.close()
            }
        }
    }

}