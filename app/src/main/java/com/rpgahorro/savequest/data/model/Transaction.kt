package com.rpgahorro.savequest.data.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Long,
    val category: String,
    val description: String,
    val date: Long,
    val goalId: Long? = null
)