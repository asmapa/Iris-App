package com.example.iris_new.face

import android.util.Log
import com.example.iris_new.face.db.FaceDao
import com.example.iris_new.face.db.FaceEntity
import kotlin.math.sqrt

class FaceRepository(
    private val dao: FaceDao
) {

    // ---------------- ADD FACE ----------------
    suspend fun addFace(name: String, embedding: FloatArray) {
        Log.d("FACE_DB", "Saving face for $name")

        dao.insertFace(
            FaceEntity(
                name = name,
                embedding = floatArrayToByteArray(embedding)
            )
        )

        Log.d("FACE_DB", "Face saved successfully")
    }

    // ---------------- GET ALL FACES ----------------
    suspend fun getAllFaces(): List<FaceEntity> {
        val faces = dao.getAllFaces()
        Log.d("FACE_DB", "Stored faces count = ${faces.size}")
        return faces
    }

    // ---------------- DELETE FACE ----------------
    suspend fun deleteFace(id: Int) {
        Log.d("FACE_DB", "Deleting face id = $id")
        dao.deleteFace(id)
    }

    // ---------------- NORMALIZATION ----------------
    private fun normalize(vec: FloatArray): FloatArray {
        var sum = 0f
        for (v in vec) sum += v * v

        val mag = sqrt(sum)

        if (mag == 0f) {
            Log.e("FACE_ERROR", "Zero magnitude vector!")
            return vec
        }

        return vec.map { it / mag }.toFloatArray()
    }

    // ---------------- FACE MATCHING ----------------
    suspend fun findMatchWithScore(embedding: FloatArray): Pair<String, Float>? {

        Log.d("FACE_DEBUG", "==============================")
        Log.d("FACE_DEBUG", "STARTING FACE COMPARISON")

        // 🔴 LIVE VECTOR
        Log.d("LIVE_VECTOR_RAW", embedding.joinToString(","))

        val normalizedInput = normalize(embedding)

        Log.d("LIVE_VECTOR_NORMALIZED", normalizedInput.joinToString(","))

        val faces = dao.getAllFaces()

        if (faces.isEmpty()) {
            Log.d("FACE_DEBUG", "No faces stored in DB")
            return null
        }

        var bestDistance = Float.MAX_VALUE
        var bestName: String? = null

        for (face in faces) {

            Log.d("FACE_DEBUG", "------------------------------")
            Log.d("FACE_DEBUG", "Comparing with stored face: ${face.name}")

            val storedArray = byteArrayToFloatArray(face.embedding)

            Log.d("STORED_VECTOR_RAW_${face.name}", storedArray.joinToString(","))

            val storedEmbedding = normalize(storedArray)

            Log.d("STORED_VECTOR_NORMALIZED_${face.name}", storedEmbedding.joinToString(","))

            val dist = euclideanDistance(storedEmbedding, normalizedInput)

            Log.d("FACE_DISTANCE", "${face.name} → $dist")

            if (dist < bestDistance) {
                bestDistance = dist
                bestName = face.name
            }
        }

        Log.d("FACE_DEBUG", "==============================")
        Log.d("FACE_DEBUG", "BEST MATCH: $bestName")
        Log.d("FACE_DEBUG", "BEST DISTANCE: $bestDistance")

        val THRESHOLD = 0.75f

        return if (bestName != null && bestDistance < THRESHOLD) {
            Log.d("FACE_RESULT", "RECOGNIZED PERSON: $bestName")
            Pair(bestName!!, bestDistance)
        } else {
            Log.d("FACE_RESULT", "UNKNOWN PERSON")
            null
        }
    }

    // ---------------- DISTANCE ----------------
    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {

        if (a.size != b.size) {
            Log.e("FACE_ERROR", "Vector size mismatch: ${a.size} vs ${b.size}")
            return Float.MAX_VALUE
        }

        var sum = 0f

        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    // ---------------- BYTE CONVERSION ----------------
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
