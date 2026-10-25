package com.gpufreq.patcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patches")
data class ModulePatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val originalFreq: Int,
    val originalVolt: Int,
    val targetFreq: Int,
    val targetVolt: Int,
    val outputPath: String,
    val timestamp: Long = System.currentTimeMillis()
)
