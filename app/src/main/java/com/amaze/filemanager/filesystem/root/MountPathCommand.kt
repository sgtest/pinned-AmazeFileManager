package com.amaze.filemanager.filesystem.root

import android.os.Build
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.base.IRootCommand

object MountPathCommand : IRootCommand() {
    const val READ_ONLY = "RO"
    const val READ_WRITE = "RW"

    /**
     * Mount filesystem associated with path for writable access (rw) Since we don't have the root of
     * filesystem to remount, we need to parse output of # mount command.
     *
     * @param path the path on which action to perform
     * @param operation RO or RW
     * @return String the root of mount point that was ro, and mounted to rw; null otherwise
     */
    @Throws(ShellNotRunningException::class)
    fun mountPath(
        pathArg: String,
        operation: String,
    ): String? {
        val path = RootHelper.getCommandLineString(pathArg)
        return when (operation) {
            READ_WRITE -> mountReadWrite(path)
            READ_ONLY -> {
                val command = "umount -r \"$path\""
                runShellCommand(command)
                null
            }

            else -> null
        }
    }

    private fun mountReadWrite(path: String): String? {
        val command = "mount"
        val output = runShellCommandToList(command)
        var mountPoint = ""
        var types: String? = null
        var mountArgument: String? = null
        for (line in output) {
            val words = line.split(" ").toTypedArray()

            // mount command output for older Androids
            // <code>/dev/block/vda /system ext4 ro,seclabel,relatime,data=ordered 0 0</code>
            var mountPointOutputFromShell = words[1]
            var mountPointFileSystemTypeFromShell = words[2]
            var mountPointArgumentFromShell = words[3]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // mount command output for Android version >= 7
                // <code>/dev/block/bootdevice/by-name/system on /system type ext4
                // (ro,seclabel,relatime,data=ordered)</code>
                mountPointOutputFromShell = words[2]
                mountPointFileSystemTypeFromShell = words[4]
                mountPointArgumentFromShell = words[5]
            }
            if (path.startsWith(mountPointOutputFromShell)) {
                // current found point is bigger than last one, hence not a conflicting one
                // we're finding the best match, this omits for eg. / and /sys when we're actually
                // looking for /system
                if (mountPointOutputFromShell.length > mountPoint.length) {
                    mountPoint = mountPointOutputFromShell
                    types = mountPointFileSystemTypeFromShell
                    mountArgument = mountPointArgumentFromShell
                }
            }
        }

        if (mountPoint != "" && types != null && mountArgument != null) {
            // we have the mountpoint, check for mount options if already rw
            if (mountArgument.contains("rw")) {
                // already a rw filesystem return
                return null
            } else if (mountArgument.contains("ro")) {
                // read-only file system, remount as rw
                val mountCommand = "mount -o rw,remount $mountPoint"
                val mountOutput = runShellCommandToList(mountCommand)
                return if (mountOutput.isNotEmpty()) {
                    // command failed, and we got a reason echo'ed
                    null
                } else {
                    mountPoint
                }
            }
        }
        return null
    }
}
