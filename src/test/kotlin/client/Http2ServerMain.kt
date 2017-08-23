package client

import io.http2.koala.KHttp2Server

fun syncHtt2Server() {
    KHttp2Server.newHttp2Server()
}

fun main(args: Array<String>) {
    syncHtt2Server()
}