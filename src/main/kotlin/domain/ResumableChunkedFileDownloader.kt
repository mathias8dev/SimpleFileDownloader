package com.mathias8dev.simplefiledownloader.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URI


class ResumableChunkedFileDownloader(
    private val fileURL: String,
    private val outputFilePath: String,
    private val startByte: Long,
    private val endByte: Long,
    private val totalFileSize: Long,
    private val progressListener: ProgressListener,
    private val controller: FastFileDownloaderController
) {

    suspend fun downloadChunk() = coroutineScope {
        val remoteUrl = URI.create(fileURL).toURL()
        val connection = remoteUrl.openConnection() as HttpURLConnection
        connection.setRequestProperty("Range", "bytes=$startByte-$endByte")
        connection.connect()

        val input = connection.inputStream
        val output = RandomAccessFile(outputFilePath, "rw")
        output.seek(startByte)

        val buffer = ByteArray(4096)
        var bytesRead: Long = 0
        var totalBytesDownloaded = startByte

        while (totalBytesDownloaded <= endByte && input.read(buffer).also { bytesRead = it.toLong() } != -1) {
            if (controller.isCancelled()) break

            while (controller.isPaused()) {
                delay(100) // Check every 100ms if the operation should resume
            }

            val bytesToWrite = minOf(bytesRead.toLong(), endByte - totalBytesDownloaded + 1)
            output.write(buffer, 0, bytesToWrite.toInt())
            totalBytesDownloaded += bytesToWrite
            progressListener.onProgressUpdate(bytesRead, totalFileSize)
        }

        input.close()
        output.close()
    }
}