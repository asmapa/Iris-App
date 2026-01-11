package com.example.iris_new.face.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FaceDao {

    @Insert
    suspend fun insertFace(face: FaceEntity)

    @Query("SELECT * FROM faces")
    suspend fun getAllFaces(): List<FaceEntity>

    @Query("DELETE FROM faces WHERE id = :id")
    suspend fun deleteFace(id: Int)

    @Query("DELETE FROM faces")
    suspend fun deleteAll()
}
