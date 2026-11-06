package com.example.detecmovi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class Principal : AppCompatActivity() {

    private lateinit var etRut: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnIrRegistro: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        etRut         = findViewById(R.id.etRut)
        etPassword    = findViewById(R.id.etPassword)
        btnIngresar   = findViewById(R.id.btnIngresar)
        btnIrRegistro = findViewById(R.id.btnIrRegistro)

        botonIngresar()
        botonCrearCuenta()
        desactivarFlecha()

    } // Cierra el "onCreate".



    fun botonIngresar() {
        btnIngresar.setOnClickListener {

            val usuIngresado  = etRut.text.toString().trim()
            val passIngresada = etPassword.text.toString().trim()

            if (usuIngresado.isEmpty() || passIngresada.isEmpty()) {
                Toast.makeText(this@Principal, "¡ Complete Los Campos Faltantes !", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!Seguridad.tieneInternet(this)) {
                Toast.makeText(this@Principal, "Sin conexión a internet. Revise su WiFi o datos móviles.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            ocultarTeclado()
            btnIngresar.isEnabled = false
            Toast.makeText(this@Principal, "Verificando…", Toast.LENGTH_SHORT).show()

            // Conexión a la colección "Usuario" de Realtime Database.
            val dbref = Seguridad.db().getReference("Usuario")

            // Si Firebase no responde en 15 segundos se avisa al usuario.
            var respondio = false
            val timeout = Runnable {
                if (!respondio) {
                    btnIngresar.isEnabled = true
                    Toast.makeText(this@Principal,
                        "Firebase no responde. Revise la conexión y las reglas de la base de datos.",
                        Toast.LENGTH_LONG).show()
                }
            }
            handler.postDelayed(timeout, 15000)

            dbref.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    respondio = true
                    handler.removeCallbacks(timeout)
                    btnIngresar.isEnabled = true

                    val usuarios = Seguridad.recolectarUsuarios(snapshot)
                    Log.d("LOGIN", "Usuarios encontrados en BD: ${usuarios.size}")

                    if (usuarios.isEmpty()) {
                        Toast.makeText(this@Principal,
                            "La colección \"Usuario\" está vacía en Firebase.", Toast.LENGTH_LONG).show()
                        return
                    }

                    var estado = false

                    for (x in usuarios) {
                        val rutDb = x.child("rut").value?.toString() ?: ""
                        val nomDb = x.child("nombre").value?.toString() ?: ""

                        // Se puede ingresar con el RUT (con o sin puntos/guion) o con el nombre de usuario.
                        val coincideUsuario =
                            (rutDb.isNotEmpty() && Seguridad.normalizarRut(rutDb) == Seguridad.normalizarRut(usuIngresado)) ||
                            nomDb.equals(usuIngresado, ignoreCase = true)

                        if (coincideUsuario && Seguridad.contrasenaCorrecta(x, passIngresada)) {
                            estado = true

                            // Si la contraseña estaba en texto plano, se cifra (SHA-256) en la BD.
                            migrarContrasena(x, passIngresada)

                            val usu = Usuario(
                                id       = x.key ?: "",
                                rut      = rutDb,
                                nombre   = nomDb,
                                apellido = x.child("apellido").value?.toString() ?: "",
                                correo   = (x.child("correo").value ?: x.child("email").value)?.toString() ?: "",
                                cargo    = (x.child("cargo").value ?: x.child("tipo").value)?.toString() ?: ""
                            )

                            Toast.makeText(this@Principal, "¡ Bienvenido(a) $nomDb !", Toast.LENGTH_SHORT).show()
                            etPassword.setText("")
                            val intent = Intent(this@Principal, MenuActivity::class.java)
                            intent.putExtra("usu", usu)
                            startActivity(intent)
                            break
                        }
                    }

                    if (!estado) {
                        etPassword.setText("")
                        Toast.makeText(this@Principal, "¡ RUT o Contraseña incorrectos !", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    respondio = true
                    handler.removeCallbacks(timeout)
                    btnIngresar.isEnabled = true
                    Log.e("LOGIN", "Error: ${error.message} código: ${error.code}")
                    val msj = if (error.code == DatabaseError.PERMISSION_DENIED)
                        "Permiso denegado: revise las Reglas de Realtime Database (.read / .write)."
                    else "Error Firebase: ${error.message}"
                    Toast.makeText(this@Principal, msj, Toast.LENGTH_LONG).show()
                }
            })

        } // Cierra onClick.
    } // Cierra botonIngresar.



    // Reemplaza una contraseña antigua en texto plano por su hash SHA-256.
    private fun migrarContrasena(x: DataSnapshot, pass: String) {
        val actual = x.child("contrasena").value?.toString() ?: ""
        if (!Seguridad.esHash(actual) || x.hasChild("contraseña")) {
            val cambios = HashMap<String, Any?>()
            cambios["contrasena"] = Seguridad.sha256(pass)
            cambios["contraseña"] = null   // elimina la versión en texto plano
            x.ref.updateChildren(cambios)
        }
    }



    fun botonCrearCuenta() {
        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this@Principal, RegistroActivity::class.java))
        }
    } // Cierra botonCrearCuenta.



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

} // Cierra la clase "Principal".
