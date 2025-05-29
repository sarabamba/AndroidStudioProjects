package com.sara.chat_kotlin.fragmentos

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sara.chat_kotlin.Constantes
import com.sara.chat_kotlin.EditarInformacion
import com.sara.chat_kotlin.OpcionesLoginActivity
import com.sara.chat_kotlin.R
import com.sara.chat_kotlin.databinding.FragmentPerfilBinding

class FragmentPerfil : Fragment() {

    private lateinit var binding: FragmentPerfilBinding
    private lateinit var mContext: android.content.Context
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onAttach(context: android.content.Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPerfilBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        cargarInformacion()

        binding.btnActualizarInfo.setOnClickListener {
            startActivity(Intent(mContext, EditarInformacion::class.java))
        }

        binding.btnCerrarsesion.setOnClickListener {
            actualizarEstado()
            Toast.makeText(mContext, "Cerrando sesión en 3 segundos...", Toast.LENGTH_SHORT).show()

            object : CountDownTimer(3000, 1000) {
                override fun onTick(p0: Long) {
                    // Puedes mostrar un contador si lo deseas
                }

                override fun onFinish() {
                    cerrarSesion()
                }
            }.start()
        }
    }

    private fun cerrarSesion() {
        firebaseAuth.signOut()
        startActivity(Intent(mContext, OpcionesLoginActivity::class.java))
        activity?.finishAffinity()
    }

    private fun actualizarEstado() {
        val ref = FirebaseDatabase.getInstance().reference.child("Usuarios").child(firebaseAuth.uid!!)
        val hashMap = HashMap<String, Any>()
        hashMap["estado"] = "Offline"
        ref.updateChildren(hashMap)
    }

    private fun cargarInformacion() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val email = "${snapshot.child("email").value}"
                    val proveedor = "${snapshot.child("proveedor").value}"
                    var t_registro = "${snapshot.child("tiempoR").value}"
                    val imagen = "${snapshot.child("imagen").value}"

                    if (t_registro == "null") {
                        t_registro = "0"
                    }

                    val fecha = Constantes.formatoFecha(t_registro.toLong())

                    binding.tvNombres.text = nombres
                    binding.tvEmail.text = email
                    binding.tvProveedor.text = proveedor
                    binding.tvTRegistro.text = fecha

                    try {
                        Glide.with(mContext.applicationContext)
                            .load(imagen)
                            .placeholder(R.drawable.ic_img_perfil)
                            .into(binding.ivPerfil)
                    } catch (e: Exception) {
                        Toast.makeText(
                            mContext,
                            "${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(mContext, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
