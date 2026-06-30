package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request/Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Retrofit Client Singleton ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val geminiService: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Gemini Repository/Engine ---

object GeminiDeobfuscator {

    /**
     * Call Gemini to analyze, deobfuscate, beautify, and rename variables.
     * Returns a Pair: first = deobfuscated lua script, second = markdown explanation of what the script does
     */
    suspend fun deobfuscateWithAI(obfuscatedScript: String): Pair<String, String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("API Key Gemini belum dikonfigurasi. Silakan masukkan di panel Secrets AI Studio.")
        }

        val prompt = """
            Berikut adalah skrip Lua Roblox yang ter-obfuscate atau tersensor.
            Lakukan deobfuscasi menyeluruh terhadap skrip ini dengan mengikuti aturan berikut:
            1. Analisis alur kontrol, variabel, tabel, dan fungsi.
            2. Ubah nama semua variabel acak, tidak jelas, atau tersensor (seperti _mek_0x12a3, v1, v2, l1l, dll.) menjadi nama yang bermakna (semantik) berdasarkan cara variabel tersebut digunakan (misal: jika mengakses 'game.Players.LocalPlayer', namakan 'localPlayer').
            3. Hapus semua kode sampah (junk code) atau pemeriksaan palsu yang tidak mengubah hasil eksekusi program.
            4. Format kode agar terindentasi dengan rapi, indah, dan mudah dibaca (Luau format).
            5. Berikan keluaran terpisah:
               - Bagian kode deobfuscated murni (tanpa penjelasan).
               - Bagian penjelasan fungsi, fitur skrip, serta analisis potensi bahaya atau backdoor (backdoor check).

            Format respons harus dalam JSON dengan struktur berikut:
            {
               "deobfuscated_code": "KODE LUA YANG DEOBFUSCATED DI SINI",
               "explanation": "PENJELASAN MARKDOWN DI SINI"
            }
        """.trimIndent()

        val systemInstructionText = "You are an expert Luau and Roblox script security analyst and deobfuscator."

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Prompt:\n$prompt\n\nSkrip Obfuscated:\n$obfuscatedScript")
                    )
                )
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(text = systemInstructionText)
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json"
            )
        )

        val response = RetrofitClient.geminiService.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Tidak ada respons dari server Gemini API.")

        // Parse JSON response safely
        val jsonAdapter = RetrofitClient.moshi.adapter(Map::class.java)
        val resultMap = jsonAdapter.fromJson(responseText)
            ?: throw Exception("Gagal mengurai respons JSON dari Gemini.")

        val deobfuscatedCode = resultMap["deobfuscated_code"] as? String ?: ""
        val explanation = resultMap["explanation"] as? String ?: "Gagal memproses penjelasan."

        return Pair(deobfuscatedCode, explanation)
    }
}
