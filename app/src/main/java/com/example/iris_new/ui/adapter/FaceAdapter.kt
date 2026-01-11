package com.example.iris_new.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.iris_new.databinding.ItemFaceBinding
import com.example.iris_new.face.db.FaceEntity

class FaceAdapter(
    private var faces: List<FaceEntity>,
    private val onDelete: (FaceEntity) -> Unit
) : RecyclerView.Adapter<FaceAdapter.FaceViewHolder>() {

    inner class FaceViewHolder(
        private val binding: ItemFaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(face: FaceEntity) {
            binding.faceName.text = face.name
            binding.deleteButton.setOnClickListener {
                onDelete(face)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceViewHolder {
        val binding = ItemFaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaceViewHolder, position: Int) {
        holder.bind(faces[position])
    }

    override fun getItemCount() = faces.size

    fun updateData(newFaces: List<FaceEntity>) {
        faces = newFaces
        notifyDataSetChanged()
    }
}
