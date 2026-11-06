package com.example.detecmovi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Patterns
import androidx.core.content.IntentCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest


object Seguridad {


    const val URL_FIREBASE = "https://detecmov2-default-rtdb.firebaseio.com"

    fun db(): FirebaseDatabase = FirebaseDatabase.getInstance(URL_FIREBASE)


    // ---------- Cifrado ----------


    fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }


    fun esHash(valor: String): Boolean = valor.length == 64 && valor.all { it in "0123456789abcdef" }



    fun contrasenaCorrecta(x: DataSnapshot, ingresada: String): Boolean {
        val guardadas = listOf(
            x.child("contrasena").value?.toString() ?: "",
            x.child("contraseña").value?.toString() ?: ""
        ).filter { it.isNotEmpty() }

        for (g in guardadas) {
            if (esHash(g) && g == sha256(ingresada)) return true
            if (!esHash(g) && g == ingresada) return true
        }
        return false
    }


    // ---------- RUT ----------


    fun normalizarRut(rut: String): String =
        rut.replace(".", "").replace("-", "").replace(" ", "").uppercase()


    fun rutValido(rut: String): Boolean {
        val limpio = normalizarRut(rut)
        if (limpio.length < 2) return false
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        if (!cuerpo.all { it.isDigit() }) return false

        var suma = 0
        var multiplicador = 2
        for (c in cuerpo.reversed()) {
            suma += (c - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        val resto = 11 - (suma % 11)
        val dvEsperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> ('0' + resto)
        }
        return dv == dvEsperado
    }


    fun formatearRut(rut: String): String {
        val limpio = normalizarRut(rut)
        if (limpio.length < 2) return rut
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-$dv"
    }

    fun correoValido(correo: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(correo).matches()


    // ---------- Firebase ---------

    fun recolectarUsuarios(snapshot: DataSnapshot): List<DataSnapshot> {
        val lista = mutableListOf<DataSnapshot>()
        for (x in snapshot.children) {
            if (x.hasChild("contrasena") || x.hasChild("contraseña") || x.hasChild("rut") || x.hasChild("nombre")) {
                lista.add(x)
            } else {
                for (y in x.children) {
                    if (y.hasChild("contrasena") || y.hasChild("contraseña")) lista.add(y)
                }
            }
        }
        return lista
    }


    // ---------- Conectividad ----------

    fun tieneInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    // ---------- Control de acceso ----------

    fun usuarioLogueado(activity: Activity): Usuario? {
        val usu = IntentCompat.getSerializableExtra(activity.intent, "usu", Usuario::class.java)
        if (usu == null) {
            val intent = Intent(activity, Principal::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }
        return usu
    }
}
