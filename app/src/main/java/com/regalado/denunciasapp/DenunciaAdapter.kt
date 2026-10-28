package com.regalado.denunciasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.regalado.denunciasapp.api.Denuncia
import com.regalado.denunciasapp.databinding.ItemDenunciaBinding
import com.regalado.denunciasapp.util.bindEstado
import com.regalado.denunciasapp.util.formatFecha

/**
 * Adapter para el RecyclerView de denuncias (ID, DNI, Tipo, Estado).
 */
class DenunciaAdapter(
    private val onClick: (Denuncia) -> Unit
) : ListAdapter<Denuncia, DenunciaAdapter.DenunciaViewHolder>(DIFF) {

    inner class DenunciaViewHolder(
        private val binding: ItemDenunciaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(denuncia: Denuncia) = with(binding) {
            val ctx = root.context
            tvId.text = ctx.getString(R.string.fmt_id_corto, denuncia.id ?: 0)
            tvTipo.text = denuncia.tipo
            tvDni.text = ctx.getString(R.string.fmt_dni, denuncia.dni)
            tvEstado.bindEstado(denuncia.estado)
            tvFecha.text = formatFecha(denuncia.fecha)

            tvDireccion.text = "📍 ${denuncia.direccion ?: "—"}"
            if (denuncia.descripcion.isNullOrBlank()) {
                tvDescripcion.visibility = View.GONE
            } else {
                tvDescripcion.visibility = View.VISIBLE
                tvDescripcion.text = denuncia.descripcion
            }

            root.setOnClickListener { onClick(denuncia) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenunciaViewHolder {
        val binding = ItemDenunciaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DenunciaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DenunciaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Denuncia>() {
            override fun areItemsTheSame(oldItem: Denuncia, newItem: Denuncia) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Denuncia, newItem: Denuncia) = oldItem == newItem
        }
    }
}
