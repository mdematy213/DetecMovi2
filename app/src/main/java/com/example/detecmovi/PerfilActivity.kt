package com.example.detecmovi

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class PerfilActivity : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var tvRut: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvApellido: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvCargo: TextView
    private lateinit var tvContrasena: TextView
    private lateinit var btnVolver: Button

    private var usu: Usuario? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        tvBienvenido = findViewById(R.id.tvBienvenido)
        tvRut        = findViewById(R.id.tvRut)
        tvNombre     = findViewById(R.id.tvNombre)
        tvApellido   = findViewById(R.id.tvApellido)
        tvCorreo     = findViewById(R.id.tvCorreo)
        tvCargo      = findViewById(R.id.tvCargo)
        tvContrasena = findViewById(R.id.tvContrasena)
        btnVolver    = findViewById(R.id.btnVolver)

        usu = Seguridad.usuarioLogueado(this) ?: return

        tvBienvenido.text = "${usu?.nombre} ${usu?.apellido}".trim()
        mostrar(usu!!)
        // La contraseña nunca se muestra: se guarda cifrada (SHA-256) en Firebase.
        tvContrasena.text = "•••••••• (cifrada)"

        refrescarDesdeFirebase()

        botonVolver()
        desactivarFlecha()

    } // Cierra el "onCreate".



    private fun mostrar(u: Usuario) {
        tvRut.text      = u.rut.ifEmpty { "-" }
        tvNombre.text   = u.nombre.ifEmpty { "-" }
        tvApellido.text = u.apellido.ifEmpty { "-" }
        tvCorreo.text   = u.correo.ifEmpty { "-" }
        tvCargo.text    = u.cargo.ifEmpty { "-" }
    }



    // Vuelve a leer los datos del usuario desde Firebase por si cambiaron.
    private fun refrescarDesdeFirebase() {
        val id = usu?.id ?: return
        if (id.isEmpty()) return
        Seguridad.db().getReference("Usuario").child(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(x: DataSnapshot) {
                    if (!x.exists()) return
                    val u = Usuario(
                        id       = id,
                        rut      = x.child("rut").value?.toString() ?: "",
                        nombre   = x.child("nombre").value?.toString() ?: "",
                        apellido = x.child("apellido").value?.toString() ?: "",
                        correo   = (x.child("correo").value ?: x.child("email").value)?.toString() ?: "",
                        cargo    = (x.child("cargo").value ?: x.child("tipo").value)?.toString() ?: ""
                    )
                    mostrar(u)
                }
                override fun onCancelled(error: DatabaseError) { }
            })
    }



    fun botonVolver() {
        btnVolver.setOnClickListener { finish() }
    } // Cierra botonVolver.



    private fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) {
            // Sin acción.
        }
    } // Cierra desactivarFlecha.

} // Cierra la clase "PerfilActivity".
