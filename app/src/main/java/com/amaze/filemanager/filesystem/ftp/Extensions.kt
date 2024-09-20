

package com.amaze.filemanager.filesystem.ftp

import net.schmizz.sshj.xfer.FilePermission
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPFile.EXECUTE_PERMISSION
import org.apache.commons.net.ftp.FTPFile.GROUP_ACCESS
import org.apache.commons.net.ftp.FTPFile.READ_PERMISSION
import org.apache.commons.net.ftp.FTPFile.USER_ACCESS
import org.apache.commons.net.ftp.FTPFile.WORLD_ACCESS
import org.apache.commons.net.ftp.FTPFile.WRITE_PERMISSION
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

/**
 * @see https://stackoverflow.com/a/7606723
 */
@Throws(IOException::class)
fun FTPClient.makeDirectoryTree(dirTree: String) {
    if (dirTree == "/") {
        return
    }

    changeWorkingDirectory("/")
    var dirExists = true
    // Tokenize the string and attempt to change into each directory level.
    // If you cannot, then start creating.
    val directories = dirTree.split('/')
    for (dir in directories) {
        if (dir.isNotEmpty()) {
            if (dirExists) {
                dirExists = changeWorkingDirectory(dir)
            }
            if (!dirExists) {
                if (!makeDirectory(dir)) {
                    throw IOException(
                        "Unable to create remote directory '$dir'. Error='$replyString'",
                    )
                }
                if (!changeWorkingDirectory(dir)) {
                    throw IOException(
                        "Unable to change into newly created remote directory '$dir'. " +
                            "Error='$replyString'",
                    )
                }
            }
        }
    }
}

/**
 * Try to ask server for space available, if it supports use of AVBL command.
 *
 * @param path Path to check for space available.
 */
fun FTPClient.getSpaceAvailable(path: String = "/"): Long {
    if (hasFeature("AVBL")) {
        val result = sendCommand("AVBL", path)
        if (result == FTPReply.FILE_STATUS) {
            // skip the return code (e.g. 213) and the space
            return getReplyString().substring(4).toLong()
        } else {
            return -1L
        }
    } else {
        return -1L
    }
}

/**
 * Translate FTPFile's permission to Set of [FilePermission].
 */
fun FTPFile.toFilePermissions(): Set<FilePermission> {
    val retval = HashSet<FilePermission>()
    // Got a better and smarter idea?
    if (hasPermission(USER_ACCESS, READ_PERMISSION)) {
        retval.add(FilePermission.USR_R)
    }
    if (hasPermission(USER_ACCESS, WRITE_PERMISSION)) {
        retval.add(FilePermission.USR_W)
    }
    if (hasPermission(USER_ACCESS, EXECUTE_PERMISSION)) {
        retval.add(FilePermission.USR_X)
    }
    if (hasPermission(GROUP_ACCESS, READ_PERMISSION)) {
        retval.add(FilePermission.GRP_R)
    }
    if (hasPermission(GROUP_ACCESS, WRITE_PERMISSION)) {
        retval.add(FilePermission.GRP_W)
    }
    if (hasPermission(GROUP_ACCESS, EXECUTE_PERMISSION)) {
        retval.add(FilePermission.GRP_X)
    }
    if (hasPermission(WORLD_ACCESS, READ_PERMISSION)) {
        retval.add(FilePermission.OTH_R)
    }
    if (hasPermission(WORLD_ACCESS, WRITE_PERMISSION)) {
        retval.add(FilePermission.OTH_W)
    }
    if (hasPermission(WORLD_ACCESS, EXECUTE_PERMISSION)) {
        retval.add(FilePermission.OTH_X)
    }
    return retval
}
