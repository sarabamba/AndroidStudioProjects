package com.sara.chat_kotlin.chat

import Modelos.Chat
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request.Method
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.sara.chat_kotlin.Constantes
import com.sara.chat_kotlin.R
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.JsonObject
import com.sara.chat_kotlin.adaptadores.AdaptadorChat
import com.sara.chat_kotlin.databinding.ActivityChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.android.volley.Response
import android.util.Log




class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private var uid = ""

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var miUid = ""

    private var chatRuta = ""
    private var imagenUri: Uri? = null

    private var recibimosToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        uid = intent.getStringExtra("uid")!!
        miUid = firebaseAuth.uid!!
        chatRuta = Constantes.rutaChat(uid, miUid)

        binding.adjuntarFAB.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                imagenGaleria()
            } else {
                solicitarPermisoAlmacenamiento.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.enviarFAB.setOnClickListener {
            validarMensaje()
        }

        binding.IbRegresar.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        cargarInfo()
        cargarMensajes()
        cargarTokenDestinatario()
    }

    private fun cargarMensajes() {
        val mensajesArrayList = ArrayList<Chat>()
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatRuta)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    mensajesArrayList.clear()
                    for (ds: DataSnapshot in snapshot.children) {
                        try {
                            val chat = ds.getValue(Chat::class.java)
                            mensajesArrayList.add(chat!!)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val adaptadorChat = AdaptadorChat(this@ChatActivity, mensajesArrayList)
                    binding.chatsRV.layoutManager = LinearLayoutManager(this@ChatActivity)
                    binding.chatsRV.adapter = adaptadorChat

                    binding.chatsRV.setHasFixedSize(true)
                    val linearLayoutManager = LinearLayoutManager(this@ChatActivity)
                    linearLayoutManager.stackFromEnd = true
                    binding.chatsRV.layoutManager = linearLayoutManager
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChatActivity,
                        "Error al cargar mensajes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun validarMensaje() {
        val mensaje = binding.EtMensajeChat.text.toString().trim()
        val tiempo = Constantes.obtenerTiempoD()

        if (mensaje.isEmpty()) {
            Toast.makeText(this, "Ingrese un mensaje", Toast.LENGTH_SHORT).show()
        } else {
            enviarMensaje(Constantes.MENSAJE_TIPO_TEXTO, mensaje, tiempo)
        }
    }

    private fun cargarInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios")
        ref.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombres = "${snapshot.child("nombres").value}"
                val imagen = "${snapshot.child("imagen").value}"
                val estado = "${snapshot.child("estado").value}"

                binding.txtEstadoChat.text = estado
                binding.txtNombreUsuario.text = nombres

                try {
                    Glide.with(applicationContext)
                        .load(imagen)
                        .placeholder(R.drawable.perfil_usuario)
                        .into(binding.toolbarIv)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun cargarTokenDestinatario() {
        val ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
        ref.child("token").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recibimosToken = snapshot.value?.toString() ?: ""
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ChatActivity,
                    "No se pudo obtener el token del destinatario",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun imagenGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultadoGaleriaARL.launch(intent)
    }

    private val resultadoGaleriaARL =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == Activity.RESULT_OK) {
                val data = resultado.data
                imagenUri = data?.data
                subirImgStorage()
            } else {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }

    private val solicitarPermisoAlmacenamiento =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { esConcedido ->
            if (esConcedido) {
                imagenGaleria()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de almacenamiento no concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun subirImgStorage() {
        progressDialog.setMessage("Subiendo Imagen")
        progressDialog.show()

        val tiempo = Constantes.obtenerTiempoD()
        val nombreRutaImg = "ImagenesChat/$tiempo"
        val storageRef = FirebaseStorage.getInstance().getReference(nombreRutaImg)

        imagenUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        val urlImagen = downloadUri.toString()
                        enviarMensaje(Constantes.MENSAJE_TIPO_IMAGEN, urlImagen, tiempo)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "No se pudo enviar la imagen debido a ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                }
        }
    }

    private fun enviarMensaje(tipoMensaje: String, mensaje: String, tiempo: Long) {
        progressDialog.setMessage("Enviando mensaje")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")
        val keyId = refChat.push().key ?: System.currentTimeMillis().toString()

        val hashMap = HashMap<String, Any>()
        hashMap["idMensaje"] = keyId
        hashMap["tipoMensaje"] = tipoMensaje
        hashMap["mensaje"] = mensaje
        hashMap["emisorUid"] = miUid
        hashMap["receptorUid"] = uid
        hashMap["tiempo"] = tiempo

        refChat.child(chatRuta).child(keyId).setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                binding.EtMensajeChat.setText("")

                if (tipoMensaje == Constantes.MENSAJE_TIPO_TEXTO) {
                    prepararNotificacion(mensaje)
                } else {
                    prepararNotificacion("Se envió una imagen")
                }

            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se pudo enviar el mensaje debido a ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()


            }
    }

    private fun obtenerAccesToken(): String? {
        return try {
            val servicioCuenta = applicationContext.assets.open("service-account.json")
            val googleCredentials = GoogleCredentials.fromStream(servicioCuenta)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase/messaging"))
            googleCredentials.refreshIfExpired()
            googleCredentials.accessToken.tokenValue
        } catch (e: Exception) {
            null
        }
    }

    private fun prepararNotificacion(mensaje: String) {
        try {
            val notificationPayload = JSONObject()
            notificationPayload.put("title", "Nuevo mensaje")
            notificationPayload.put("body", mensaje)

            val messageData = JSONObject()
            messageData.put("notificationType", "nuevo_mensaje")
            messageData.put("senderUid", firebaseAuth.uid)

            val messageJo = JSONObject()
            messageJo.put("notification", notificationPayload)
            messageJo.put("data", messageData)
            messageJo.put("token", recibimosToken)

            val notificationJo = JSONObject()
            notificationJo.put("message", messageJo)

            enviarNotificacion(notificationJo)  // <-- llamada para enviar la notificación

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }


    private fun enviarNotificacion(notificationJo: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            val url =
                "https://fcm.googleapis.com/v1/projects/chat---kotlin-eac20/messages:send" // https y no http
            val accessToken = obtenerAccesToken()
            if (accessToken != null) {
                withContext(Dispatchers.Main) {
                    val queue = Volley.newRequestQueue(this@ChatActivity)  //
                    val jsonObjectRequest = object : JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        notificationJo,
                        { response ->
                            // La solicitud es exitosa
                            println("Notificación enviada correctamente")
                        },
                        { error ->
                            // La solicitud NO es exitosa
                            error.printStackTrace()
                        }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            headers["Content-Type"] = "application/json"
                            headers["Authorization"] = "Bearer $accessToken"
                            return headers
                        }
                    }
                    Volley.newRequestQueue(this@ChatActivity).add(jsonObjectRequest)
                }
            } else {
                Log.e("Error", "No se pudo el token de acceso")
            }

        }
    }


    private fun actualizarEstado(estado: String) {
        val ref =
            FirebaseDatabase.getInstance().reference.child("Usuarios").child(firebaseAuth.uid!!)
        val hashMap = HashMap<String, Any>()
        hashMap["estado"] = estado
        ref.updateChildren(hashMap)
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser != null) {
            actualizarEstado("Online")
        }
        actualizarEstado("Online")
    }

    override fun onStop() {
        if (firebaseAuth.currentUser != null) {
        }
        super.onStop()
        actualizarEstado("Offline")
    }

    override fun onPause() {
        super.onPause()
        if (firebaseAuth.currentUser != null) {
            actualizarEstado("Offline")
        }
    }
}
