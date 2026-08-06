// app/src/main/java/com/kimro/ai/lotto/data/PurchaseEntity.kt
package com.kimro.ai.lotto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_table")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val round: Int,
    val numbers: String,
    val rankResult: String = "미확인"
)

