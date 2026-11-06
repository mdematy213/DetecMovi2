package com.example.detecmovi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var btnManipular: Button
    private lateinit var btnAcciones: Button
    private lateinit var btnEstadisticas: Button
    private lateinit var btnPerfil: Button
    private lateinit var btnCerrarSesion: Button

    private var usu: Usuario? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        tvBienvenido     = findViewById(R.id.tvBienvenido)
        btnManipular     = findViewById(R.id.btnManipular)
        btnAcciones      = findViewById(R.id.btnAcciones)
        btnEstadisticas  = findViewById(R.id.btnEstadisticas)
        btnPerfil        = findViewById(R.id.btnPerfil)
        btnCerrarSesion  = findViewById(R.id.btnCerrarSesion)

        // Recibe el objeto Usuario desde Principal (control de acceso).
        usu = Seguridad.usuarioLogueado(this) ?: return

        tvBienvenido.text = "${usu?.nombre} ${usu?.apellido}".trim()

        botonManipular()
        botonAcciones()
        botonEstadisticas()
        botonPerfil()
        botonCerrarSesion()
        desactivarFlecha()

    } // Cierra el "onCreate".



    private fun abrir(destino: Class<*>) {
        val intent = Intent(this@MenuActivity, destino)
        intent.putExtra("usu", usu)
        startActivity(intent)
    }

    fun botonManipular()    { btnManipular.setOnClickListener    { abrir(ManipularESP32::class.java) } }
    fun botonAcciones()     { btnAcciones.setOnClickListener     { abrir(AccionesActivity::class.java) } }
    fun botonEstadisticas() { btnEstadisticas.setOnClickListener { abrir(EstadisticasActivity::class.java) } }
    fun botonPerfil()       { btnPerfil.setOnClickListener       { abrir(PerfilActivity::class.java) } }



    fun botonCerrarSesion() {
        btnCerrarSesion.setOnClickListener {
            val dialogo = AlertDialog.Builder(this@MenuActivity)
            dialogo.setTitle("Cerrar Sesión")
            dialogo.setMessage("¿Desea cerrar la sesión?")

            dialogo.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            dialogo.setPositiveButton("Aceptar") { _, _ ->
                // Limpia todas las pantallas abiertas para que no se pueda volver sin loguearse.
                val intent = Intent(this@MenuActivity, Principal::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            dialogo.setCancelable(false)
            dialogo.show()
        }
    } // Cierra botonCerrarSesion.



    private fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) {
            // Sin acción — se sale solo con "Cerrar Sesión".
        }
    } // Cierra desactivarFlecha.

} // Cierra la clase "MenuActivity".
