package com.example.keyboard

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_notes")
data class SavedNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "keyboard_settings")
data class KeyboardSettings(
    @PrimaryKey val id: Int = 1, // Singleton row
    val themeName: String = "COSMIC",
    val animationIntensity: String = "BALANCED",
    val hapticEnabled: Boolean = true,
    val hapticWeight: Float = 0.5f,
    val boardScaleMultiplier: Float = 1.0f,
    val glowIntensity: Float = 0.6f,
    val borderThickness: Float = 1.0f,
    val cornerRadius: Int = 12,
    val backlightsSpeed: Float = 1.0f,
    val particlesCount: Int = 12
)
