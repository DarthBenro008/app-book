package com.benrostudios.biometriccryptography

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.zeroone.conceal.listener.OnDataChamberChangeListener
import kotlinx.android.synthetic.main.activity_encrypted2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.*
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec


class EncryptedActivity : OnDataChamberChangeListener, AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypted2)

        val getContent =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                it?.let {
                    encryption(it)
                }
            }

        select.setOnClickListener {
            getContent.launch(arrayOf("*/*"))
        }

    }

    override fun onDataChange(key: String, value: String) {
        TODO("Not yet implemented")
    }

    private fun encryption(uri: Uri) {
        val filename = fileNameExtractor(uri)
        val encryption = lifecycleScope.async(Dispatchers.IO) {
            encrypt(uri, filename)
        }
        encryption.invokeOnCompletion {
            runOnUiThread {
                Toast.makeText(this, "lololol", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun fileNameExtractor(uri: Uri): String {
        val uriString = uri.toString()
        val myFile = File(uriString)
        var fileName = ""

        if (uriString.startsWith("content://")) {
            var cursor: Cursor? = null
            try {
                cursor = this.contentResolver?.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    fileName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close();
            }
        } else if (uriString.startsWith("file://")) {
            fileName = myFile.name;
        }
        return fileName
    }


    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class
    )
    suspend fun encrypt(uri: Uri, fileName: String) = withContext(Dispatchers.IO) {
        val safayaPathFile = File("/storage/emulated/0/safaya")
        if (!safayaPathFile.exists()) {
            safayaPathFile.mkdirs()
        }
        val fis = BufferedInputStream(contentResolver.openInputStream(uri))
        val fos =
            BufferedOutputStream(FileOutputStream("${applicationContext.filesDir}/$fileName"))

        val sks = SecretKeySpec(
            "MyDifficultPassw".toByteArray(),
            "AES"
        )

        val cipher: Cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, sks)
        // Wrap the output stream
        val cos = BufferedOutputStream(CipherOutputStream(fos, cipher))
        // Write bytes
        var b: Int = 0
        val d = ByteArray(8)
        while (fis.read(d).also { b = it } != -1) {
            cos.write(d, 0, b)
        }
        // Flush and close streams.
        cos.flush()
        cos.close()
        fis.close()
        Log.d("Trial", "Success")
        // decrypt()
        Log.d("trial", "${uri.path}")
        //DocumentFile.fromSingleUri(applicationContext, uri)?.delete()
    }


    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class
    )
    suspend fun decrypt() = withContext(Dispatchers.IO) {
        val extStore = "/storage/emulated/0"
        val fis = BufferedInputStream(FileInputStream("$extStore/encrypted"))
        val fos = BufferedOutputStream(FileOutputStream("$extStore/decrypted"))
        val sks = SecretKeySpec(
            "MyDifficultPassw".toByteArray(),
            "AES"
        )
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, sks)
        val cis = BufferedInputStream(CipherInputStream(fis, cipher))
        var b: Int = 0
        val d = ByteArray(8)
        while (cis.read(d).also { b = it } != -1) {
            fos.write(d, 0, b)
        }
        fos.flush()
        fos.close()
        cis.close()
    }
}