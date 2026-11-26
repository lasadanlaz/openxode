package com.uwu.animex.core.pairing

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.uwu.animex.core.network.NetworkModule
import com.uwu.animex.data.anilist.AniList
import com.uwu.animex.data.mal.Mal
import com.uwu.animex.sync.AuthData
import com.uwu.animex.sync.AccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

internal data class PairingLink(
    val host: String,
    val port: Int,
    val code: String,
)

internal data class PairingSession(
    val link: PairingLink,
    val deepLink: String,
)

internal enum class PairingPhase { IDLE, WAITING, SUCCESS, ERROR }

internal data class PairRequest(
    val code: String? = null,
    val mal: AuthData? = null,
    val anilist: AuthData? = null,
)

internal data class PairResponse(
    val ok: Boolean,
    val mal: Boolean = false,
    val anilist: Boolean = false,
    val message: String? = null,
)

/** Deep link dari QR yang dipindai HP: animex://pair?host=...&port=...&code=... */
internal object PairingRouter {
    val pending = MutableStateFlow<PairingLink?>(null)
}

private const val PAIR_PORT = 8787
private const val TIMEOUT_MS = 5 * 60 * 1000L
private const val MAX_BODY = 16 * 1024

/** Server pairing lokal di TV. Hanya menerima kode sekali pakai dari jaringan yang sama. */
internal object PairingServer {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var appContext: Context? = null
    private var server: ServerSocket? = null
    private var acceptJob: Job? = null
    private var timeoutJob: Job? = null
    private var expectedCode: String? = null
    private var gotMal = false
    private var gotAniList = false

    private val _phase = MutableStateFlow(PairingPhase.IDLE)
    val phase: StateFlow<PairingPhase> = _phase

    private val _session = MutableStateFlow<PairingSession?>(null)
    val session: StateFlow<PairingSession?> = _session

    private val _detail = MutableStateFlow<String?>(null)
    val detail: StateFlow<String?> = _detail

    @Synchronized
    fun start(context: Context): PairingSession? {
        appContext = context.applicationContext
        AccountManager.init(appContext ?: context)
        val existing = _session.value
        if (server != null && existing != null && _phase.value == PairingPhase.WAITING) return existing

        val host = lanIp(context)
        if (host == null) {
            _detail.value = "TV tidak punya alamat IP jaringan. Hubungkan ke Wi-Fi dulu."
            _phase.value = PairingPhase.ERROR
            return null
        }

        val code = buildString {
            val rnd = SecureRandom()
            repeat(6) { append(rnd.nextInt(10)) }
        }
        val bindAddress = InetAddress.getByName(host)
        var port = PAIR_PORT
        val socket =
            try {
                ServerSocket(port, 8, bindAddress)
            } catch (_: Exception) {
                try {
                    ServerSocket(0, 8, bindAddress).also { port = it.localPort }
                } catch (e: Exception) {
                    _detail.value = "Gagal membuka port pairing: ${e.message.orEmpty()}"
                    _phase.value = PairingPhase.ERROR
                    return null
                }
            }

        server = socket
        expectedCode = code
        gotMal = false
        gotAniList = false
        val link = PairingLink(host, port, code)
        val session =
            PairingSession(
                link = link,
                deepLink = "animex://pair?host=$host&port=$port&code=$code",
            )
        _session.value = session
        _detail.value = "Menunggu HP di jaringan yang sama…"
        _phase.value = PairingPhase.WAITING

        acceptJob?.cancel()
        acceptJob = scope.launch { acceptLoop(socket) }
        timeoutJob?.cancel()
        timeoutJob =
            scope.launch {
                delay(TIMEOUT_MS)
                if (_phase.value == PairingPhase.WAITING) {
                    stop()
                    _detail.value = "Waktu pairing habis. Buka halaman ini lagi."
                    _phase.value = PairingPhase.ERROR
                }
            }
        return session
    }

    @Synchronized
    fun stop() {
        runCatching { server?.close() }
        server = null
        acceptJob?.cancel()
        timeoutJob?.cancel()
        acceptJob = null
        timeoutJob = null
        expectedCode = null
        if (_phase.value == PairingPhase.WAITING) _phase.value = PairingPhase.IDLE
    }

    private suspend fun acceptLoop(socket: ServerSocket) =
        withContext(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                val client =
                    try {
                        socket.accept()
                    } catch (_: Exception) {
                        break
                    }
                launch { handleClient(client) }
            }
        }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 15_000
            val input = client.getInputStream()
            val headerLine = StringBuilder()
            var requestLine = ""
            var length = 0
            while (true) {
                val byte = input.read()
                if (byte == -1) return
                if (byte == '\n'.code) {
                    val line = headerLine.toString().trimEnd('\r')
                    headerLine.setLength(0)
                    if (requestLine.isEmpty()) requestLine = line
                    if (line.isEmpty()) break
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        length = line.substringAfter(':').trim().toIntOrNull() ?: 0
                    }
                } else {
                    headerLine.append(byte.toChar())
                }
            }
            if (!requestLine.startsWith("POST ")) {
                respond(client, "405 Method Not Allowed", PairResponse(false, message = "POST dibutuhkan"))
                return
            }
            if (length <= 0 || length > MAX_BODY) {
                respond(client, "400 Bad Request", PairResponse(false, message = "Ukuran data tidak valid"))
                return
            }
            val bytes = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val read = input.read(bytes, offset, length - offset)
                if (read < 0) break
                offset += read
            }
            if (offset != length) return
            val request =
                runCatching {
                    gson.fromJson(String(bytes, 0, offset, Charsets.UTF_8), PairRequest::class.java)
                }.getOrNull()
            if (request == null || request.code.isNullOrBlank() || request.code != expectedCode) {
                respond(client, "401 Unauthorized", PairResponse(false, message = "Kode pairing salah"))
                return
            }

            request.mal?.takeIf { it.token.accessToken?.isNotBlank() == true }?.let {
                Mal.importAuth(it)
                gotMal = true
            }
            request.anilist?.takeIf { it.token.accessToken?.isNotBlank() == true }?.let {
                AniList.importAuth(it)
                gotAniList = true
            }
            if (!gotMal && !gotAniList) {
                respond(client, "400 Bad Request", PairResponse(false, message = "Tidak ada akun MAL/AniList di HP"))
                return
            }

            respond(client, "200 OK", PairResponse(true, gotMal, gotAniList))
            _detail.value =
                listOfNotNull(
                    if (gotMal) "MAL tersambung" else null,
                    if (gotAniList) "AniList tersambung" else null,
                ).joinToString(" • ")
            _phase.value = PairingPhase.SUCCESS
            stop()
        } catch (e: Exception) {
            runCatching {
                respond(client, "500 Internal Server Error", PairResponse(false, message = e.message.orEmpty()))
            }
        } finally {
            runCatching { client.close() }
        }
    }

    private fun respond(
        client: Socket,
        status: String,
        body: PairResponse,
    ) {
        val bytes = gson.toJson(body).toByteArray(Charsets.UTF_8)
        val header =
            "HTTP/1.1 $status\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        val out = client.getOutputStream()
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun lanIp(context: Context): String? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val addresses =
            manager.activeNetwork
                ?.let(manager::getLinkProperties)
                ?.linkAddresses
                .orEmpty()
        return addresses
            .mapNotNull { it.address.hostAddress }
            .firstOrNull { value ->
                value.contains('.') &&
                    !value.startsWith("127.") &&
                    !value.startsWith("169.254.")
            }
    }
}

/** Pengirim dari HP ke TV. Memakai OkHttp biasa karena alamatnya IP lokal. */
internal object PairingClient {
    private val gson = Gson()
    private val http =
        NetworkModule.client
            .newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    suspend fun send(
        link: PairingLink,
        mal: AuthData?,
        anilist: AuthData?,
    ): PairResponse =
        withContext(Dispatchers.IO) {
            val body = gson.toJson(PairRequest(link.code, mal, anilist))
            val request =
                Request
                    .Builder()
                    .url("http://${link.host}:${link.port}/pair")
                    .post(body.toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
                    .build()
            runCatching {
                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    runCatching { gson.fromJson(text, PairResponse::class.java) }.getOrNull()
                        ?: PairResponse(false, message = "HTTP ${response.code}")
                }
            }.getOrElse { PairResponse(false, message = it.message.orEmpty()) }
        }
}
