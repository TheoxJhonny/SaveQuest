package com.rpgahorro.savequest.data.local.entity

import androidx.room.*

@Entity(tableName = "shared_expenses", foreignKeys = [
    ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["payerContactId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("accountId"), Index("payerContactId")])
data class SharedExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: Long,
    val payerContactId: Long?,
    val accountId: Long?,
    val total: Long,
    val ownExpense: Long,
    val cashOutflow: Long
)

@Entity(tableName = "shared_items", primaryKeys = ["expenseId", "position"], foreignKeys = [
    ForeignKey(entity = SharedExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.RESTRICT)
])
data class SharedItemEntity(val expenseId: String, val position: Int, val name: String, val quantity: Long, val unitPrice: Long)

@Entity(tableName = "shared_shares", primaryKeys = ["expenseId", "participantId"], foreignKeys = [
    ForeignKey(entity = SharedExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["contactId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("contactId")])
data class SharedShareEntity(val expenseId: String, val participantId: Long, val contactId: Long?, val name: String, val amount: Long)

data class SharedExpenseDetails(
    @Embedded val expense: SharedExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId") val items: List<SharedItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "expenseId") val shares: List<SharedShareEntity>
) {
    val receivable: Long get() = if (expense.payerContactId == null) expense.total - expense.ownExpense else 0
    val payable: Long get() = if (expense.payerContactId != null) expense.ownExpense else 0
}
