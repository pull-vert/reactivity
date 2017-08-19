package client

import io.khttp2.Http2Server

object Http2ServerMain {
    fun syncHtt2Server() {
        Http2Server.newHttp2Server()
    }

    @JvmStatic fun main(vararg args: String) {
        syncHtt2Server()
    }
}