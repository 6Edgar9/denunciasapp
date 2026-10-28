package com.regalado.denunciasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.regalado.denunciasapp.api.ApiResult
import com.regalado.denunciasapp.api.Denuncia
import com.regalado.denunciasapp.api.PageResponse
import com.regalado.denunciasapp.api.RetrofitClient
import com.regalado.denunciasapp.api.safeApiCall
import com.regalado.denunciasapp.databinding.ActivityMainBinding
import com.regalado.denunciasapp.util.applySystemBarsPadding
import com.regalado.denunciasapp.util.esDniValido
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Pantalla principal: listado paginado ("Cargar 5 siguientes") + búsqueda por DNI.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val api = RetrofitClient.denunciaApi
    private val adapter = DenunciaAdapter { abrirDetalle(it) }

    // Estado de paginación
    private val items = mutableListOf<Denuncia>()
    private var paginaActual = 0
    private var esUltimaPagina = false
    private var cargando = false
    private var jobActual: Job? = null

    // Si no es null, se está mostrando el resultado de una búsqueda por DNI
    private var dniBuscado: String? = null

    private val registrarLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) recargar()
        }

    private val detalleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                recargar()
            } else if (result.resultCode == DetalleActivity.RESULT_BUSCAR_DNI) {
                val dni = result.data?.getStringExtra(DetalleActivity.EXTRA_BUSCAR_DNI)
                if (!dni.isNullOrBlank()) {
                    binding.etDni.setText(dni)
                    ejecutarBusqueda(dni)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        setSupportActionBar(binding.toolbar)

        binding.rvDenuncias.layoutManager = LinearLayoutManager(this)
        binding.rvDenuncias.adapter = adapter

        binding.btnCargarMas.setOnClickListener { cargarPagina() }
        binding.fabNueva.setOnClickListener {
            registrarLauncher.launch(Intent(this, RegistroActivity::class.java))
        }
        binding.btnBuscar.setOnClickListener { buscarPorDni() }
        binding.etDni.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                buscarPorDni(); true
            } else false
        }
        binding.btnLimpiar.setOnClickListener { limpiarBusqueda() }

        cargarPagina()
    }

    // ---------------------------------------------------------------- Listado

    /** GET /denuncias/?offset=&limit= (limit = 5) */
    private fun cargarPagina() {
        if (cargando || esUltimaPagina) return
        setCargando(true)

        jobActual = lifecycleScope.launch {
            when (val result = safeApiCall { api.listar(paginaActual, PAGE_SIZE) }) {
                is ApiResult.Success -> {
                    val page = result.data
                    items.addAll(page.content.orEmpty())
                    adapter.submitList(items.toList())
                    paginaActual++
                    esUltimaPagina = page.esUltima(PAGE_SIZE)
                }
                is ApiResult.Error -> mostrarError(result.message) { cargarPagina() }
            }
            setCargando(false)
        }
    }

    private fun reiniciarListado() {
        jobActual?.cancel()
        cargando = false
        items.clear()
        adapter.submitList(emptyList())
        paginaActual = 0
        esUltimaPagina = false
    }

    private fun recargar() {
        val dni = dniBuscado
        if (dni != null) {
            ejecutarBusqueda(dni)
        } else {
            reiniciarListado()
            cargarPagina()
        }
    }

    // --------------------------------------------------------------- Búsqueda

    private fun buscarPorDni() {
        val dni = binding.etDni.text?.toString()?.trim().orEmpty()
        if (!dni.esDniValido()) {
            binding.tilDni.error = getString(R.string.err_dni)
            return
        }
        binding.tilDni.error = null
        ocultarTeclado()
        ejecutarBusqueda(dni)
    }

    /** GET /denuncias/ciudadano/{dni} */
    private fun ejecutarBusqueda(dni: String) {
        reiniciarListado()
        dniBuscado = dni
        setCargando(true)

        jobActual = lifecycleScope.launch {
            when (val result = safeApiCall { api.buscarPorDni(dni) }) {
                is ApiResult.Success -> {
                    items.addAll(result.data)
                    adapter.submitList(items.toList())
                }
                is ApiResult.Error -> mostrarError(result.message) { ejecutarBusqueda(dni) }
            }
            setCargando(false)
        }
    }

    private fun limpiarBusqueda() {
        dniBuscado = null
        binding.etDni.text = null
        binding.tilDni.error = null
        ocultarTeclado()
        reiniciarListado()
        cargarPagina()
    }

    // --------------------------------------------------------------------- UI

    private fun setCargando(valor: Boolean) {
        cargando = valor
        actualizarUi()
    }

    private fun actualizarUi() = with(binding) {
        val enBusqueda = dniBuscado != null
        val vacio = items.isEmpty()

        progress.visibility = if (cargando && vacio) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (!cargando && vacio) View.VISIBLE else View.GONE
        tvEmpty.setText(if (enBusqueda) R.string.empty_busqueda else R.string.empty_listado)

        btnLimpiar.visibility = if (enBusqueda) View.VISIBLE else View.GONE
        tvInfo.text = if (enBusqueda) {
            getString(R.string.info_resultados_dni, items.size, dniBuscado)
        } else {
            getString(R.string.info_recientes, items.size)
        }

        // Botón de paginación solo en modo listado
        btnCargarMas.visibility = if (enBusqueda) View.INVISIBLE else View.VISIBLE
        btnCargarMas.isEnabled = !cargando && !esUltimaPagina
        btnCargarMas.setText(
            when {
                cargando -> R.string.btn_cargando
                esUltimaPagina -> R.string.btn_no_hay_mas
                else -> R.string.btn_cargar_mas
            }
        )
    }

    private fun mostrarError(mensaje: String, reintentar: () -> Unit) {
        Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
            .setAction(R.string.btn_reintentar) { reintentar() }
            .setAnchorView(binding.fabNueva)
            .show()
    }

    private fun abrirDetalle(denuncia: Denuncia) {
        val id = denuncia.id ?: return
        detalleLauncher.launch(
            Intent(this, DetalleActivity::class.java)
                .putExtra(DetalleActivity.EXTRA_ID, id)
        )
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etDni.windowToken, 0)
        binding.etDni.clearFocus()
    }

    companion object {
        private const val PAGE_SIZE = 5 // "Cargar 5 siguientes"
    }
}

/** Determina si es la última página, soportando ambos formatos de Page de Spring. */
private fun PageResponse<*>.esUltima(pageSize: Int): Boolean {
    last?.let { return it }
    page?.let { return it.number + 1 >= it.totalPages }
    if (totalPages != null && number != null) return number + 1 >= totalPages
    return content.orEmpty().size < pageSize
}