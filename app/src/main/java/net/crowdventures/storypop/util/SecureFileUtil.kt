package net.crowdventures.storypop.util

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedFile
import net.crowdventures.storypop.Config
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SecureFileUtil {
    companion object{
        fun writeRawContentToFile(file: File, base64Content: String){
            val fout = FileOutputStream(file)
            val decodedString: ByteArray = Base64.decode(base64Content, Base64.DEFAULT)
            fout.write(decodedString)
            fout.flush()
            fout.close()
        }
        fun createSecureTmpContentFile(context:Context):File?{
            var mFileTemp: File? = null
            val root = context.getDir("tmp", Context.MODE_PRIVATE).getAbsolutePath();
            val myDir = File("$root/enc");
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            try {
                mFileTemp = File(
                    myDir.getAbsoluteFile(),
                    "temp_enc",
                )
                if (mFileTemp.exists()) mFileTemp.delete()
               // mFileTemp.createNewFile()
            } catch (e1: IOException) {
                //show blank image,failure
                Log.v("ViewModelFact","Could not create temp_enc file! ${e1.message}")
                return null
            }
            return mFileTemp
         }

        fun writeSecureTmpContent(context:Context,secureFile:File,content:String){
            val encryptedFile = EncryptedFile.Builder(
                secureFile,
                context,
                Config.getEncryptionKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build()
            encryptedFile.openFileOutput().use { fout ->
                val encodedString: ByteArray =content.encodeToByteArray()
                fout.write(encodedString)
                fout.flush()
                fout.close()
            }
        }

        fun readSecureTmpContentAsBase64(context:Context,secureFile:File):String{
            val encryptedFile = EncryptedFile.Builder(
                secureFile,
                context,
                Config.getEncryptionKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build()
            encryptedFile.openFileInput().use { fin ->
                val bytes = fin.readBytes()
                fin.close()
                return Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        }

        fun readSecureTmpContent(context:Context,secureFile:File):String{
            val encryptedFile = EncryptedFile.Builder(
                secureFile,
                context,
                Config.getEncryptionKeyAlias(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build()
            encryptedFile.openFileInput().use { inputStream ->
                // Read data from your encrypted file
                val fileContent = StringBuffer("")
                val buffer = ByteArray(1024)
                while (inputStream.read(buffer) != -1) {
                    fileContent.append(String(buffer))
                }
                return fileContent.toString()
            }
        }
    }
}