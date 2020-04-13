package com.verygoodsecurity.vgscollect.core.api

import androidx.annotation.VisibleForTesting
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Wraps a SSLSocketFactory and enables more TLS versions for older versions of Android.
 */
internal class TLSSocketFactory : SSLSocketFactory() {

    private val internalSSLSocketFactory: SSLSocketFactory

    internal companion object {
        private const val TLS_V11 = "TLSv1.1"
        private const val TLS_V12 = "TLSv1.2"
    }

    init {
        val context = SSLContext.getInstance("TLS")
        context.init(null, null, null)
        internalSSLSocketFactory = context.socketFactory
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return internalSSLSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return internalSSLSocketFactory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket()
        )
    }

    @Throws(IOException::class)
    override fun createSocket(
        s: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(s, host, port, autoClose)
        )
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(host, port)
        )
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(host, port, localHost, localPort)
        )
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(host, port)
        )
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket {
        return enableTLSOnSocket(
            internalSSLSocketFactory.createSocket(address, port, localAddress, localPort)
        )
    }

    private fun enableTLSOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = mergeProtocols(socket.enabledProtocols)
        }
        return socket
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun mergeProtocols(protocols: Array<String>?): Array<String> {
        return protocols?.run {
            setOf(*this, TLS_V11, TLS_V12).toTypedArray()
        }?:arrayOf(TLS_V11, TLS_V12)
    }
}