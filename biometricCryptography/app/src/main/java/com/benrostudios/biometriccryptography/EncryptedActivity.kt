package com.benrostudios.biometriccryptography

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.chamber.kotlin.library.SharedChamber.ChamberBuilder
import com.chamber.kotlin.library.model.ChamberType
import com.zeroone.conceal.listener.OnDataChamberChangeListener
import kotlinx.android.synthetic.main.activity_encrypted2.*
import java.io.*
import java.nio.charset.StandardCharsets
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
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri

                val inputStream: InputStream? = contentResolver.openInputStream(uri!!)

                lol(uri)
                val file: File? = File(uri.toString())
                Log.d("trial1", file!!.absolutePath)
                encrypt(uri)
            }

        select.setOnClickListener {
            getContent.launch("image/*")
        }


    }

    override fun onDataChange(key: String, value: String) {
        TODO("Not yet implemented")
    }


    private fun lol(uri: Uri) {
        val uriString = uri.toString()
        val myFile = File(uriString)
        var imageName = ""

        if (uriString.startsWith("content://")) {
            var cursor: Cursor? = null
            try {
                cursor = this?.contentResolver?.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    imageName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close();
            }
        } else if (uriString.startsWith("file://")) {
            imageName = myFile.name;
        }
        Log.d("trail", "$imageName ,$uriString")
    }


    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class
    )
    fun encrypt(uri: Uri) {
        val fis = contentResolver.openInputStream(uri)
        // This stream write the encrypted text. This stream will be wrapped by
        // another stream.
        val fos =
            FileOutputStream("/storage/emulated/0/encrypted")

        // Length is 16 byte
        val sks = SecretKeySpec(
            "MyDifficultPassw".toByteArray(),
            "AES"
        )
        // Create cipher
        val cipher: Cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, sks)
        // Wrap the output stream
        val cos = CipherOutputStream(fos, cipher)
        // Write bytes
        var b: Int = 0
        val d = ByteArray(8)
        if (fis != null) {
            while (fis.read(d).also { b = it } != -1) {
                cos.write(d, 0, b)
            }
        }
        // Flush and close streams.
        cos.flush()
        cos.close()
        fis?.close()
        decrypt()
    }


    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class
    )
    fun decrypt() {
        val extStore = "/storage/emulated/0"
        val fis = FileInputStream("$extStore/encrypted")
        val fos = FileOutputStream("$extStore/decrypted")
        val sks = SecretKeySpec(
            "MyDifficultPassw".toByteArray(),
            "AES"
        )
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, sks)
        val cis = CipherInputStream(fis, cipher)
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