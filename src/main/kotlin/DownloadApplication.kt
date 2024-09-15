package com.mathias8dev.simplefiledownloader

import com.mathias8dev.simplefiledownloader.domain.FastFileDownloader
import com.mathias8dev.simplefiledownloader.domain.FastFileDownloaderController
import com.mathias8dev.simplefiledownloader.domain.ProgressListener
import com.mathias8dev.simplefiledownloader.domain.asHumanReadableSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


object DownloadApplication {

    suspend fun downloadFile(fileURL: String, fileName: String) = coroutineScope {
        val controller = FastFileDownloaderController()
        val progressListener = object : ProgressListener {
            val mutex = Mutex()
            var totalBytesDownloaded = 0L
            override suspend fun onProgressUpdate(bytesDownloaded: Long, totalFileSize: Long) {
                mutex.withLock {
                    totalBytesDownloaded += bytesDownloaded
                }
                println("Downloaded ${totalBytesDownloaded.asHumanReadableSize()} bytes out of ${totalFileSize.asHumanReadableSize()}")
            }

            override suspend fun onDownloadComplete(totalFileSize: Long, timeTaken: Long) {
                println("Download complete: ${totalFileSize.asHumanReadableSize()} bytes in $timeTaken ms")
            }
        }
        val fileDownloader = FastFileDownloader(fileURL, fileName, progressListener, controller)
        kotlin.runCatching {
            launch {
                fileDownloader.download()
            }

            launch {
                delay(2000)
                controller.pause()
                delay(2000)
                controller.resume()
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

}
