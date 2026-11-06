package com.example.detecmovi

import java.io.Serializable

// Datos del usuario logueado que viajan entre pantallas.
// Por seguridad (ISO 27400) la contraseña NO se guarda en este objeto.
data class Usuario(
    var id: String = "",          // Clave del registro en Firebase
    var rut: String = "",
    var nombre: String = "",
    var apellido: String = "",
    var correo: String = "",
    var cargo: String = ""
) : Serializable
