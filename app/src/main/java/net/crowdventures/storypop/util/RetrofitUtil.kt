package net.crowdventures.storypop.util

import android.content.Context
import net.crowdventures.storypop.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class RetrofitUtil {
    companion object{
        fun generateSecureOkHttpClient(context: Context): OkHttpClient {
            // Create a simple builder for our http client, this is only por example purposes
            val httpClientBuilder = OkHttpClient.Builder()
                .readTimeout(25, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)

            // Here you may wanna add some headers or custom setting for your builder

            // Get the file of our certificate
            val caFileInputStream = context.resources.openRawResource(R.raw.isrgrootx1)
            val ca2FileInputStream = context.resources.openRawResource(R.raw.usertrust_rsa)

            val caCert: Certificate = CertificateFactory.getInstance("X.509").generateCertificate(caFileInputStream)
            val ca2Cert: Certificate = CertificateFactory.getInstance("X.509").generateCertificate(ca2FileInputStream)
            // We're going to put our certificates in a Keystore
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ISRG Root X1",caCert)
            keyStore.setCertificateEntry("USERTrust RSA Certification Authority",ca2Cert)

            val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
            val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
            tmf.init(keyStore)


            // Get hold of the default trust manager
            // Get hold of the default trust manager
            var x509Tm: X509TrustManager? = null
            for (tm in tmf.trustManagers) {
                if (tm is X509TrustManager) {
                    x509Tm = tm
                    break
                }
            }
            if (x509Tm == null) throw Exception("No default trust manager found!")

            // Create a SSL context with the key managers of the KeyManagerFactory
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, tmf.trustManagers, SecureRandom())


            //Finally set the sslSocketFactory to our builder and build it
            return httpClientBuilder
                .sslSocketFactory(sslContext.socketFactory,x509Tm)
                .build()
        }
    }
}