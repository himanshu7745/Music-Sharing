package com.example.musicsharing.core.model

data class BufferedFrame(
    val playAtMs: Long,
    val pcmData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BufferedFrame

        if (playAtMs != other.playAtMs) return false
        if (!pcmData.contentEquals(other.pcmData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playAtMs.hashCode()
        result = 31 * result + pcmData.contentHashCode()
        return result
    }
}