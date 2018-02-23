package reactivity.experimental.http2.ssl

import org.eclipse.jetty.alpn.ALPN
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.TrustManagerFactory


// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSocketFactory
// http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html
// http://atetric.com/atetric/javadoc/org.apache.tomcat/tomcat-coyote/8.0.26/src-html/org/apache/tomcat/util/net/Nio2Channel.html le plus intéressant pour le moment !
// https://github.com/ThreaT/WebServers/blob/master/src/main/java/com/webservers/HttpsServer.java

fun createSSLEngine(
        port: Int
) {
    // Create and initialize the SSLContext with key material
    val passphrase = "passphrase".toCharArray()

    // First initialize the key and trust material
    val ksKeys = KeyStore.getInstance("JKS")
    ksKeys.load(FileInputStream("testKeys"), passphrase)
    val ksTrust = KeyStore.getInstance("JKS")
    ksTrust.load(FileInputStream("testTrust"), passphrase)

    // KeyManagers decide which key material to use
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(ksKeys, passphrase)

    // TrustManagers decide whether to allow connections
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ksTrust)

    val sslContext = SSLContext.getInstance("TLSv1.2")
    sslContext.init(kmf.keyManagers, tmf.trustManagers, null)

    // Create the engine
    val engine = sslContext.createSSLEngine("localhost", port)
    engine.useClientMode = false
//    engine.needClientAuth = true // really needed ?
}

class SSLEnginAlpn {

    fun performTLSHandshake(serverSSLEngine: SSLEngine) {
        val encrypted = ByteBuffer.allocate(serverSSLEngine.session.packetBufferSize)
        val decrypted = ByteBuffer.allocate(serverSSLEngine.session.applicationBufferSize)

        ALPN.put(serverSSLEngine, object : ALPN.ServerProvider {
            override fun unsupported() {
                ALPN.remove(serverSSLEngine)
            }

            override fun select(protocols: List<String>): String {
                ALPN.remove(serverSSLEngine)
                return protocols[0]
            }
        })
        serverSSLEngine.beginHandshake()
//    Assert.assertSame(SSLEngineResult.HandshakeStatus.NEED_UNWRAP, serverSSLEngine.getHandshakeStatus());

        // Read the ClientHello
        unwrap(serverSSLEngine, encrypted, decrypted)
        // Generate and write ServerHello (and other messages)
        wrap(serverSSLEngine, decrypted, encrypted)
        // Read ClientKeyExchange, ChangeCipherSpec and Finished
        unwrap(serverSSLEngine, encrypted, decrypted)
        // Generate and write ChangeCipherSpec and Finished
        wrap(serverSSLEngine, decrypted, encrypted)

//        Assert.assertSame(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, serverSSLEngine.getHandshakeStatus());

    }

    protected fun performTLSClose(serverSSLEngine: SSLEngine) {
        val encrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getPacketBufferSize())
        val decrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getApplicationBufferSize())

        unwrap(serverSSLEngine, encrypted, decrypted)
        wrap(serverSSLEngine, decrypted, encrypted)

//        Assert.assertSame(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, serverSSLEngine.getHandshakeStatus())
    }

    protected fun performDataExchange(serverSSLEngine: SSLEngine) {
        val encrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getPacketBufferSize())
        val decrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getApplicationBufferSize())

//        Assert.assertSame(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, serverSSLEngine.getHandshakeStatus())

        // Write the data.
        encrypted.clear()
        decrypted.clear()

        // Read the data.
        encrypted.flip()
        decrypted.clear()
        var result = serverSSLEngine.unwrap(encrypted, decrypted)
//        Assert.assertSame(SSLEngineResult.Status.OK, result.getStatus())

        // Write the data back =
        encrypted.clear()
        decrypted.flip()
        result = serverSSLEngine.wrap(decrypted, encrypted)
//        Assert.assertSame(SSLEngineResult.Status.OK, result.getStatus())
    }

    protected fun performTLSRenegotiation(serverSSLEngine: SSLEngine) {
        val encrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getPacketBufferSize())
        val decrypted = ByteBuffer.allocate(serverSSLEngine.getSession().getApplicationBufferSize())

        serverSSLEngine.beginHandshake()
//            Assert.assertSame(SSLEngineResult.HandshakeStatus.NEED_WRAP, serverSSLEngine.getHandshakeStatus())

         wrap(serverSSLEngine, decrypted, encrypted)
        unwrap(serverSSLEngine, encrypted, decrypted)
        wrap(serverSSLEngine, decrypted, encrypted)
        unwrap(serverSSLEngine, encrypted, decrypted)

//        Assert.assertSame(SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, serverSSLEngine.getHandshakeStatus())
    }

    private fun wrap(sslEngine: SSLEngine, decrypted: ByteBuffer, encrypted: ByteBuffer) {
        encrypted.clear()
        val tmp = ByteBuffer.allocate(encrypted.capacity())
        while (true) {
            encrypted.clear()
            val result = sslEngine.wrap(decrypted, encrypted)
            val status = result.status
            if (status != SSLEngineResult.Status.OK && status != SSLEngineResult.Status.CLOSED)
                throw AssertionError(status.toString())
            encrypted.flip()
            tmp.put(encrypted)
            if (result.handshakeStatus != SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                tmp.flip()
                encrypted.clear()
                encrypted.put(tmp).flip()
                return
            }
        }
    }

    private fun unwrap(sslEngine: SSLEngine, encrypted: ByteBuffer, decrypted: ByteBuffer) {
        decrypted.clear()
        while (true) {
            decrypted.clear()
            val result = sslEngine.unwrap(encrypted, decrypted)
            val status = result.status
            if (status != SSLEngineResult.Status.OK && status != SSLEngineResult.Status.CLOSED)
                throw AssertionError(status.toString())
            var handshakeStatus: SSLEngineResult.HandshakeStatus = result.handshakeStatus
            if (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                sslEngine.delegatedTask.run()
                handshakeStatus = sslEngine.handshakeStatus
            }
            if (handshakeStatus != SSLEngineResult.HandshakeStatus.NEED_UNWRAP)
                return
        }
    }
}

//fun doHandshake(socketChannel: SocketChannel, engine: SSLEngine,
//                myNetData: ByteBuffer, peerNetData: ByteBuffer) {
//    // Create byte buffers to use for holding application data
//    val appBufferSize = engine.session.applicationBufferSize
//    val myAppData = ByteBuffer.allocate(appBufferSize)
//    val peerAppData = ByteBuffer.allocate(appBufferSize)
//
//    // Begin handshake
//    engine.beginHandshake()
//    val hs = engine.handshakeStatus
//
//    // Process handshaking message
//    while (hs != HandshakeStatus.FINISHED &&
//            hs != HandshakeStatus.NOT_HANDSHAKING) {
//        when (hs) {
//
//            NEED_UNWRAP -> {
                // Receive handshaking data from peer
//            if (socketChannel.read(peerNetData) < 0) {
//                // The channel has reached end-of-stream
//            }
//
//            // Process incoming handshaking data
//            peerNetData.flip();
//            SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
//            peerNetData.compact();
//            hs = res.getHandshakeStatus();
//
//            // Check status
//            switch (res.getStatus()) {
//                case OK :
//                // Handle OK status
//                break;
//
//                // Handle other status: BUFFER_UNDERFLOW, BUFFER_OVERFLOW, CLOSED
//                ...
//            }
//            }
            // Receive handshaking data from peer
//            if (socketChannel.read(peerNetData) < 0) {
//                // The channel has reached end-of-stream
//            }
//
//            // Process incoming handshaking data
//            peerNetData.flip();
//            SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
//            peerNetData.compact();
//            hs = res.getHandshakeStatus();
//
//            // Check status
//            switch (res.getStatus()) {
//                case OK :
//                // Handle OK status
//                break;
//
//                // Handle other status: BUFFER_UNDERFLOW, BUFFER_OVERFLOW, CLOSED
//                ...
//            }
//            break;
//
//            case NEED_WRAP :
//            // Empty the local network packet buffer.
//            myNetData.clear();
//
//            // Generate handshaking data
//            res = engine.wrap(myAppData, myNetData);
//            hs = res.getHandshakeStatus();
//
//            // Check status
//            switch (res.getStatus()) {
//                case OK :
//                myNetData.flip();
//
//                // Send the handshaking data to peer
//                while (myNetData.hasRemaining()) {
//                    socketChannel.write(myNetData);
//                }
//                break;
//
//                // Handle other status:  BUFFER_OVERFLOW, BUFFER_UNDERFLOW, CLOSED
//                ...
//            }
//            break;
//
//            case NEED_TASK :
//            // Handle blocking tasks
//            break;
//
//            // Handle other status:  // FINISHED or NOT_HANDSHAKING
//            ...
//        }
//    }
//
//    // Processes after handshaking
//    ...
//}
//}