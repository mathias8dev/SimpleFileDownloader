package com.mathias8dev.simplefiledownloader.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.HttpURLConnection
import java.net.URI

class FastFileDownloader(
    private val fileURL: String,
    private val outputFilePath: String,
    private val progressListener: ProgressListener,
    private val controller: FastFileDownloaderController
) {

    suspend fun download() = coroutineScope {
        val connection = URI(fileURL).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connect()

        val fileSize: Long = connection.contentLengthLong
        val numThreads = fileSize.div(BASE_SIZE_PER_THREAD).coerceIn(MIN_THREADS, MAX_THREADS)
        println("Number of concurrent threads: $numThreads")

        val chunkSize = fileSize / numThreads

        val startTime = System.currentTimeMillis()
        val deferred = (0 until numThreads).map { i ->
            async(Dispatchers.IO) {
                val startByte = i * chunkSize
                val endByte = if (i == numThreads - 1) fileSize - 1 else (startByte + chunkSize - 1)
                val downloader = ResumableChunkedFileDownloader(
                    fileURL,
                    outputFilePath,
                    startByte,
                    endByte,
                    fileSize,
                    progressListener,
                    controller
                )
                downloader.downloadChunk()
            }
        }

        deferred.awaitAll()

        if (!controller.isCancelled()) {
            progressListener.onDownloadComplete(fileSize, System.currentTimeMillis() - startTime)
        }
    }

    companion object {
        private const val BASE_SIZE_PER_THREAD = 10 * 1024 * 1024L // 10 MB
        private const val MIN_THREADS = 1L
        private const val MAX_THREADS = 8L
    }
}