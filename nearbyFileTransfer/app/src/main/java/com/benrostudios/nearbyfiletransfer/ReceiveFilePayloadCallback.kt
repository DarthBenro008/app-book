package com.benrostudios.nearbyfiletransfer

import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.util.Log
import androidx.collection.SimpleArrayMap
import androidx.core.net.toUri
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.io.*
import java.nio.charset.StandardCharsets


internal class ReceiveFilePayloadCallback(private val context: Context) : PayloadCallback() {
    private val incomingFilePayloads =
        SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads =
        SimpleArrayMap<Long, Payload?>()
    private val filePayloadFilenames =
        SimpleArrayMap<Long, String>()



    override fun onPayloadReceived(
        endpointId: String,
        payload: Payload
    ) {
        if (payload.type == Payload.Type.BYTES) {
            val payloadFilenameMessage =
                String(payload.asBytes()!!, StandardCharsets.UTF_8)
            val payloadId = addPayloadFilename(payloadFilenameMessage)
            processFilePayload(payloadId)
        } else if (payload.type == Payload.Type.FILE) {
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.id, payload)
        }
    }

    /**
     * Extracts the payloadId and filename from the message and stores it in the
     * filePayloadFilenames map. The format is payloadId:filename.
     */
    private fun addPayloadFilename(payloadFilenameMessage: String): Long {
        val parts =
            payloadFilenameMessage.split(":".toRegex()).toTypedArray()
        val payloadId = parts[0].toLong()
        val filename = parts[1]
        filePayloadFilenames.put(payloadId, filename)
        return payloadId
    }

    private fun processFilePayload(payloadId: Long) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        val filePayload= completedFilePayloads[payloadId]
        val filename = filePayloadFilenames[payloadId]
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId)
            filePayloadFilenames.remove(payloadId)

            // Get the received file (which will be in the Downloads folder)
//            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
//                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
//                // allowed to access filepaths from another process directly. Instead, we must open the
//                // uri using our ContentResolver.
//
//                //val uri: Uri = Uri.parse(filePayload.asFile()?.asJavaFile()?.absolutePath)
//                //val file = File(filePayload.asFile()!!.asJavaFile()!!.absolutePath)
//                val uri: Uri = filePayload.asFile()!!.asJavaFile()!!.toUri()
//                try {
//                    Log.d("Reciever","Processing File $uri")
//                    // Copy the file to a new location.
//                    val inputStream: InputStream? = if(uri.toString().startsWith("file://")){
//                        File(filePayload.asFile()!!.asJavaFile()!!.absolutePath).inputStream()
//                    }else{
//                        context.contentResolver.openInputStream(uri)
//                    }
//                    copyStream(
//                        inputStream!!,
//                        FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$filename.jpeg"))
//                    )
//                } catch (e: IOException) {
//                    // Log the error.
//                    Log.d("ErrorParsing","$e")
//                } finally {
//                    // Delete the original file.
//                    //context.contentResolver.delete(uri, null, null)
//                    Log.d("Reciever","Success")
//                }
//            } else {
                val payloadFile: File? = filePayload.asFile()!!.asJavaFile()

                // Rename the file.
                payloadFile?.renameTo(File(payloadFile.parentFile, "$filename.jpeg"))
                Log.d("Recieevr","Done")
//            }
        }
    }

    override fun onPayloadTransferUpdate(
        endpointId: String,
        update: PayloadTransferUpdate
    ) {
        if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
            val payloadId = update.payloadId
            val payload = incomingFilePayloads.remove(payloadId)
            completedFilePayloads.put(payloadId, payload)
            if (payload?.type == Payload.Type.FILE) {
                processFilePayload(payloadId)
                Log.d("Reciever","Going to Processing File")
            }
        }
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