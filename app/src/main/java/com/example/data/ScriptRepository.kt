package com.example.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allHistory: Flow<List<ScriptItem>> = scriptDao.getAllHistory()

    suspend fun insert(item: ScriptItem): Long {
        return scriptDao.insertHistory(item)
    }

    suspend fun deleteById(id: Int) {
        scriptDao.deleteHistoryById(id)
    }

    suspend fun clearAll() {
        scriptDao.clearAllHistory()
    }
}
