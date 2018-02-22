package reactivity.experimental.http2.ssl

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
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
    engine.needClientAuth = true
}

fun doHandshake(socketChannel: SocketChannel, engine: SSLEngine,
                myNetData: ByteBuffer, peerNetData: ByteBuffer) {
    // Create byte buffers to use for holding application data
    val appBufferSize = engine.session.applicationBufferSize
    val myAppData = ByteBuffer.allocate(appBufferSize)
    val peerAppData = ByteBuffer.allocate(appBufferSize)

    // Begin handshake
    engine.beginHandshake()
    val hs = engine.handshakeStatus

    // Process handshaking message
    while (hs != HandshakeStatus.FINISHED &&
            hs != HandshakeStatus.NOT_HANDSHAKING) {
        when (hs) {

            NEED_UNWRAP -> {
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
            }
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
}