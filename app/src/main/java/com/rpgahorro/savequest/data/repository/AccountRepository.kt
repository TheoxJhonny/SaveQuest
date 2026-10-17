package com.rpgahorro.savequest.data.repository

import com.rpgahorro.savequest.data.local.dao.AccountDao

class AccountRepository(private val dao: AccountDao) {
    val accounts = dao.observeBalances()
    suspend fun create(name: String): Long = dao.create(name)
}
