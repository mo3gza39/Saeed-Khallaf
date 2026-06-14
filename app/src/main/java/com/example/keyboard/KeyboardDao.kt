package com.example.keyboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyboardDao {
    @Query("SELECT * FROM saved_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<SavedNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SavedNote)

    @Query("DELETE FROM saved_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM saved_notes")
    suspend fun clearAllNotes()

    @Query("SELECT * FROM keyboard_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<KeyboardSettings?>

    @Query("SELECT * FROM keyboard_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): KeyboardSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: KeyboardSettings)
}
