package com.rpgahorro.savequest.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rpgahorro.savequest.data.local.entity.TransactionEntity
import com.rpgahorro.savequest.data.model.TransactionType
import com.rpgahorro.savequest.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.rpgahorro.savequest.data.repository.UserProgressRepository
import com.rpgahorro.savequest.data.repository.ChallengeRepository
import kotlinx.coroutines.flow.first

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val challengeRepository: ChallengeRepository
) : ViewModel() {

    val transactions: Flow<List<TransactionEntity>> =
        repository.transactions

    fun addTransaction(
        type: TransactionType,
        amount: Long,
        category: String,
        description: String
    ) {

        val transaction = TransactionEntity(
            type = type.name,
            amount = amount,
            category = category,
            description = description,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch {

            // 1. Guardar el movimiento UNA sola vez
            repository.insert(transaction)

            // 2. Dar XP base por registrar el movimiento
            userProgressRepository.addXp(10)

            // 3. Buscar el desafío "Mantén el ritmo"
            val challenge = challengeRepository
                .challenges
                .first()
                .firstOrNull {
                    it.id == "track_3_transactions"
                }

            if (
                challenge != null &&
                !challenge.completed
            ) {

                val newProgress =
                    (challenge.progress + 1)
                        .coerceAtMost(challenge.target)

                challengeRepository.updateProgress(
                    challengeId = challenge.id,
                    progress = newProgress
                )

                // 4. Comprobar si se completó
                if (newProgress >= challenge.target) {

                    challengeRepository.completeChallenge(
                        challengeId = challenge.id
                    )

                    // 5. Entregar recompensa una sola vez
                    if (!challenge.rewardClaimed) {

                        userProgressRepository.addXp(
                            challenge.xpReward
                        )

                        challengeRepository.markRewardClaimed(
                            challengeId = challenge.id
                        )
                    }
                }
            }
        }
    }
    }
