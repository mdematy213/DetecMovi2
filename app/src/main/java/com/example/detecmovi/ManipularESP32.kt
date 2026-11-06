package com.example.detecmovi

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class ManipularESP32 : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var btnActivar: Button
    private lateinit var btnDesactivar: Button
    private lateinit var btnSimular: Button
    private lateinit var btnVolver: Button

    private var usu: Usuario? = null

    private lateinit var refCircuito: DatabaseReference
    private var listener: ValueEventListener? = null

    private var activo = false
    private var ultimaDistancia: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manipular_esp32)

        tvBienvenido  = findViewById(R.id.tvBienvenido)
        tvEstado      = findViewById(R.id.tvEstado)
        tvDistancia   = findViewById(R.id.tvDistancia)
        btnActivar    = findViewById(R.id.btnActivar)
        btnDesactivar = findViewById(R.id.btnDesactivar)
        btnSimular    = findViewById(R.id.btnSimular)
        btnVolver     = findViewById(R.id.btnVolver)

        usu = Seguridad.usuarioLogueado(this) ?: return
        tvBienvenido.text = "${usu?.nombre} ${usu?.apellido}".trim()

        refCircuito = Seguridad.db().getReference(Iot.NODO_CIRCUITO)

        escucharCircuito()
        botonActivar()
        botonDesactivar()
        botonSimular()
        botonVolver()
        desactivarFlecha()

    } // Cierra el "onCreate".



    // Escucha en tiempo real el nodo "Circuito" (estado y distancia enviados por el ESP32).
    private fun escucharCircuito() {
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activo = snapshot.child("estado").value as? Boolean ?: false
                ultimaDistancia = snapshot.child("distancia").value?.toString()?.toDoubleOrNull()?.toInt()

                if (activo) {
                    tvEstado.text = "Estado: ACTIVADO"
                    tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    tvEstado.text = "Estado: DESACTIVADO"
                    tvEstado.setTextColor(Color.parseColor("#E57373"))
                }
                tvDistancia.text = "Distancia: ${ultimaDistancia?.toString() ?: "--"} cm"

                btnActivar.isEnabled    = !activo
                btnDesactivar.isEnabled = activo
                btnSimular.isEnabled    = activo
            }

            override fun onCancelled(error: DatabaseError) {
                tvEstado.text = "Estado: sin conexión"
                Toast.makeText(this@ManipularESP32, "Error Firebase: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        refCircuito.addValueEventListener(listener!!)
    }



    private fun cambiarEstado(nuevo: Boolean) {
        if (!Seguridad.tieneInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet.", Toast.LENGTH_LONG).show()
            return
        }
        val cambios = HashMap<String, Any>()
        cambios["estado"]         = nuevo
        cambios["actualizadoPor"] = usu?.rut ?: ""
        cambios["fechaHora"]      = "${Iot.fecha()} ${Iot.hora()}"

        refCircuito.updateChildren(cambios)
            .addOnSuccessListener {
                Iot.registrarAccion(usu, if (nuevo) Iot.ACTIVADO else Iot.DESACTIVADO, ultimaDistancia)
                Toast.makeText(this,
                    if (nuevo) "Circuito activado" else "Circuito desactivado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun botonActivar()    { btnActivar.setOnClickListener    { cambiarEstado(true) } }
    fun botonDesactivar() { btnDesactivar.setOnClickListener { cambiarEstado(false) } }



    // Simula lo que haría el ESP32 al detectar un obstáculo (para pruebas sin hardware).
    fun botonSimular() {
        btnSimular.setOnClickListener {
            if (!activo) {
                Toast.makeText(this, "Primero active el circuito.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dist = Random.nextInt(3, 120)
            refCircuito.child("distancia").setValue(dist)
            val accion = if (dist <= Iot.DISTANCIA_PELIGRO) Iot.CERCA else Iot.OBSTACULO
            Iot.registrarAccion(usu, accion, dist)
            Toast.makeText(this, "$accion a $dist cm", Toast.LENGTH_SHORT).show()
        }
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
        if (::refCircuito.isInitialized) listener?.let { refCircuito.removeEventListener(it) }
        super.onDestroy()
    }

} // Cierra la clase "ManipularESP32".
