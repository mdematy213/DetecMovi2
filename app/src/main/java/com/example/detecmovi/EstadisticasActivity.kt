package com.example.detecmovi

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var tvActivaciones: TextView
    private lateinit var tvDesactivaciones: TextView
    private lateinit var tvObstaculos: TextView
    private lateinit var tvDistanciaTotal: TextView
    private lateinit var btnVolver: Button

    private var usu: Usuario? = null

    private var ref: DatabaseReference? = null
    private var listener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estadisticas)

        tvBienvenido      = findViewById(R.id.tvBienvenido)
        tvActivaciones    = findViewById(R.id.tvActivaciones)
        tvDesactivaciones = findViewById(R.id.tvDesactivaciones)
        tvObstaculos      = findViewById(R.id.tvObstaculos)
        tvDistanciaTotal  = findViewById(R.id.tvDistanciaTotal)
        btnVolver         = findViewById(R.id.btnVolver)

        usu = Seguridad.usuarioLogueado(this) ?: return
        tvBienvenido.text = "${usu?.nombre} ${usu?.apellido}".trim()

        tvActivaciones.text    = "…"
        tvDesactivaciones.text = "…"
        tvObstaculos.text      = "…"
        tvDistanciaTotal.text  = "…"

        calcularEstadisticas()
        botonVolver()
        desactivarFlecha()

    } // Cierra el "onCreate".



    // Calcula las cantidades a partir de la colección "Acciones" (se actualiza en tiempo real).
    private fun calcularEstadisticas() {
        ref = Seguridad.db().getReference(Iot.NODO_ACCIONES)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activaciones = 0
                var desactivaciones = 0
                var obstaculos = 0
                var distanciaTotal = 0L

                for (x in snapshot.children) {
                    val accion = x.child("accion").value?.toString() ?: ""
                    val dist = x.child("distancia").value?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                    when (accion) {
                        Iot.ACTIVADO    -> activaciones++
                        Iot.DESACTIVADO -> desactivaciones++
                        Iot.OBSTACULO, Iot.CERCA -> {
                            obstaculos++
                            distanciaTotal += dist
                        }
                    }
                }

                tvActivaciones.text    = activaciones.toString()
                tvDesactivaciones.text = desactivaciones.toString()
                tvObstaculos.text      = obstaculos.toString()
                tvDistanciaTotal.text  = "$distanciaTotal cm"
            }

            override fun onCancelled(error: DatabaseError) {
                tvActivaciones.text = "Error"
            }
        }
        ref!!.addValueEventListener(listener!!)
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
        listener?.let { ref?.removeEventListener(it) }
        super.onDestroy()
    }

} // Cierra la clase "EstadisticasActivity".
