package client

import io.khttp2.Http2Server

fun syncHtt2Server() {
    Http2Server.newHttp2Server()
}

fun main(args: Array<String>) {
    syncHtt2Server()
}