package com.amaze.filemanager.asynchronous.asynctasks.ssh

import android.app.AlertDialog
import androidx.annotation.MainThread
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.KeyPair
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.transport.TransportException

class SshAuthenticationTask(
    private val hostname: String,
    private val port: Int,
    private val hostKey: String,
    private val username: String,
    private val password: String? = null,
    private val privateKey: KeyPair? = null,
) : Task<SSHClient, SshAuthenticationTaskCallable> {
    override fun getTask(): SshAuthenticationTaskCallable =
        SshAuthenticationTaskCallable(hostname, port, hostKey, username, password, privateKey)

    @MainThread
    override fun onError(error: Throwable) {
        if (SocketException::class.java.isAssignableFrom(error.javaClass) ||
            ConnectException::class.java.isAssignableFrom(error.javaClass) ||
            SocketTimeoutException::class.java
                .isAssignableFrom(error.javaClass)
        ) {
            AppConfig.toast(
                AppConfig.getInstance(),
                AppConfig.getInstance()
                    .resources
                    .getString(
                        R.string.ssh_connect_failed,
                        hostname,
                        port,
                        error.localizedMessage ?: error.message,
                    ),
            )
        } else if (TransportException::class.java
                .isAssignableFrom(error.javaClass)
        ) {
            val disconnectReason =
                TransportException::class.java.cast(error)!!.disconnectReason
            if (DisconnectReason.HOST_KEY_NOT_VERIFIABLE == disconnectReason) {
                AlertDialog.Builder(AppConfig.getInstance().mainActivityContext)
                    .setTitle(R.string.ssh_connect_failed_host_key_changed_title)
                    .setMessage(R.string.ssh_connect_failed_host_key_changed_message)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        } else if (password != null) {
            AppConfig.toast(
                AppConfig.getInstance(),
                R.string.ssh_authentication_failure_password,
            )
        } else if (privateKey != null) {
            AppConfig.toast(
                AppConfig.getInstance(),
                R.string.ssh_authentication_failure_key,
            )
        }
    }

    @MainThread
    override fun onFinish(value: SSHClient) = Unit
}
