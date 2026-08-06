package com.kimro.ai.lotto.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_table",
    indices = [
        Index(value = ["round", "numbers"], unique = true)
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val round: Int,

    val numbers: String,

    val rankResult: String = "미확인"
)
