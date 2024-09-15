package com.mathias8dev.simplefiledownloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main() {
    println("Hello downloader!")
    runBlocking(Dispatchers.IO) {
        /*launch {
            DownloadApplication.downloadFile(
                "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
                "sample-mp4-file.mp4",
            )
        }*/

        launch {
            DownloadApplication.downloadFile(
                "http://212.183.159.230/512MB.zip",
                "test.zip",
            )
        }
    }
}



