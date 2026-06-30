package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "script_history")
data class ScriptItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "OBFUSCATE" or "DEOBFUSCATE"
    val originalScript: String,
    val processedScript: String,
    val timestamp: Long = System.currentTimeMillis(),
    val analysis: String? = null // Optional AI analysis/explanation
) : Serializable
