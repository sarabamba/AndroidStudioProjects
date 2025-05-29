package com.sara.chat_kotlin.adaptadores

import android.content.Context
import Modelos.Chats
import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sara.chat_kotlin.Constantes
import com.sara.chat_kotlin.R
import com.sara.chat_kotlin.chat.ChatActivity
import com.sara.chat_kotlin.databinding.ItemChatsBinding

class AdaptadorChats : RecyclerView.Adapter<AdaptadorChats.HolderChats> {
    private lateinit var context: Context
    private lateinit var chatArrayList: ArrayList<Chats>
    private lateinit var binding : ItemChatsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var miUid = ""

    // Constantes para SharedPreferences y claves
    private val prefsName = "PREFS"
    private val pinKey = "PIN_PARENTAL"
    private val chatsDesbloqueadosKey = "CHATS_DESBLOQUEADOS"

    constructor(context: Context, chatArrayList: ArrayList<Chats>) {
        this.context = context
        this.chatArrayList = chatArrayList
        firebaseAuth = FirebaseAuth.getInstance()
        miUid = firebaseAuth.uid!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChats {
        binding = ItemChatsBinding.inflate(LayoutInflater.from(context),parent,false )
        return HolderChats(binding.root)
    }

    override fun getItemCount(): Int {
        return chatArrayList.size
    }

    override fun onBindViewHolder(holder: HolderChats, position: Int) {
        val modeloChats = chatArrayList[position]

        cargarUltimoMensaje(modeloChats, holder)

        holder.itemView.setOnClickListener{
            val uidRecibimos = modeloChats.uidRecibimos
            if (uidRecibimos != null) {
                val sharedPref = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val chatsDesbloqueados = sharedPref.getStringSet(chatsDesbloqueadosKey, mutableSetOf()) ?: mutableSetOf()

                if (chatsDesbloqueados.contains(uidRecibimos)) {
                    // Chat ya desbloqueado: abrir directamente
                    abrirChat(uidRecibimos)
                } else {
                    // Pedir PIN parental
                    mostrarDialogoPin(uidRecibimos)
                }
            }
        }
    }

    private fun abrirChat(uidRecibimos: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("uid", uidRecibimos)
        context.startActivity(intent)
    }

    private fun mostrarDialogoPin(uidRecibimos: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle("Introduce el PIN parental")

        val input = android.widget.EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val pinIntroducido = input.text.toString()

            val sharedPref = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val pinGuardado = sharedPref.getString(pinKey, null)

            if (pinGuardado == null) {
                android.widget.Toast.makeText(context, "PIN parental no configurado", android.widget.Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setPositiveButton
            }

            if (pinIntroducido == pinGuardado) {
                // Guardar chat desbloqueado
                val chatsDesbloqueados = sharedPref.getStringSet(chatsDesbloqueadosKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                chatsDesbloqueados.add(uidRecibimos)
                sharedPref.edit().putStringSet(chatsDesbloqueadosKey, chatsDesbloqueados).apply()

                abrirChat(uidRecibimos)
                dialog.dismiss()
            } else {
                android.widget.Toast.makeText(context, "PIN incorrecto", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun cargarUltimoMensaje(modeloChats: Chats, holder: HolderChats) {
        val chatKey = modeloChats.KeyChat

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatKey).limitToLast(1)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children){
                        val emisorUid = "${ds.child("emisorUid").value}"
                        val idMensaje = "${ds.child("idMensaje").value}"
                        val mensaje = "${ds.child("mensaje").value}"
                        val receptorUid = "${ds.child("receptorUid").value}"
                        val tiempo = ds.child("tiempo").value as Long
                        val tipoMensaje = "${ds.child("tipoMensaje").value}"

                        val formatoFechaHora = Constantes.obtenerFechaHora(tiempo)

                        modeloChats.emisorUid = emisorUid
                        modeloChats.idMensaje = idMensaje
                        modeloChats.mensaje = mensaje
                        modeloChats.receptorUid =receptorUid
                        modeloChats.tipoMensaje = tipoMensaje

                        holder.tvFecha.text = "$formatoFechaHora"

                        if (tipoMensaje == Constantes.MENSAJE_TIPO_TEXTO){
                            holder.tvUltimoMensaje.text = mensaje
                        }else{
                            holder.tvUltimoMensaje.text = "Se ha enviado una imagen"
                        }

                        cargarInfoUsuarioRecibido(modeloChats,holder)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun cargarInfoUsuarioRecibido(modeloChats: Chats, holder: HolderChats) {
        val emisorUid = modeloChats.emisorUid
        val receptorUid = modeloChats.receptorUid

        var uidRecibimos = ""
        if (emisorUid == miUid){
            uidRecibimos = receptorUid
        }else{
            uidRecibimos = emisorUid
        }
        modeloChats.uidRecibimos = uidRecibimos

        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uidRecibimos)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = "${snapshot.child("nombres").value}"
                    val imagen = "${snapshot.child("imagen").value}"

                    modeloChats.nombres = nombres
                    modeloChats.imagen = imagen

                    holder.tvNombres.text = nombres

                    try{
                        Glide.with(context.applicationContext)
                            .load(imagen)
                            .placeholder(R.drawable.ic_img_perfil)
                            .into(holder.IvPerfil)
                    }catch (e:Exception){
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    inner class HolderChats (itemView: View) : RecyclerView.ViewHolder(itemView){
        var  IvPerfil = binding.Ivperfil
        var tvNombres = binding.tvNombres
        var tvUltimoMensaje = binding.tvUltimoMensaje
        var tvFecha = binding.tvFecha

    }
}
