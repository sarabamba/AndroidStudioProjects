package com.sara.chat_kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    private lateinit var etPin: EditText
    private lateinit var btnAceptar: Button
    private lateinit var btnCancelar: Button
    private var uidDestino: String? = null  // UID del usuario al que se quiere acceder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        etPin = findViewById(R.id.etPin)
        btnAceptar = findViewById(R.id.btnAceptar)
        btnCancelar = findViewById(R.id.btnCancelar)

        uidDestino = intent.getStringExtra("uid")

        val prefs = getSharedPreferences("PIN_PREFS", Context.MODE_PRIVATE)
        val pinGuardado = prefs.getString("PIN", null)

        // Si no hay PIN, pedir que lo configure
        if (pinGuardado == null) {
            Toast.makeText(this, "Establece tu PIN parental", Toast.LENGTH_SHORT).show()
        }

        btnAceptar.setOnClickListener {
            val pinIngresado = etPin.text.toString()
            if (pinGuardado == null) {
                if (pinIngresado.length < 4) {
                    Toast.makeText(this, "El PIN debe tener al menos 4 dígitos", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("PIN", pinIngresado).apply()
                    abrirChat()
                }
            } else {
                if (pinIngresado == pinGuardado) {
                    abrirChat()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun abrirChat() {
        val intent = Intent(this, com.sara.chat_kotlin.chat.ChatActivity::class.java)
        intent.putExtra("uid", uidDestino)
        startActivity(intent)
        finish()
    }
}
