package com.mathias8dev.simplefiledownloader.domain


interface ProgressListener {
    suspend fun onProgressUpdate(bytesDownloaded: Long, totalFileSize: Long)
    suspend fun onDownloadComplete(totalFileSize: Long, timeTaken: Long)
}
