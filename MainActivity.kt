package com.omnia.ai

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val AppBackground = Color(0xFF0B1020)
private val Panel = Color(0xFF131B2E)
private val Accent = Color(0xFF7C8CFF)
private val Muted = Color(0xFF9AA6BF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniaTheme { OmniaApp() } }
    }
}

@Composable
private fun OmniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = Color(0xFF42D6C5),
            background = AppBackground,
            surface = Panel,
            onPrimary = Color.White,
            onBackground = Color(0xFFF5F7FF),
            onSurface = Color(0xFFF5F7FF)
        ),
        content = content
    )
}

private data class ChatMessage(val id: String, val role: String, val text: String)
private data class ChatThread(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val apiItemsJson: String
)

@Composable
private fun OmniaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("omnia_settings", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(KeyVault.read(context)) }
    var model by remember { mutableStateOf(prefs.getString("model", "grok-4.7") ?: "grok-4.7") }
    var threads by remember { mutableStateOf(loadThreads(context)) }
    val dataPrefs = remember { context.getSharedPreferences("omnia_data", Context.MODE_PRIVATE) }
    var activeId by remember { mutableStateOf(dataPrefs.getString("active_thread", null) ?: threads.firstOrNull()?.id.orEmpty()) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (threads.isEmpty()) {
        val first = newThread()
        threads = listOf(first)
        activeId = first.id
        saveThreads(context, threads, activeId)
    }
    val activeThread = threads.firstOrNull { it.id == activeId } ?: threads.first()

    fun updateThread(updated: ChatThread) {
        threads = threads.map { if (it.id == updated.id) updated else it }
        saveThreads(context, threads, activeId)
    }

    fun startNewChat() {
        val fresh = newThread()
        threads = listOf(fresh) + threads
        activeId = fresh.id
        saveThreads(context, threads, activeId)
        notice = "Yeni sohbet hazır."
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(Accent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("O", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("OMNIA AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Sohbet alanın", fontSize = 11.sp, color = Muted)
                }
                IconButton(onClick = { showHistory = true }) {
                    Icon(Icons.Default.History, contentDescription = "Sohbet geçmişi", tint = Muted)
                }
                IconButton(onClick = { startNewChat() }) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni sohbet", tint = Muted)
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Muted)
                }
            }
            Divider(color = Color(0xFF202A40))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Accent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(activeThread.title, color = Color(0xFFE9ECF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("GROK API", color = Color(0xFF42D6C5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            val listState = rememberLazyListState()
            if (activeThread.messages.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).background(Color(0xFF202A49), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", color = Accent, fontSize = 34.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Nereden başlayalım?", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sohbetlerin bu cihazda saklanır. Grok yanıtları için ayarlardan resmî xAI API anahtarını ekle.",
                        color = Muted, fontSize = 13.sp, lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeThread.messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
                LaunchedEffect(activeThread.messages.size) {
                    if (activeThread.messages.isNotEmpty()) listState.animateScrollToItem(activeThread.messages.lastIndex)
                }
            }

            if (sending) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Accent)
                    Spacer(Modifier.width(9.dp))
                    Text("Grok yanıt hazırlıyor…", color = Muted, fontSize = 12.sp)
                }
            }
            notice?.let {
                Text(it, color = Color(0xFFFFC978), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("OMNIA'ya mesaj yaz…", color = Muted, fontSize = 14.sp) },
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val prompt = draft.trim()
                        if (prompt.isNotEmpty() && !sending) {
                            draft = ""
                            if (apiKey.isBlank()) {
                                notice = "Önce Ayarlar bölümünden xAI API anahtarı eklemelisin."
                                draft = prompt
                            } else {
                                sending = true
                                notice = null
                                val userMessage = ChatMessage(System.nanoTime().toString(), "user", prompt)
                                val updatedMessages = activeThread.messages + userMessage
                                val title = if (activeThread.messages.isEmpty()) prompt.take(36) else activeThread.title
                                updateThread(activeThread.copy(title = title, messages = updatedMessages))
                                val threadId = activeThread.id
                                val priorApiItems = activeThread.apiItemsJson
                                val selectedModel = model
                                val selectedKey = apiKey
                                scope.launch {
                                    val outcome = withContext(Dispatchers.IO) {
                                        runCatching { GrokApi.send(selectedKey, selectedModel, priorApiItems, prompt) }
                                    }
                                    val latest = threads.firstOrNull { it.id == threadId }
                                    if (latest != null) {
                                        val assistantText = outcome.fold(
                                            onSuccess = { it.answer },
                                            onFailure = { errorMessage(it) }
                                        )
                                        val assistantMessage = ChatMessage(System.nanoTime().toString(), "assistant", assistantText)
                                        val newApiHistory = outcome.getOrNull()?.apiItemsJson ?: latest.apiItemsJson
                                        updateThread(latest.copy(messages = latest.messages + assistantMessage, apiItemsJson = newApiHistory))
                                    }
                                    sending = false
                                }
                            }
                        }
                    },
                    enabled = !sending,
                    modifier = Modifier.size(52.dp).background(if (sending) Color(0xFF39415A) else Accent, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Gönder", tint = Color.White)
                }
            }
            Text(
                "Yanıtlar xAI API üzerinden gelir. Kullanım ücretleri ve limitleri xAI hesabına bağlıdır.",
                color = Color(0xFF65718C), fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Panel).padding(start = 18.dp, end = 18.dp, bottom = 9.dp)
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentKey = apiKey,
            currentModel = model,
            onDismiss = { showSettings = false },
            onSave = { newKey, newModel ->
                apiKey = newKey.trim()
                model = newModel.trim().ifBlank { "grok-4.7" }
                KeyVault.write(context, apiKey)
                prefs.edit().putString("model", model).apply()
                showSettings = false
                notice = "Ayarlar kaydedildi."
            }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            containerColor = Panel,
            title = { Text("Sohbet geçmişi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    threads.take(20).forEach { thread ->
                        TextButton(
                            onClick = {
                                activeId = thread.id
                                saveThreads(context, threads, activeId)
                                showHistory = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (if (thread.id == activeId) "●  " else "○  ") + thread.title,
                                color = if (thread.id == activeId) Accent else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Kapat") } }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.widthIn(max = 340.dp)) {
            Text(if (isUser) "SEN" else "OMNIA", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            Card(
                shape = RoundedCornerShape(if (isUser) 20.dp else 20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isUser) Color(0xFF303D68) else Panel)
            ) {
                Text(message.text, color = Color(0xFFF2F4FC), fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    currentKey: String,
    currentModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var key by remember(currentKey) { mutableStateOf(currentKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Bağlantı ayarları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Grok için resmî xAI API bağlantısı", color = Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("xAI API anahtarı") },
                    placeholder = { Text("API anahtarını buraya yapıştır") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model kimliği") },
                    singleLine = true
                )
                Text(
                    "Bu alana Grok web sitesi parolanı değil, xAI API anahtarını gir. API kullanımı Grok uygulamasındaki abonelik kotasından ayrıdır ve ayrıca ücretlendirilebilir.",
                    color = Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(key, model) }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

private object GrokApi {
    data class Result(val answer: String, val apiItemsJson: String)

    fun send(apiKey: String, model: String, previousItemsJson: String, prompt: String): Result {
        val history = JSONArray(previousItemsJson)
        val inputItem = JSONObject()
            .put("role", "user")
            .put("content", JSONArray().put(JSONObject().put("type", "input_text").put("text", prompt)))
        history.put(inputItem)

        val payload = JSONObject()
            .put("model", model)
            .put("input", history)
            .put("store", false)
            .put("include", JSONArray().put("reasoning.encrypted_content"))
            .put("max_output_tokens", 2048)

        val connection = (URL("https://api.x.ai/v1/responses").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 900_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(body).optString("error", body) }.getOrDefault(body)
                throw ApiException(status, detail.take(500))
            }

            val response = JSONObject(body)
            val output = response.optJSONArray("output") ?: JSONArray()
            for (i in 0 until output.length()) history.put(output.getJSONObject(i))
            val answer = extractText(response, output)
            if (answer.isBlank()) throw IllegalStateException("Grok boş yanıt döndürdü.")
            return Result(answer, history.toString())
        } finally {
            connection.disconnect()
        }
    }

    private fun extractText(response: JSONObject, output: JSONArray): String {
        val direct = response.optString("output_text", "")
        if (direct.isNotBlank()) return direct
        val parts = mutableListOf<String>()
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val type = part.optString("type")
                if (type == "output_text" || type == "text") {
                    part.optString("text").takeIf { it.isNotBlank() }?.let(parts::add)
                }
            }
        }
        return parts.joinToString("\n\n")
    }
}

private class ApiException(val status: Int, detail: String) : Exception(detail)

private fun errorMessage(error: Throwable): String {
    val apiError = error as? ApiException
    return when (apiError?.status) {
        401, 403 -> "API anahtarı kabul edilmedi. Ayarlar bölümündeki xAI API anahtarını kontrol et."
        429 -> "xAI API kullanım sınırına ulaşıldı. Uygulama hesapları otomatik döndürmez; limitin yenilenmesini bekle veya xAI hesabındaki seçenekleri kontrol et."
        else -> "Bağlantı hatası: ${error.message ?: "Bilinmeyen hata"}"
    }
}

private fun newThread() = ChatThread(
    id = System.nanoTime().toString(),
    title = "Yeni sohbet",
    messages = emptyList(),
    apiItemsJson = "[]"
)

private fun loadThreads(context: Context): List<ChatThread> {
    val raw = context.getSharedPreferences("omnia_data", Context.MODE_PRIVATE).getString("threads", "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val messagesArray = obj.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until messagesArray.length()).mapNotNull { m ->
                val item = messagesArray.optJSONObject(m) ?: return@mapNotNull null
                ChatMessage(item.optString("id"), item.optString("role"), item.optString("text"))
            }
            ChatThread(
                id = obj.optString("id"),
                title = obj.optString("title", "Sohbet"),
                messages = messages,
                apiItemsJson = obj.optString("apiItemsJson", "[]")
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveThreads(context: Context, threads: List<ChatThread>, activeId: String) {
    val array = JSONArray()
    threads.take(30).forEach { thread ->
        val messages = JSONArray()
        thread.messages.forEach { msg ->
            messages.put(JSONObject().put("id", msg.id).put("role", msg.role).put("text", msg.text))
        }
        array.put(
            JSONObject()
                .put("id", thread.id)
                .put("title", thread.title)
                .put("messages", messages)
                .put("apiItemsJson", thread.apiItemsJson)
        )
    }
    context.getSharedPreferences("omnia_data", Context.MODE_PRIVATE).edit()
        .putString("threads", array.toString())
        .putString("active_thread", activeId)
        .apply()
}

private object KeyVault {
    private const val KEY_ALIAS = "omnia_xai_api_key"
    private const val PREFS = "omnia_secure"
    private const val VALUE = "encrypted_key"

    fun write(context: Context, value: String) {
        if (value.isBlank()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(VALUE).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = Base64.getEncoder().encodeToString(cipher.iv) + "." + Base64.getEncoder().encodeToString(encrypted)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(VALUE, packed).apply()
    }

    fun read(context: Context): String {
        val packed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(VALUE, null) ?: return ""
        return runCatching {
            val pieces = packed.split(".")
            require(pieces.size == 2)
            val iv = Base64.getDecoder().decode(pieces[0])
            val encrypted = Base64.getDecoder().decode(pieces[1])
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
