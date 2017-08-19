package client

import khttp2.Http2Server

object Http2ServerMain {
    fun syncHtt2Server() {
        val server = Http2Server.newHttp2Server()
    }

    @JvmStatic fun main(vararg args: String) {
        syncHtt2Server()
    }
}