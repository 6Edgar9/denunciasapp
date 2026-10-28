package com.regalado.denunciasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.regalado.denunciasapp.api.ApiResult
import com.regalado.denunciasapp.api.Denuncia
import com.regalado.denunciasapp.api.RetrofitClient
import com.regalado.denunciasapp.api.safeApiCall
import com.regalado.denunciasapp.databinding.ActivityDetalleBinding
import com.regalado.denunciasapp.util.applySystemBarsPadding
import com.regalado.denunciasapp.util.bindEstado
import com.regalado.denunciasapp.util.formatFecha
import kotlinx.coroutines.launch

/**
 * Pantalla de Detalle de una denuncia (GET /denuncias/{id})
 * Incluye botón "Atender" (PUT /denuncias/{id}) y botón "Buscar por DNI".
 */
class DetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleBinding
    private val api = RetrofitClient.denunciaApi
    private var denunciaId: Int = -1
    private var denunciaActual: Denuncia? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        denunciaId = intent.getIntExtra(EXTRA_ID, -1)
        if (denunciaId == -1) {
            Toast.makeText(this, R.string.err_id_invalido, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnReintentar.setOnClickListener { cargarDetalle() }
        binding.btnAtender.setOnClickListener { confirmarAtender() }
        binding.btnBuscarDni.setOnClickListener {
            denunciaActual?.dni?.let { dni ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_BUSCAR_DNI, dni)
                }
                setResult(RESULT_BUSCAR_DNI, resultIntent)
                finish()
            }
        }

        cargarDetalle()
    }

    /** GET /denuncias/{id} */
    private fun cargarDetalle() {
        mostrarEstado(cargando = true)

        lifecycleScope.launch {
            when (val result = safeApiCall { api.obtenerPorId(denunciaId) }) {
                is ApiResult.Success -> {
                    mostrarEstado(cargando = false)
                    denunciaActual = result.data
                    mostrarDenuncia(result.data)
                }
                is ApiResult.Error -> mostrarEstado(cargando = false, error = result.message)
            }
        }
    }

    private fun confirmarAtender() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_atender_titulo)
            .setMessage(R.string.dialog_atender_msg)
            .setNegativeButton(R.string.dialog_cancelar, null)
            .setPositiveButton(R.string.dialog_confirmar) { _, _ -> atender() }
            .show()
    }

    /** PUT /denuncias/{id} -> estado = 0 */
    private fun atender() {
        binding.btnAtender.isEnabled = false
        binding.btnAtender.setText(R.string.btn_atendiendo)

        lifecycleScope.launch {
            when (val result = safeApiCall { api.atender(denunciaId) }) {
                is ApiResult.Success -> {
                    denunciaActual = result.data
                    mostrarDenuncia(result.data)
                    setResult(RESULT_OK) // El listado se recargará al volver
                    Snackbar.make(binding.root, R.string.msg_atendida, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.btnAtender)
                        .show()
                }
                is ApiResult.Error -> {
                    binding.btnAtender.isEnabled = true
                    binding.btnAtender.setText(R.string.btn_atender)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_reintentar) { atender() }
                        .setAnchorView(binding.btnAtender)
                        .show()
                }
            }
        }
    }

    private fun mostrarDenuncia(d: Denuncia) = with(binding) {
        tvId.text = getString(R.string.fmt_id, d.id ?: denunciaId)
        tvTipo.text = d.tipo
        tvEstado.bindEstado(d.estado)
        tvDni.text = d.dni
        tvFecha.text = formatFecha(d.fecha)
        tvDireccion.text = d.direccion ?: "—"
        tvDescripcion.text =
            if (d.descripcion.isNullOrBlank()) getString(R.string.sin_descripcion) else d.descripcion

        btnBuscarDni.isEnabled = !d.dni.isNullOrBlank()

        val pendiente = d.estado != 0
        btnAtender.isEnabled = pendiente
        btnAtender.setText(if (pendiente) R.string.btn_atender else R.string.btn_ya_atendida)
    }

    private fun mostrarEstado(cargando: Boolean, error: String? = null) = with(binding) {
        progress.visibility = if (cargando) View.VISIBLE else View.GONE
        errorView.visibility = if (error != null) View.VISIBLE else View.GONE
        content.visibility = if (!cargando && error == null) View.VISIBLE else View.GONE
        tvError.text = error
        if (cargando || error != null) {
            btnAtender.isEnabled = false
            btnBuscarDni.isEnabled = false
        }
    }

    companion object {
        const val EXTRA_ID = "extra_denuncia_id"
        const val EXTRA_BUSCAR_DNI = "extra_buscar_dni"
        const val RESULT_BUSCAR_DNI = 200
    }
}
