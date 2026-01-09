package com.example.musicsharing.server

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

class AudioHttpServer(
    port: Int,
    private val resolver: ContentResolver,
    private val audioUri: Uri
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "AudioHttpServer"
        private const val DEFAULT_MIME_TYPE = "audio/mpeg"
    }

    override fun serve(session: IHTTPSession): Response {
        Log.d(TAG, "Request: ${session.method} ${session.uri} from ${session.remoteIpAddress}")

        return when (session.uri) {
            "/", "/audio" -> serveAudio(session)
            "/info" -> serveInfo()
            else -> notFound()
        }
    }

    private fun serveAudio(session: IHTTPSession): Response {
        return try {
            val afd = resolver.openAssetFileDescriptor(audioUri, "r")
                ?: return error("Audio not accessible")

            val fileLength = afd.length
            val mimeType = resolver.getType(audioUri) ?: DEFAULT_MIME_TYPE
            val rangeHeader = session.headers["range"]

            var start = 0L
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                start = rangeHeader
                    .removePrefix("bytes=")
                    .substringBefore("-")
                    .toLong()
            }

            val inputStream = afd.createInputStream().apply {
                skip(start)
            }

            val status = if (rangeHeader != null)
                Response.Status.PARTIAL_CONTENT
            else
                Response.Status.OK

            val response = newFixedLengthResponse(
                status,
                mimeType,
                inputStream,
                fileLength - start
            )

            response.addHeader("Accept-Ranges", "bytes")

            if (rangeHeader != null) {
                response.addHeader(
                    "Content-Range",
                    "bytes $start-${fileLength - 1}/$fileLength"
                )
            }

            response
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error", e)
            error("Streaming failed")
        }
    }


    private fun serveInfo(): Response {
        val json = """
            {
              "status": "online",
              "endpoint": "/audio",
              "server": "AudioHttpServer"
            }
        """.trimIndent()

        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            json
        )
    }

    private fun notFound(): Response =
        newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            MIME_PLAINTEXT,
            "Endpoint not found. Use /audio"
        )

    private fun error(message: String): Response =
        newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR,
            MIME_PLAINTEXT,
            message
        )

    override fun stop() {
        Log.d(TAG, "HTTP server stopped")
        super.stop()
    }
}
