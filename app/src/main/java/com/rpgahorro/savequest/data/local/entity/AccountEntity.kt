package com.rpgahorro.savequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index(value = ["name"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class AccountBalance(
    @Embedded val account: AccountEntity,
    val balance: Long,
    val reserved: Long
) {
    val available: Long get() = balance - reserved
}
