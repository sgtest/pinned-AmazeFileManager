package com.amaze.filemanager.utils

import java.security.MessageDigest
import java.util.WeakHashMap
import net.schmizz.sshj.common.ByteArrayUtils
import org.json.JSONObject

object X509CertificateUtil {
    const val SUBJECT = "subject"
    const val ISSUER = "issuer"
    const val SERIAL = "serial"
    const val FINGERPRINT = "sha256Fingerprint"

    private fun colonSeparatedHex(array: ByteArray) =
        ByteArrayUtils.toHex(array).chunked(2).joinToString(":")

    /**
     * Parse a [javax.security.cert.X509Certificate] and return part of its information in a JSON object.
     *
     * Includes the certificate's subject, issuer, serial number and SHA-256 fingerprint.
     *
     * @param certificate [javax.security.cert.X509Certificate]
     * @return [JSONObject]
     */
    fun parse(certificate: javax.security.cert.X509Certificate): Map<String, String> {
        val retval = WeakHashMap<String, String>()
        retval[SUBJECT] = certificate.subjectDN.name
        retval[ISSUER] = certificate.issuerDN.name
        retval[SERIAL] = colonSeparatedHex(certificate.serialNumber.toByteArray())
        retval[FINGERPRINT] =
            MessageDigest.getInstance("sha-256").run {
                colonSeparatedHex(digest(certificate.encoded))
            }
        return retval
    }

    /**
     * Parse a [java.security.cert.X509Certificate] and return part of its information in a JSON object.
     *
     * Includes the certificate's subject, issuer, serial number and SHA-256 fingerprint.
     *
     * @param certificate [java.security.cert.X509Certificate]
     * @return [JSONObject]
     */
    fun parse(certificate: java.security.cert.X509Certificate): Map<String, String> {
        val retval = WeakHashMap<String, String>()
        retval[SUBJECT] = certificate.subjectDN.name
        retval[ISSUER] = certificate.issuerDN.name
        retval[SERIAL] = colonSeparatedHex(certificate.serialNumber.toByteArray())
        retval[FINGERPRINT] =
            MessageDigest.getInstance("sha-256").run {
                colonSeparatedHex(digest(certificate.encoded))
            }
        return retval
    }
}
