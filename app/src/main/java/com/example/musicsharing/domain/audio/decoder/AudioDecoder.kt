package com.example.musicsharing.domain.audio.decoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.musicsharing.core.model.PcmFrame
import com.example.musicsharing.core.constant.AudioConstants
import com.example.musicsharing.core.constant.AudioConstants.CODEC_DEQUEUE_TIMEOUT_US

class AudioDecoder(
    private val context: Context,
    private val audioUri: Uri
) {

    fun decode(onFrameDecoded: (PcmFrame) -> Unit) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, audioUri, null)

        val trackIndex = selectAudioTrack(extractor)
            ?: throw IllegalStateException("No audio track found")

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalStateException("No MIME type")

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()

        var frameIndex = 0L
        var pcmAccumulator = ByteArray(0)

        var isEOS = false

        while (true) {

            // Feed decoder
            if (!isEOS) {
                val inputIndex = codec.dequeueInputBuffer(CODEC_DEQUEUE_TIMEOUT_US)
                if (inputIndex >= 0) {
                    val inputBuffer = inputBuffers[inputIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            sampleSize,
                            extractor.sampleTime,
                            0
                        )
                        extractor.advance()
                    }
                }
            }

            // Drain decoder
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_DEQUEUE_TIMEOUT_US)
            if (outputIndex >= 0) {
                val outputBuffer = outputBuffers[outputIndex]
                val chunk = ByteArray(bufferInfo.size)

                outputBuffer.get(chunk)
                outputBuffer.clear()

                codec.releaseOutputBuffer(outputIndex, false)

                val monoPcm = convertToMonoIfNeeded(chunk, format)
                pcmAccumulator += monoPcm

                while (pcmAccumulator.size >= AudioConstants.FRAME_BYTES) {
                    val frameData = pcmAccumulator.copyOfRange(0, AudioConstants.FRAME_BYTES.toInt())
                    pcmAccumulator = pcmAccumulator.copyOfRange(
                        AudioConstants.FRAME_BYTES.toInt(),
                        pcmAccumulator.size
                    )

                    onFrameDecoded(
                        PcmFrame(
                            index = frameIndex++,
                            data = frameData
                        )
                    )
                }

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private fun convertToMonoIfNeeded(
        pcm: ByteArray,
        format: MediaFormat
    ): ByteArray {

        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (channels == 1) return pcm

        // Stereo → Mono (average L + R), LITTLE-ENDIAN SAFE
        val mono = ByteArray(pcm.size / 2)

        var i = 0
        var j = 0

        while (i + 3 < pcm.size) {

            val left = (
                    (pcm[i].toInt() and 0xFF) or
                            ((pcm[i + 1].toInt() and 0xFF) shl 8)
                    ).toShort()

            val right = (
                    (pcm[i + 2].toInt() and 0xFF) or
                            ((pcm[i + 3].toInt() and 0xFF) shl 8)
                    ).toShort()

            val avg = ((left + right) / 2).toShort()

            mono[j]     = (avg.toInt() and 0xFF).toByte()
            mono[j + 1] = ((avg.toInt() shr 8) and 0xFF).toByte()

            i += 4
            j += 2
        }

        return mono
    }

}