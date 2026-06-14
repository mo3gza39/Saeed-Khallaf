package com.example.keyboard

import kotlinx.coroutines.flow.Flow

class KeyboardRepository(private val keyboardDao: KeyboardDao) {
    val allNotes: Flow<List<SavedNote>> = keyboardDao.getAllNotes()
    val settingsFlow: Flow<KeyboardSettings?> = keyboardDao.getSettingsFlow()

    suspend fun insertNote(content: String) {
        val note = SavedNote(content = content)
        keyboardDao.insertNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        keyboardDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        keyboardDao.clearAllNotes()
    }

    suspend fun getSettingsDirect(): KeyboardSettings? {
        return keyboardDao.getSettingsDirect()
    }

    suspend fun saveSettings(settings: KeyboardSettings) {
        keyboardDao.saveSettings(settings)
    }
}
