package com.rpgahorro.savequest.domain.shared

/** El identificador 0 representa a la persona que usa la app; los demás son IDs de contactos. */
const val SELF_PARTICIPANT = 0L

data class SharedItem(val name: String, val quantity: Long, val unitPrice: Long)

data class ContactDebt(val contactId: Long, val receivable: Long = 0, val payable: Long = 0)

/** Plan validado en pesos enteros. Aún no escribe movimientos, deudas ni recompensas. */
class SharedExpensePlan private constructor(
    val total: Long,
    val ownExpense: Long,
    val cashOutflow: Long,
    val debts: List<ContactDebt>,
    val shares: Map<Long, Long>
) {
    companion object {
        fun equal(
            items: List<SharedItem>,
            participantIds: List<Long>,
            payerId: Long
        ): SharedExpensePlan {
            validateParticipants(participantIds, payerId)
            val total = total(items)
            val participants = participantIds.sorted()
            val base = total / participants.size
            val remainder = total % participants.size
            // Orden estable: el residuo se distribuye de a un peso, empezando por el ID menor.
            val shares =
                participants.mapIndexed { index, id -> id to base + if (index.toLong() < remainder) 1L else 0L }
                    .toMap()
            return build(total, shares, payerId)
        }

        fun custom(
            items: List<SharedItem>,
            participantIds: List<Long>,
            payerId: Long,
            shares: Map<Long, Long>
        ): SharedExpensePlan {
            validateParticipants(participantIds, payerId)
            require(shares.keys == participantIds.toSet()) { "Debes asignar una parte a cada participante." }
            require(shares.values.all { it >= 0 }) { "Las partes no pueden ser negativas." }
            val total = total(items)
            require(exact {
                shares.values.fold(
                    0L,
                    Math::addExact
                )
            } == total) { "La suma de las partes debe coincidir con el total." }
            return build(total, shares.toSortedMap(), payerId)
        }

        private fun validateParticipants(ids: List<Long>, payerId: Long) {
            require(ids.size >= 2 && SELF_PARTICIPANT in ids && ids.any { it > 0 }) { "Inclúyete y agrega al menos un contacto." }
            require(ids.all { it >= 0 } && ids.distinct().size == ids.size) { "Los participantes deben ser válidos y no repetirse." }
            require(payerId in ids) { "Quien paga debe ser un participante." }
        }

        private fun total(items: List<SharedItem>): Long {
            require(items.isNotEmpty()) { "Agrega al menos un producto o servicio." }
            require(items.all { it.name.trim().length in 1..100 && it.quantity > 0 && it.unitPrice > 0 }) {
                "Cada producto debe tener nombre, cantidad y precio mayores que cero."
            }
            return exact {
                items.fold(0L) { sum, item ->
                    Math.addExact(
                        sum,
                        Math.multiplyExact(item.quantity, item.unitPrice)
                    )
                }
            }
        }

        private fun build(total: Long, shares: Map<Long, Long>, payerId: Long): SharedExpensePlan {
            val own = shares.getValue(SELF_PARTICIPANT)
            val debts =
                if (payerId == SELF_PARTICIPANT) shares.filter { it.key != SELF_PARTICIPANT && it.value > 0 }
                    .map { ContactDebt(contactId = it.key, receivable = it.value) }
                else if (own > 0) listOf(
                    ContactDebt(
                        contactId = payerId,
                        payable = own
                    )
                ) else emptyList()
            return SharedExpensePlan(
                total,
                own,
                if (payerId == SELF_PARTICIPANT) total else 0L,
                debts,
                shares.toMap()
            )
        }

        private fun exact(block: () -> Long): Long = try {
            block()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("El monto supera el máximo permitido.")
        }
    }
}
