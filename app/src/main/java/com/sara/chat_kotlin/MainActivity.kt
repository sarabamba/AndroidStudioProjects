package com.sara.chat_kotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.sara.chat_kotlin.databinding.ActivityMainBinding
import com.sara.chat_kotlin.fragmentos.FragmentPerfil
import com.sara.chat_kotlin.fragmentos.FragmentUsuarios
import com.sara.chat_kotlin.fragmentos.FragmentChats

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIO: Configurar PIN parental si no existe ---
        val sharedPref = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if (sharedPref.getString("PIN_PARENTAL", null) == null) {
            // Guardar PIN por defecto la primera vez
            sharedPref.edit().putString("PIN_PARENTAL", "1234").apply()  // Cambia "1234" por el PIN que prefieras
        }
        // --- FIN configuración PIN parental ---

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        comprobarSesion()

        // Mostrar fragmento por defecto
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, FragmentPerfil())
            .commit()
        binding.tvTitulo.text = "Perfil"

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_perfil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentoFL.id, FragmentPerfil())
                        .commit()
                    binding.tvTitulo.text = "Perfil"
                    true
                }
                R.id.item_usuarios -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentoFL.id, FragmentUsuarios())
                        .commit()
                    binding.tvTitulo.text = "Usuarios"
                    true
                }
                R.id.item_chats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentoFL.id, FragmentChats())
                        .commit()
                    binding.tvTitulo.text = "Chats"
                    true
                }
                else -> false
            }
        }
    }

    private fun comprobarSesion() {
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(applicationContext, OpcionesLoginActivity::class.java))
            finishAffinity()
        } else {
            agregarToken()
            solicitarPermisoNotificaciones()
        }
    }

    private fun actualizarEstado(estado: String) {
        val ref = FirebaseDatabase.getInstance().reference.child("Usuarios").child(firebaseAuth.uid!!)

        val hashMap = HashMap<String, Any>()
        hashMap["estado"] = estado
        ref.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser != null) {
            actualizarEstado("Online")
        }
    }

    override fun onPause() {
        super.onPause()
        if (firebaseAuth.currentUser != null) {
            actualizarEstado("Offline")
        }
    }

    private fun agregarToken() {
        val miUid = "${firebaseAuth.uid}"
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                val hashMap = HashMap<String, Any>()
                hashMap["fcmToken"] = fcmToken
                val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
                ref.child(miUid)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {
                        // Éxito
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                concederPermiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val concederPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
            if (esConcedido) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
}
