package com.sara.chat_kotlin.fragmentos

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.sara.chat_kotlin.databinding.FragmentUsuariosBinding
import com.sara.chat_kotlin.adaptadores.AdaptadorUsuario
import Modelos.Usuario

class FragmentUsuarios : Fragment() {

    private lateinit var binding: FragmentUsuariosBinding
    private lateinit var mContext: Context

    private var usuarioLista = ArrayList<Usuario>()
    private var usuarioAdaptador: AdaptadorUsuario? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsuariosBinding.inflate(inflater, container, false)

        binding.RVUsuarios.setHasFixedSize(true)
        binding.RVUsuarios.layoutManager = LinearLayoutManager(mContext)

        usuarioAdaptador = AdaptadorUsuario(mContext, usuarioLista)
        binding.RVUsuarios.adapter = usuarioAdaptador

        binding.etBuscarUsuario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buscarUsuario(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listarUsuarios()

        return binding.root
    }

    private fun listarUsuarios() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = FirebaseDatabase.getInstance().reference.child("Usuarios").orderByChild("nombres")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarioLista.clear()
                for (sn in snapshot.children) {
                    val usuario = sn.getValue(Usuario::class.java)
                    if (usuario != null && usuario.uid != currentUserUid) {
                        usuarioLista.add(usuario)
                    }
                }
                usuarioAdaptador?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun buscarUsuario(nombre: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = FirebaseDatabase.getInstance().reference
            .child("Usuarios").orderByChild("nombres")
            .startAt(nombre).endAt(nombre + "\uf8ff")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarioLista.clear()
                for (sn in snapshot.children) {
                    val usuario = sn.getValue(Usuario::class.java)
                    if (usuario != null && usuario.uid != currentUserUid) {
                        usuarioLista.add(usuario)
                    }
                }
                usuarioAdaptador?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
