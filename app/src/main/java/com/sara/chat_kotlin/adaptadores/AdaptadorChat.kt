package com.sara.chat_kotlin.adaptadores

import Modelos.Chat
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.sara.chat_kotlin.Constantes
import com.sara.chat_kotlin.R
import com.sara.chat_kotlin.PinActivity

class AdaptadorChat(private val context: Context, private val chatArray: ArrayList<Chat>) : RecyclerView.Adapter<AdaptadorChat.HolderChat>() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val MENSAJE_IZQUIERDO = 0
        private const val MENSAJE_DERECHO = 1
        const val REQUEST_PIN = 1234 // Código para identificar el resultado del PIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChat {
        val layout = if (viewType == MENSAJE_DERECHO) R.layout.item_chat_derecho else R.layout.item_chat_izquierdo
        val view = LayoutInflater.from(context).inflate(layout, parent, false)
        return HolderChat(view)
    }

    override fun getItemCount(): Int = chatArray.size

    override fun onBindViewHolder(holder: HolderChat, position: Int) {
        val modeloChat = chatArray[position]

        val mensaje = modeloChat.mensaje
        val tipoMensaje = modeloChat.tipoMensaje
        val tiempo = modeloChat.tiempo

        val formato_fecha_hora = Constantes.obtenerFechaHora(tiempo)
        holder.Iv_tiempo_mensaje.text = formato_fecha_hora

        if (tipoMensaje == Constantes.MENSAJE_TIPO_TEXTO) {
            holder.Tv_mensaje.visibility = View.VISIBLE
            holder.Iv_mensaje.visibility = View.GONE
            holder.Tv_mensaje.text = mensaje
        } else {
            holder.Tv_mensaje.visibility = View.GONE
            holder.Iv_mensaje.visibility = View.VISIBLE

            try {
                Glide.with(context)
                    .load(mensaje)
                    .placeholder(R.drawable.imagen_enviada)
                    .error(R.drawable.imagen_rota)
                    .into(holder.Iv_mensaje)
            } catch (e: Exception) {
                // Manejar error si es necesario
            }
        }

        holder.itemView.setOnClickListener {
            if (esChatNuevo(modeloChat)) {
                // Lanzar la actividad para pedir PIN
                val intent = Intent(context, PinActivity::class.java)

                (context as? Activity)?.startActivityForResult(intent, REQUEST_PIN)

            } else {
                // Chat ya abierto, abrir directamente
                abrirChat(modeloChat)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatArray[position].emisorUid == firebaseAuth.uid) MENSAJE_DERECHO else MENSAJE_IZQUIERDO
    }

    private fun abrirChat(chat: Chat) {

    }

    private fun esChatNuevo(chat: Chat): Boolean {

        return false
    }

    inner class HolderChat(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var Tv_mensaje: TextView = itemView.findViewById(R.id.Tv_mensaje)
        var Iv_mensaje: ShapeableImageView = itemView.findViewById(R.id.Iv_mensaje)
        var Iv_tiempo_mensaje: TextView = itemView.findViewById(R.id.Tv_tiempo_mensaje)
    }
}
