package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.dao.UserProgressDao
import com.rpgahorro.savequest.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

class UserProgressRepository(
    private val userProgressDao: UserProgressDao
) {

    val progress: Flow<UserProgressEntity?> =
        userProgressDao.observeProgress()

    suspend fun addXp(amount: Int) {
        userProgressDao.addXp(amount)
    }
}