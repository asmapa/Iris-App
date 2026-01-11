package com.example.iris_new.face.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faces")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    // Face embedding stored as ByteArray
    val embedding: ByteArray
)
