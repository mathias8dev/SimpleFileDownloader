package com.mathias8dev.simplefiledownloader

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpRequest
import java.time.Duration
import java.time.temporal.TemporalUnit
import kotlin.time.Duration.Companion.milliseconds


fun main() {
    println("Hello downloader!")
    DownloadApplication.downloadFile(
        "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
        "sample-mp4-file.mp4",
    )

    DownloadApplication.downloadFile(
        "http://212.183.159.230/512MB.zip",
        "test.zip",
    )
}

object DownloadApplication {

    fun downloadFile(fileURL: String, fileName: String) {
        val fileDownloader = FileDownloader(fileURL, fileName)
        runBlocking(Dispatchers.IO) {
            kotlin.runCatching {
                val progressListener = object : ProgressListener {
                    private val mutex = Mutex()
                    private var totalDownloaded = 0L


                    override suspend fun onProgressUpdate(bytesRead: Int, fileTotalSize: Long) {
                        mutex.withLock {
                            totalDownloaded += bytesRead
                            val progress = (totalDownloaded * 100 / fileTotalSize)
                            //println("Download progress: $progress%")
                        }
                    }

                    override suspend fun onDownloadComplete(fileSize: Long, downloadedIn: Long) {
                        println("Completed: file $fileName downloaded in ${downloadedIn.milliseconds.inWholeSeconds} seconds")
                    }
                }

                fileDownloader.download(progressListener)

            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}

class FileDownloader(
    private val fileURL: String,
    private val outputFilePath: String,
) {


    suspend fun download(progressListener: ProgressListener) = coroutineScope {
        val connection = URI(fileURL).toURL().openConnection() as HttpURLConnection
        connection.setRequestMethod("HEAD")
        connection.connect()

        val fileSize: Long = connection.contentLengthLong
        val numThreads = fileSize.div(BASE_SIZE_PER_THREAD).coerceIn(MIN_THREADS, MAX_THREADS)
        println("Number of concurrent call: $numThreads")

        val chunkSize = fileSize / numThreads

        val downloadStartTime = System.currentTimeMillis()
        val deferred = (0 until numThreads).map { i ->
            async(Dispatchers.IO) {
                val startByte = i * chunkSize
                val endByte = if ((i == numThreads - 1)) fileSize - 1 else (startByte + chunkSize - 1)
                val downloader =
                    ChunkDownloader(fileURL, outputFilePath, startByte, endByte, fileSize, progressListener)
                downloader.run()
            }
        }

        deferred.awaitAll()

        progressListener.onDownloadComplete(fileSize, System.currentTimeMillis() - downloadStartTime)

    }

    companion object {
        private const val BASE_SIZE_PER_THREAD = 10 * 1024 * 1024L // 10 MB
        private const val MIN_THREADS = 1L
        private const val MAX_THREADS = 8L
    }
}


class ChunkDownloader(
    private val fileURL: String,
    private val fileName: String,
    private val startByte: Long,
    private val endByte: Long,
    private val totalFileSize: Long,
    private val chunkProgressListener: ProgressListener
) {

    suspend fun run() = coroutineScope {
        val remoteUrl = URI.create(fileURL).toURL()
        val connection = remoteUrl.openConnection() as HttpURLConnection
        connection.setRequestProperty("Range", "bytes=$startByte-$endByte")
        connection.connect()

        val input = connection.inputStream
        val output = RandomAccessFile(fileName, "rw")
        output.seek(startByte)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while ((input.read(buffer, 0, 4096).also { bytesRead = it }) != -1) {
            output.write(buffer, 0, bytesRead)
            chunkProgressListener.onProgressUpdate(bytesRead, totalFileSize)
        }

        input.close()
        output.close()
    }
}


interface ProgressListener {
    suspend fun onProgressUpdate(bytesRead: Int, fileTotalSize: Long)
    suspend fun onDownloadComplete(fileSize: Long, downloadedIn: Long)
}

