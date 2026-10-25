package com.gpufreq.patcher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM patches ORDER BY timestamp DESC")
    fun getAllPatches(): Flow<List<ModulePatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatch(patch: ModulePatch)

    @Query("DELETE FROM patches WHERE id = :id")
    suspend fun deletePatchById(id: Int)

    @Query("DELETE FROM patches")
    suspend fun deleteAllPatches()
}
