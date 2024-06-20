package com.eemphasys.vitalconnect.common

import android.graphics.Color
import java.util.Random

object ParticipantColorManager {
    private val participantColors = mutableMapOf<String, Int>()

    @JvmStatic
    fun getColorForParticipant(participantId: String): Int {
        return participantColors[participantId] ?: generateRandomColor().also {
            participantColors[participantId] = it
        }
    }

    private fun generateRandomColor(): Int {
        val random = Random()
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }
}