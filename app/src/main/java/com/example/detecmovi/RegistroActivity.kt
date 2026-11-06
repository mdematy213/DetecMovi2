package com.example.detecmovi

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class RegistroActivity : AppCompatActivity() {

    private lateinit var etRutReg: EditText
    private lateinit var etNombreReg: EditText
    private lateinit var etApellidoReg: EditText
    private lateinit var etCorreoReg: EditText
    private lateinit var spCargoReg: Spinner
    private lateinit var etContrasenaReg: EditText
    private lateinit var etRepetirContrasenaReg: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var btnVolver: Button

    private val handler = Handler(Looper.getMainLooper())

    private val cargos = arrayOf("Seleccione Cargo", "Administrador", "Dueño", "Supervisor", "Operador")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        etRutReg               = findViewById(R.id.etRutReg)
        etNombreReg            = findViewById(R.id.etNombreReg)
        etApellidoReg          = findViewById(R.id.etApellidoReg)
        etCorreoReg            = findViewById(R.id.etCorreoReg)
        spCargoReg             = findViewById(R.id.spCargoReg)
        etContrasenaReg        = findViewById(R.id.etContrasenaReg)
        etRepetirContrasenaReg = findViewById(R.id.etRepetirContrasenaReg)
        btnRegistrar           = findViewById(R.id.btnRegistrar)
        btnVolver              = findViewById(R.id.btnVolver)

        cargarCargos()
        botonRegistrar()
        botonVolver()
        desactivarFlecha()

    } // Cierra el "onCreate".



    // Llena la lista desplegable de cargos (texto claro sobre fondo oscuro).
    private fun cargarCargos() {
        val adaptador = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cargos) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(if (position == 0) Color.parseColor("#555555") else Color.WHITE)
                v.setPadding(24, 0, 24, 0)
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(Color.WHITE)
                v.setBackgroundColor(Color.parseColor("#2A2A2A"))
                return v
            }
        }
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCargoReg.adapter = adaptador
    }



    private fun botonRegistrar() {
        btnRegistrar.setOnClickListener {

            val rut  = etRutReg.text.toString().trim()
            val nom  = etNombreReg.text.toString().trim()
            val ape  = etApellidoReg.text.toString().trim()
            val ema  = etCorreoReg.text.toString().trim()
            val car  = if (spCargoReg.selectedItemPosition > 0) spCargoReg.selectedItem.toString() else ""
            val pass = etContrasenaReg.text.toString().trim()
            val rep  = etRepetirContrasenaReg.text.toString().trim()

            // ----- Validaciones -----
            if (rut.isEmpty() || nom.isEmpty() || ape.isEmpty() ||
                ema.isEmpty() || car.isEmpty() || pass.isEmpty() || rep.isEmpty()) {
                Toast.makeText(this, "¡ Complete todos los campos faltantes !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Seguridad.rutValido(rut)) {
                etRutReg.error = "RUT inválido"
                Toast.makeText(this, "¡ El RUT ingresado no es válido !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Seguridad.correoValido(ema)) {
                etCorreoReg.error = "Correo inválido"
                Toast.makeText(this, "¡ El correo no es válido !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                etContrasenaReg.error = "Mínimo 6 caracteres"
                Toast.makeText(this, "¡ La contraseña debe tener al menos 6 caracteres !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != rep) {
                Toast.makeText(this, "¡ Las contraseñas no coinciden !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Seguridad.tieneInternet(this)) {
                Toast.makeText(this, "Sin conexión a internet. Revise su WiFi o datos móviles.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            ocultarTeclado()
            btnRegistrar.isEnabled = false
            Toast.makeText(this, "Registrando…", Toast.LENGTH_SHORT).show()

            val dbref = Seguridad.db().getReference("Usuario")

            var respondio = false
            handler.postDelayed({
                if (!respondio) {
                    btnRegistrar.isEnabled = true
                    Toast.makeText(this@RegistroActivity,
                        "Firebase no responde. Verifique su conexión.", Toast.LENGTH_LONG).show()
                }
            }, 15000)

            // 1) Revisar que el RUT no esté registrado.
            dbref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (x in Seguridad.recolectarUsuarios(snapshot)) {
                        val rutDb = x.child("rut").value?.toString() ?: ""
                        if (Seguridad.normalizarRut(rutDb) == Seguridad.normalizarRut(rut)) {
                            respondio = true
                            handler.removeCallbacksAndMessages(null)
                            btnRegistrar.isEnabled = true
                            Toast.makeText(this@RegistroActivity,
                                "¡ Ya existe una cuenta con ese RUT !", Toast.LENGTH_LONG).show()
                            return
                        }
                    }

                    // 2) Guardar el nuevo usuario (contraseña cifrada con SHA-256).
                    val nuevoUsuario = HashMap<String, Any>()
                    nuevoUsuario["rut"]        = Seguridad.formatearRut(rut)
                    nuevoUsuario["nombre"]     = nom
                    nuevoUsuario["apellido"]   = ape
                    nuevoUsuario["correo"]     = ema
                    nuevoUsuario["cargo"]      = car
                    nuevoUsuario["contrasena"] = Seguridad.sha256(pass)

                    val key = dbref.push().key ?: Seguridad.normalizarRut(rut)

                    dbref.child(key).setValue(nuevoUsuario)
                        .addOnSuccessListener {
                            respondio = true
                            handler.removeCallbacksAndMessages(null)
                            Log.d("REGISTRO", "Usuario guardado con key=$key")
                            Toast.makeText(this@RegistroActivity,
                                "¡ Usuario $nom registrado ! Ya puede iniciar sesión.", Toast.LENGTH_LONG).show()
                            finish()   // vuelve al Login
                        }
                        .addOnFailureListener { error ->
                            respondio = true
                            handler.removeCallbacksAndMessages(null)
                            btnRegistrar.isEnabled = true
                            Log.e("REGISTRO", "Error: ${error.message}")
                            Toast.makeText(this@RegistroActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    respondio = true
                    handler.removeCallbacksAndMessages(null)
                    btnRegistrar.isEnabled = true
                    Toast.makeText(this@RegistroActivity, "Error Firebase: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        } // Cierra onClick.
    } // Cierra botonRegistrar.



    private fun botonVolver() {
        btnVolver.setOnClickListener { finish() }
    }

    private fun desactivarFlecha() {
        onBackPressedDispatcher.addCallback(this) { }
    }

    private fun ocultarTeclado() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

} // Cierra la clase "RegistroActivity".
