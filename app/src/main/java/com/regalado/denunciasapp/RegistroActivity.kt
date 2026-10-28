package com.regalado.denunciasapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.regalado.denunciasapp.api.ApiResult
import com.regalado.denunciasapp.api.Denuncia
import com.regalado.denunciasapp.api.RetrofitClient
import com.regalado.denunciasapp.api.safeApiCall
import com.regalado.denunciasapp.databinding.ActivityRegistroBinding
import com.regalado.denunciasapp.util.applySystemBarsPadding
import com.regalado.denunciasapp.util.esDniValido
import kotlinx.coroutines.launch

/**
 * Formulario para registrar una denuncia (POST /denuncias/).
 */
class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val api = RetrofitClient.denunciaApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.actvTipo.setSimpleItems(TIPOS_SUGERIDOS)
        binding.btnGuardar.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val denuncia = validarFormulario() ?: return
        setCargando(true)

        lifecycleScope.launch {
            when (val result = safeApiCall { api.registrar(denuncia) }) {
                is ApiResult.Success -> {
                    Toast.makeText(
                        this@RegistroActivity,
                        getString(R.string.msg_registrada, result.data.id ?: 0),
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is ApiResult.Error -> {
                    setCargando(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_reintentar) { registrar() }
                        .setAnchorView(binding.btnGuardar)
                        .show()
                }
            }
        }
    }

    /** Valida los campos y devuelve la denuncia lista para enviar, o null si hay errores. */
    private fun validarFormulario(): Denuncia? = with(binding) {
        val dni = etDni.text?.toString()?.trim().orEmpty()
        val tipo = actvTipo.text?.toString()?.trim().orEmpty()
        val direccion = etDireccion.text?.toString()?.trim().orEmpty()
        val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()

        tilDni.error = if (dni.esDniValido()) null else getString(R.string.err_dni)
        tilTipo.error = if (tipo.isNotEmpty()) null else getString(R.string.err_tipo)
        tilDireccion.error = if (direccion.isNotEmpty()) null else getString(R.string.err_direccion)

        if (tilDni.error != null || tilTipo.error != null || tilDireccion.error != null) {
            return null
        }

        Denuncia(
            dni = dni,
            tipo = tipo,
            direccion = direccion,
            descripcion = descripcion.ifBlank { null }
        )
    }

    private fun setCargando(cargando: Boolean) = with(binding) {
        progress.visibility = if (cargando) android.view.View.VISIBLE else android.view.View.GONE
        btnGuardar.isEnabled = !cargando
        btnGuardar.setText(if (cargando) R.string.btn_registrando else R.string.btn_registrar)
        listOf(tilDni, tilTipo, tilDireccion, tilDescripcion).forEach { it.isEnabled = !cargando }
    }

    companion object {
        private val TIPOS_SUGERIDOS = arrayOf(
            "Ruido excesivo",
            "Robo",
            "Acumulación de basura",
            "Vandalismo",
            "Tránsito",
            "Alumbrado público",
            "Maltrato animal",
            "Otro"
        )
    }
}
