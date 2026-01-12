package com.example.musicsharing.core.model

data class AudioPacket(
    val sequence: Long,
    val playAtMs: Long,
    val expireAtMs: Long,
    val pcmData: ByteArray,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioPacket

        if (sequence != other.sequence) return false
        if (playAtMs != other.playAtMs) return false
        if (expireAtMs != other.expireAtMs) return false
        if (!pcmData.contentEquals(other.pcmData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequence.hashCode()
        result = 31 * result + playAtMs.hashCode()
        result = 31 * result + expireAtMs.hashCode()
        result = 31 * result + pcmData.contentHashCode()
        return result
    }
}