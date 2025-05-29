package com.sara.chat_kotlin.fragmentos

import Modelos.Chats
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sara.chat_kotlin.R
import com.sara.chat_kotlin.adaptadores.AdaptadorChats
import com.sara.chat_kotlin.databinding.FragmentChatsBinding


class FragmentChats : Fragment() {

    // Variables
    private lateinit var binding: FragmentChatsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var chatsArrayList: ArrayList<Chats>
    private lateinit var adaptadorChats: AdaptadorChats
    private lateinit var mContext: Context
    private var miUid: String = ""

    // onAttach
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        miUid = firebaseAuth.uid ?: ""
    }

    // onCreateView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarChats()
    }

    // Método para cargar chats
    private fun cargarChats() {
        chatsArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatsArrayList.clear()
                for (ds in snapshot.children) {
                    val chatKey = ds.key ?: continue
                    if (chatKey.contains(miUid)) {
                        val modeloChats = Chats()
                        modeloChats.KeyChat = chatKey
                        chatsArrayList.add(modeloChats)
                    }
                }
                adaptadorChats = AdaptadorChats(mContext, chatsArrayList)
                binding.chatsRV.adapter = adaptadorChats
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}




































