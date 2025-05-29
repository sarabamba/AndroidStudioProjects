package com.sara.chat_kotlin

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class OlvidePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_olvide_password)

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inicializa las vistas
        etEmail = findViewById(R.id.et_olvide_password_email)
        btnResetPassword = findViewById(R.id.btn_olvide_password_reset)
        progressBar = findViewById(R.id.pb_olvide_password_loading)

        // Configura el listener del botón
        btnResetPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo electrónico"
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        btnResetPassword.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("OlvidePasswordActivity", "Correo de restablecimiento enviado.")
                    Toast.makeText(this, "Se ha enviado un correo electrónico para restablecer tu contraseña.", Toast.LENGTH_LONG).show()
                    finish() // Cierra la actividad y regresa a la pantalla de inicio de sesión
                } else {
                    Log.e("OlvidePasswordActivity", "Error al enviar el correo de restablecimiento: ${task.exception?.message}")
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
                progressBar.visibility = android.view.View.GONE
                btnResetPassword.isEnabled = true
            }
    }
}
