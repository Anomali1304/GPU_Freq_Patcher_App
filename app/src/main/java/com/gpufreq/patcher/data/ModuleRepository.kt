package com.gpufreq.patcher.data

import kotlinx.coroutines.flow.Flow

class ModuleRepository(private val moduleDao: ModuleDao) {
    val allPatches: Flow<List<ModulePatch>> = moduleDao.getAllPatches()

    suspend fun insert(patch: ModulePatch) = moduleDao.insertPatch(patch)

    suspend fun deleteById(id: Int) = moduleDao.deletePatchById(id)

    suspend fun deleteAllPatches() = moduleDao.deleteAllPatches()
}
