package com.eemphasys.vitalconnect.common

import android.graphics.Color
import java.util.Random

object ParticipantColorManager {
//    private val participantColors = mutableMapOf<String, Int>()
//
//    @JvmStatic
//    fun getColorForParticipant(participantId: String): Int {
//        return participantColors[participantId] ?: generateRandomColor().also {
//            participantColors[participantId] = it
//        }
//    }
//
//    private fun generateRandomColor(): Int {
//        val random = Random()
////        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
//        val red = 200 + random.nextInt(56) // Random value between 200 and 255
//        val green = 200 + random.nextInt(56) // Random value between 200 and 255
//        val blue = 200 + random.nextInt(56) // Random value between 200 and 255
//        return Color.argb(255, red, green, blue)
//    }
private val participantColors = mutableMapOf<String, Int>()

    @JvmStatic
    fun getColorForParticipant(participantId: String): Int {
        return participantColors[participantId] ?: generateLightColor().also {
            participantColors[participantId] = it
        }
    }

    fun getDarkColorForParticipant(participantId: String): Int {
        val lightColor = participantColors[participantId]
        return if (lightColor != null) {
            generateDarkColor(lightColor)
        } else {
            generateDarkColor(generateLightColor())
        }
    }

    private fun generateLightColor(): Int {
        val random = Random()
        val red = 200 + random.nextInt(56) // Random value between 200 and 255
        val green = 200 + random.nextInt(56) // Random value between 200 and 255
        val blue = 200 + random.nextInt(56) // Random value between 200 and 255
        return Color.argb(255, red, green, blue)
    }

    private fun generateDarkColor(lightColor: Int): Int {
        val alpha = Color.alpha(lightColor)
        val red = (Color.red(lightColor) * 0.3).toInt() // Adjust this factor for desired darkness
        val green = (Color.green(lightColor) * 0.3).toInt() // Adjust this factor for desired darkness
        val blue = (Color.blue(lightColor) * 0.3).toInt() // Adjust this factor for desired darkness
        return Color.argb(alpha, red, green, blue)
    }
}