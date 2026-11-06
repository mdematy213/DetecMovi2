package com.example.detecmovi

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Iot {

    const val NODO_CIRCUITO = "Circuito"
    const val NODO_ACCIONES = "Acciones"

    const val ACTIVADO     = "Circuito Activado"
    const val DESACTIVADO  = "Circuito Desactivado"
    const val OBSTACULO    = "Obstáculo Detectado"
    const val CERCA        = "Cerca de Impactar"

    // Distancia (cm) bajo la cual se considera "Cerca de Impactar".
    const val DISTANCIA_PELIGRO = 15

    fun fecha(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    fun hora(): String  = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())


    fun registrarAccion(usu: Usuario?, accion: String, distancia: Int?) {
        val datos = HashMap<String, Any>()
        datos["fecha"]     = fecha()
        datos["hora"]      = hora()
        datos["usuario"]   = usu?.rut ?: ""
        datos["nombre"]    = usu?.nombre ?: ""
        datos["accion"]    = accion
        datos["distancia"] = distancia ?: 0
        // Año-mes-día hora: se lee fácil y además ordena las acciones cronológicamente.
        datos["timestamp"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        // Clave legible en Firebase: "Circuito Activado 05-11-2026 23:10:05"
        val clave = "$accion ${datos["fecha"]} ${datos["hora"]}"
        Seguridad.db().getReference(NODO_ACCIONES).child(clave).setValue(datos)
    }
}
