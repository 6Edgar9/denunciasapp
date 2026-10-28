package com.regalado.denunciasapp.util

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.regalado.denunciasapp.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Convierte el LocalDateTime ISO del backend a "dd/MM/yyyy HH:mm". */
fun formatFecha(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        LocalDateTime.parse(raw).format(FORMATO_FECHA)
    } catch (e: Exception) {
        raw
    }
}

/** Aplica el estilo de chip según el estado (1 = pendiente, 0 = atendida). */
fun TextView.bindEstado(estado: Int?) {
    val atendida = estado == 0
    setText(if (atendida) R.string.estado_atendida else R.string.estado_pendiente)
    setBackgroundResource(if (atendida) R.drawable.bg_chip_done else R.drawable.bg_chip_pending)
    setTextColor(
        ContextCompat.getColor(context, if (atendida) R.color.status_done else R.color.status_pending)
    )
}

/** Agrega padding para barras del sistema y teclado (edge-to-edge en Android 15+). */
fun View.applySystemBarsPadding() {
    val start = paddingLeft
    val top = paddingTop
    val end = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        v.setPadding(start + bars.left, top + bars.top, end + bars.right, bottom + bars.bottom)
        insets
    }
}

fun String.esDniValido(): Boolean = matches(Regex("^\\d{8}$"))
