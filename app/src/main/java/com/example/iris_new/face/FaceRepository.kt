package com.example.iris_new.face

import com.example.iris_new.face.db.FaceDao
import com.example.iris_new.face.db.FaceEntity
import kotlin.math.sqrt

class FaceRepository(
    private val dao: FaceDao
) {

    suspend fun addFace(name: String, embedding: FloatArray) {
        dao.insertFace(
            FaceEntity(
                name = name,
                embedding = floatArrayToByteArray(embedding)
            )
        )
    }

    suspend fun getAllFaces(): List<FaceEntity> {
        return dao.getAllFaces()
    }

    suspend fun deleteFace(id: Int) {
        dao.deleteFace(id)
    }

    suspend fun findMatch(embedding: FloatArray): String? {
        val faces = dao.getAllFaces()

        var bestDistance = Float.MAX_VALUE
        var bestName: String? = null

        for (face in faces) {
            val stored = byteArrayToFloatArray(face.embedding)
            val dist = euclideanDistance(stored, embedding)

            if (dist < bestDistance && dist < 1.0f) {
                bestDistance = dist
                bestName = face.name
            }
        }
        return bestName
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun floatArrayToByteArray(array: FloatArray): ByteArray {
        val buffer = java.nio.ByteBuffer.allocate(array.size * 4)
        buffer.order(java.nio.ByteOrder.nativeOrder())
        for (v in array) buffer.putFloat(v)
        return buffer.array()
    }

    private fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        buffer.order(java.nio.ByteOrder.nativeOrder())
        val array = FloatArray(bytes.size / 4)
        for (i in array.indices) array[i] = buffer.getFloat()
        return array
    }
}
