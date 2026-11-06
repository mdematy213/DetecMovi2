package com.example.detecmovi

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class AccionesActivity : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var tvMensaje: TextView
    private lateinit var llListaAcciones: LinearLayout
    private lateinit var btnVolver: Button

    private var usu: Usuario? = null

    private var consulta: Query? = null
    private var listener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acciones)

        tvBienvenido    = findViewById(R.id.tvBienvenido)
        tvMensaje       = findViewById(R.id.tvMensaje)
        llListaAcciones = findViewById(R.id.llListaAcciones)
        btnVolver       = findViewById(R.id.btnVolver)

        usu = Seguridad.usuarioLogueado(this) ?: return
        tvBienvenido.text = "${usu?.nombre} ${usu?.apellido}".trim()

        cargarAcciones()
        botonVolver()
        desactivarFlecha()

    } // Cierra el "onCreate".



    // Lee (en tiempo real) las últimas 50 acciones guardadas en Firebase.
    private fun cargarAcciones() {
        consulta = Seguridad.db().getReference(Iot.NODO_ACCIONES)
            .orderByChild("timestamp")
            .limitToLast(50)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llListaAcciones.removeAllViews()

                // Firebase entrega de la más antigua a la más nueva; se muestran las más nuevas primero.
                val lista = snapshot.children.reversed()

                if (lista.isEmpty()) {
                    tvMensaje.text = "Aún no hay acciones registradas.\nUse \"Manipular Circuito\" para generar datos."
                    return
                }
                tvMensaje.text = "Últimas ${lista.size} acciones"

                val inflater = LayoutInflater.from(this@AccionesActivity)
                for (x in lista) {
                    val item = inflater.inflate(R.layout.item_accion, llListaAcciones, false)
                    item.findViewById<TextView>(R.id.tvItemFecha).text     = "Fecha     : ${x.child("fecha").value ?: "-"}"
                    item.findViewById<TextView>(R.id.tvItemHora).text      = "Hora      : ${x.child("hora").value ?: "-"}"
                    item.findViewById<TextView>(R.id.tvItemUsuario).text   = "Usuario   : ${x.child("usuario").value ?: "-"}"
                    item.findViewById<TextView>(R.id.tvItemAccion).text    = "Acción    : ${x.child("accion").value ?: "-"}"
                    item.findViewById<TextView>(R.id.tvItemDistancia).text = "Distancia : ${x.child("distancia").value ?: 0} cm"
                    llListaAcciones.addView(item)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvMensaje.text = "Error al leer Firebase: ${error.message}"
            }
        }
        consulta!!.addValueEventListener(listener!!)
    }



    fun botonVolver() {
        btnVolver.setOnClickListener { finish() }
    } // Cierra botonVolver.



    private fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) {
            // Sin acción.
        }
    } // Cierra desactivarFlecha.

    override fun onDestroy() {
        listener?.let { consulta?.removeEventListener(it) }
        super.onDestroy()
    }

} // Cierra la clase "AccionesActivity".
