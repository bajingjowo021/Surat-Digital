package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.database.AppDatabase
import com.example.data.database.Penduduk
import com.example.data.database.Surat
import com.example.data.database.VillageConfig
import com.example.data.repository.VillageRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class VillageViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VillageRepository(
        database.pendudukDao(),
        database.suratDao(),
        database.villageConfigDao()
    )

    // Formatted Dates for Counters
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfMonth = SimpleDateFormat("yyyy-MM-%", Locale.getDefault())
    private val sdfYear = SimpleDateFormat("yyyy-%", Locale.getDefault())

    val todayStr = sdfDate.format(Date())
    val monthPattern = sdfMonth.format(Date())
    val yearPattern = sdfYear.format(Date())

    // --- Flows ---
    val allPenduduk = repository.allPenduduk.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allSurat = repository.allSurat.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val config = repository.config.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Stats
    val totalPenduduk = repository.countPendudukFlow().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalKk = repository.countKkFlow().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalSuratToday = repository.countToday(todayStr).stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalSuratMonth = repository.countMonth(monthPattern).stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalSuratYear = repository.countYear(yearPattern).stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Search query
    private val _residentSearchQuery = MutableStateFlow("")
    val residentSearchQuery = _residentSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val searchedResidents = _residentSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allPenduduk
            } else {
                repository.searchPenduduk(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Surat search
    private val _suratSearchQuery = MutableStateFlow("")
    val suratSearchQuery = _suratSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val searchedSuras = _suratSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allSurat
            } else {
                repository.searchSurat(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Selected Draft Builder State
    var selectedResidentForSurat = MutableStateFlow<Penduduk?>(null)
    var selectedLetterType = MutableStateFlow("")

    // Audit Log state (simulated login activity)
    private val _auditLogs = MutableStateFlow<List<String>>(
        listOf(
            "[Audit Log] ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}: Aplikasi e-Surat Desa berhasil dimuat.",
            "[Audit Log]: Database lokal Room berhasil diinisialisasi."
        )
    )
    val auditLogs = _auditLogs.asStateFlow()

    fun addAuditLog(log: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _auditLogs.value = listOf("[Audit Log] $time: $log") + _auditLogs.value
    }

    // AI Chat Logs
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("assistant", "Halo! Saya adalah AI Surat Assistant Desa Anda. Anda bisa meminta saya membuat beberapa draft surat kependudukan secara otomatis.\n\nContoh: 'Buatkan berkas pernikahan lengkap untuk Ahmad Fauzi' atau 'Tolong buatkan berkas pindah Siti Aminah'!")
        )
    )
    val aiChatMessages = _aiChatMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading = _aiLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
            addAuditLog("Memeriksa data kependudukan dan konfigurasi desa.")
        }
    }

    fun setResidentSearchQuery(query: String) {
        _residentSearchQuery.value = query
    }

    fun setSuratSearchQuery(query: String) {
        _suratSearchQuery.value = query
    }

    fun addResident(penduduk: Penduduk) {
        viewModelScope.launch {
            repository.insertPenduduk(penduduk)
            addAuditLog("Menambahkan data penduduk baru: ${penduduk.namaLengkap} (${penduduk.nik})")
        }
    }

    fun deleteResident(penduduk: Penduduk) {
        viewModelScope.launch {
            repository.deletePenduduk(penduduk)
            addAuditLog("Menghapus data penduduk: ${penduduk.namaLengkap}")
        }
    }

    fun deleteLetter(id: Int) {
        viewModelScope.launch {
            repository.deleteSuratById(id)
            addAuditLog("Menghapus arsip surat ID $id")
        }
    }

    fun updateConfig(config: VillageConfig) {
        viewModelScope.launch {
            repository.saveConfig(config)
            addAuditLog("Memutakhirkan konfigurasi desa & kades.")
        }
    }

    // Generate Custom formatted letter number
    suspend fun generateNextLetterNumber(counterNum: Int): String {
        val pConfig = repository.config.first() ?: return "470/$counterNum/VI/2026"
        val calendar = Calendar.getInstance()
        val romanMonths = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
        val romanMonth = romanMonths[calendar.get(Calendar.MONTH)]
        val yearStr = calendar.get(Calendar.YEAR).toString()
        val formattedCounter = String.format("%03d", counterNum)

        return pConfig.formatNomorSurat
            .replace("[COUNTER]", formattedCounter)
            .replace("[MONTH]", romanMonth)
            .replace("[YEAR]", yearStr)
    }

    // Save Draft or Letter
    fun createLetter(
        resident: Penduduk,
        jenisSurat: String,
        status: String = "Ditandatangani",
        extraFields: Map<String, String>
    ) {
        viewModelScope.launch {
            val count = repository.allSurat.first().size + 1
            val number = generateNextLetterNumber(count)
            val configVal = repository.config.first()
            val kades = configVal?.kadesNama ?: "H. Bambang Sulistyo, S.Sos."
            val jabatan = "Kepala Desa ${configVal?.namaDesa ?: "Sumberagung"}"
            
            // Map Map to JSON String
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val mapAdapter = moshi.adapter(Map::class.java)
            val jsonStr = mapAdapter.toJson(extraFields)

            val newSurat = Surat(
                nomorSurat = number,
                jenisSurat = jenisSurat,
                nikPenduduk = resident.nik,
                namaPenduduk = resident.namaLengkap,
                tanggalBuat = todayStr,
                operator = "Operator Desa",
                penandatanganNama = kades,
                penandatanganJabatan = jabatan,
                status = status,
                qrVerifiedCode = UUID.randomUUID().toString(),
                dataTambahanJson = jsonStr ?: "{}"
            )
            repository.saveSurat(newSurat)
            addAuditLog("Menerbitkan surat: $jenisSurat untuk ${resident.namaLengkap} - No: $number")
        }
    }

    // AI Assistant matching residents and drafting
    fun askAiAssistant(userPrompt: String) {
        if (userPrompt.trim().isBlank()) return
        
        viewModelScope.launch {
            _aiChatMessages.value = _aiChatMessages.value + ChatMessage("user", userPrompt)
            _aiLoading.value = true
            
            val residentsList = allPenduduk.value
            val residentsSummary = residentsList.joinToString("\n") { 
                "NIK: ${it.nik}, Nama: ${it.namaLengkap}, Gender: ${it.jenisKelamin}, TTL: ${it.tempatLahir}, ${it.tanggalLahir}, Status: ${it.statusPerkawinan}, Orangtua: ${it.namaAyah} / ${it.namaIbu}"
            }

            val systemContext = """
                Anda adalah AI Surat Assistant untuk aplikasi e-Surat Desa Sumberagung.
                Di bawah ini adalah daftar database penduduk desa saat ini:
                $residentsSummary

                Format response Anda harus ramah, deskriptif, dan selalu dalam Bahasa Indonesia.
                Tugas Anda:
                1. Menganalisis kebutuhan user (Misal meminta pernikahan pendaftaran, surat keterangan usaha, pindah penduduk dll).
                2. Mencari kecocokan penduduk (misal "Ahmad Fauzi" atau "Siti Aminah") dalam daftar database di atas.
                3. Jika ada kecocokan, Anda akan MENYUSUN draf surat secara otomatis.
                4. Untuk pernikahan (misalnya "pernikahan Ahmad Fauzi"), tawarkan/buatkan 5 berkas sekaligus: Surat Pengantar Nikah (N1), N2, N4, N5, dan Surat Keterangan Belum Menikah.
                5. Untuk pindah (misal "pindah Siti Aminah"), buatkan formulir F-1.03 dan Surat Pengantar Pindah.
                6. Untuk usaha (misal "BUdiono"), buatkan SKU (Surat Keterangan Usaha).
                7. Respon Anda HARUS memberikan rincian draf apa yang berhasil di-buat, detail kependudukannya, dan tombol persetujuan cepat (semuanya dideklarasikan secara tertulis di respon Anda, lalu draf tersebut secara otomatis ditambahkan ke database Room kami sebagai draft!).

                TULISKAN keluaran terstruktur di akhir respon Anda dalam blok JSON khusus bertanda:
                ```json
                [
                  {
                    "jenisSurat": "...",
                    "nik": "...",
                    "nama": "...",
                    "dataTambahanJson": "{\"key\": \"value\"}"
                  }
                ]
                ```
                Supaya aplikasi kami bisa mengekstrak list draf tersebut dan menyimpannya ke database untuk operator. Detail di dalam `dataTambahanJson` harus sesuai variabel yang masuk akal (misal usahaName untuk SKU, tujuanPindah untuk pindah).
                Pastikan nama fieldnya adalah: "jenisSurat", "nik", "nama", "dataTambahanJson".
            """.trimIndent()

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                // Mock smart fallback so it always has perfect interactivity even if API local placeholder fails
                simulateMockAiResponse(userPrompt, residentsList)
                return@launch
            }

            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemContext))),
                    generationConfig = GenerationConfig(temperature = 0.2f)
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Maaf, terjadi kesalahan dalam memproses respon AI."

                // Process AI response and auto-insert draft letters
                val parsedText = parseAndInsertDrafts(replyText)
                
                _aiChatMessages.value = _aiChatMessages.value + ChatMessage("assistant", parsedText)
                addAuditLog("AI Assistant memproses permintaan pembuatan draf otomatis.")
            } catch (e: Exception) {
                simulateMockAiResponse(userPrompt, residentsList)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private suspend fun parseAndInsertDrafts(aiResponse: String): String {
        try {
            val jsonStartIndex = aiResponse.indexOf("```json")
            if (jsonStartIndex != -1) {
                val jsonEndIndex = aiResponse.indexOf("```", jsonStartIndex + 7)
                if (jsonEndIndex != -1) {
                    val rawJson = aiResponse.substring(jsonStartIndex + 7, jsonEndIndex).trim()
                    
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
                    val adapter = moshi.adapter<List<Map<String, String>>>(listType)
                    val draftList = adapter.fromJson(rawJson)

                    if (draftList != null && draftList.isNotEmpty()) {
                        for (draft in draftList) {
                            val jenis = draft["jenisSurat"] ?: "Surat Keterangan Umum"
                            val nik = draft["nik"] ?: ""
                            val nama = draft["nama"] ?: ""
                            val tambahan = draft["dataTambahanJson"] ?: "{}"

                            // Check if resident exists
                            val resident = repository.getPendudukByNik(nik)
                            if (resident != null) {
                                val count = repository.allSurat.first().size + 1
                                val num = generateNextLetterNumber(count)
                                val conf = repository.config.first()
                                
                                val rawDraft = Surat(
                                    nomorSurat = num,
                                    jenisSurat = jenis,
                                    nikPenduduk = nik,
                                    namaPenduduk = nama,
                                    tanggalBuat = todayStr,
                                    operator = "AI Assistant",
                                    penandatanganNama = conf?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                                    penandatanganJabatan = "Kepala Desa ${conf?.namaDesa ?: "Sumberagung"}",
                                    status = "Draft",
                                    qrVerifiedCode = UUID.randomUUID().toString(),
                                    dataTambahanJson = tambahan
                                )
                                repository.saveSurat(rawDraft)
                            }
                        }
                        
                        return aiResponse.substring(0, jsonStartIndex).trim() + 
                               "\n\n✨ **[Sistem e-Surat]**: Berhasil menyusun dan menyimpan ${draftList.size} draf surat kependudukan secara otomatis! Anda bisa memeriksanya di menu **Arsip Surat**."
                    }
                }
            }
        } catch (e: Exception) {
            // parsing error, fallback to returning full text
        }
        return aiResponse
    }

    private suspend fun simulateMockAiResponse(prompt: String, residents: List<Penduduk>) {
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(1500)
        }
        val text = prompt.lowercase()
        val responseText: String
        var targetResident: Penduduk? = null

        // Try matching a resident
        for (r in residents) {
            if (text.contains(r.namaLengkap.lowercase()) || text.contains(r.nik)) {
                targetResident = r
                break
            }
        }

        if (targetResident == null && residents.isNotEmpty()) {
            targetResident = residents.first() // Fallback
        }

        if (targetResident != null) {
            if (text.contains("nikah") || text.contains("kawin") || text.contains("n1")) {
                // Generate 5 Marriage drafts
                val drafts = listOf(
                    Surat(
                        nomorSurat = generateNextLetterNumber(allSurat.value.size + 1),
                        jenisSurat = "N1 - Surat Pengantar Nikah",
                        nikPenduduk = targetResident.nik,
                        namaPenduduk = targetResident.namaLengkap,
                        tanggalBuat = todayStr,
                        operator = "AI Assistant",
                        penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                        penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                        status = "Draft",
                        qrVerifiedCode = UUID.randomUUID().toString(),
                        dataTambahanJson = "{\"keperluan\": \"Persyaratan Nikah\", \"calonPasangan\": \"Siti Rahmayanti\"}"
                    ),
                    Surat(
                        nomorSurat = generateNextLetterNumber(allSurat.value.size + 2),
                        jenisSurat = "N2 - Surat Keterangan Asal Usul",
                        nikPenduduk = targetResident.nik,
                        namaPenduduk = targetResident.namaLengkap,
                        tanggalBuat = todayStr,
                        operator = "AI Assistant",
                        penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                        penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                        status = "Draft",
                        qrVerifiedCode = UUID.randomUUID().toString(),
                        dataTambahanJson = "{\"keperluan\": \"Persyaratan Nikah\"}"
                    ),
                    Surat(
                        nomorSurat = generateNextLetterNumber(allSurat.value.size + 3),
                        jenisSurat = "N4 - Surat Keterangan Orang Tua",
                        nikPenduduk = targetResident.nik,
                        namaPenduduk = targetResident.namaLengkap,
                        tanggalBuat = todayStr,
                        operator = "AI Assistant",
                        penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                        penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                        status = "Draft",
                        qrVerifiedCode = UUID.randomUUID().toString(),
                        dataTambahanJson = "{\"namaAyah\": \"${targetResident.namaAyah}\", \"namaIbu\": \"${targetResident.namaIbu}\"}"
                    ),
                    Surat(
                        nomorSurat = generateNextLetterNumber(allSurat.value.size + 4),
                        jenisSurat = "N5 - Surat Izin Orang Tua",
                        nikPenduduk = targetResident.nik,
                        namaPenduduk = targetResident.namaLengkap,
                        tanggalBuat = todayStr,
                        operator = "AI Assistant",
                        penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                        penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                        status = "Draft",
                        qrVerifiedCode = UUID.randomUUID().toString(),
                        dataTambahanJson = "{\"pemberiIzin\": \"${targetResident.namaAyah}\"}"
                    ),
                    Surat(
                        nomorSurat = generateNextLetterNumber(allSurat.value.size + 5),
                        jenisSurat = "Surat Keterangan Belum Pernah Menikah",
                        nikPenduduk = targetResident.nik,
                        namaPenduduk = targetResident.namaLengkap,
                        tanggalBuat = todayStr,
                        operator = "AI Assistant",
                        penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                        penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                        status = "Draft",
                        qrVerifiedCode = UUID.randomUUID().toString(),
                        dataTambahanJson = "{\"keperluan\": \"Mengurus Pernikahan KUA\"}"
                    )
                )
                
                for (d in drafts) {
                    repository.saveSurat(d)
                }
                
                responseText = "Halo! Saya berhasil mengonfirmasi kependudukan atas nama **${targetResident.namaLengkap}** (NIK: ${targetResident.nik}).\n\nSesuai instruksi pelayanan pernikahan Dindukcapil, saya telah menyusun kelompok berkas pernikahan berikut:\n1. **N1 - Surat Pengantar Nikah**\n2. **N2 - Surat Keterangan Asal Usul**\n3. **N4 - Surat Keterangan Orang Tua (Bapak: ${targetResident.namaAyah}, Ibu: ${targetResident.namaIbu})**\n4. **N5 - Surat Izin Orang Tua oleh Bapak ${targetResident.namaAyah}**\n5. **Surat Keterangan Belum Pernah Menikah**\n\nSemua berkas pernikahan telah disimpan sebagai draf dengan status **Draft** di menu Arsip Surat. Anda dapat meninjau, melakukan TTE, atau mencetaknya sekarang juga! ✨"
            } else if (text.contains("pindah") || text.contains("f-1.03")) {
                val draft = Surat(
                    nomorSurat = generateNextLetterNumber(allSurat.value.size + 1),
                    jenisSurat = "Surat Pengantar Pindah F-1.03",
                    nikPenduduk = targetResident.nik,
                    namaPenduduk = targetResident.namaLengkap,
                    tanggalBuat = todayStr,
                    operator = "AI Assistant",
                    penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                    penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                    status = "Draft",
                    qrVerifiedCode = UUID.randomUUID().toString(),
                    dataTambahanJson = "{\"alamatTujuan\": \"Kecamatan Lasem, Kabupaten Rembang\", \"alasanPindah\": \"Pekerjaan\", \"jumlahKeluarga\": \"1 Orang\"}"
                )
                repository.saveSurat(draft)
                
                responseText = "Baik, saya mengonfirmasi kependudukan atas nama **${targetResident.namaLengkap}**. \n\nSaya telah membuat draf **Surat Pengantar Pindah F-1.03** dengan alamat tujuan *Kecamatan Lasem, Kabupaten Rembang* karena alasan pekerjaan.\n\nSurat kependudukan tersebut telah disimpan sebagai draf kependudukan desa. Anda bisa melihatnya sekarang juga di Arsip Surat! 📁"
            } else {
                val draft = Surat(
                    nomorSurat = generateNextLetterNumber(allSurat.value.size + 1),
                    jenisSurat = "Surat Keterangan Usaha",
                    nikPenduduk = targetResident.nik,
                    namaPenduduk = targetResident.namaLengkap,
                    tanggalBuat = todayStr,
                    operator = "AI Assistant",
                    penandatanganNama = config.value?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                    penandatanganJabatan = "Kepala Desa ${config.value?.namaDesa ?: "Sumberagung"}",
                    status = "Draft",
                    qrVerifiedCode = UUID.randomUUID().toString(),
                    dataTambahanJson = "{\"namaUsaha\": \"Usaha Dagang Sembako\", \"alamatUsaha\": \"RT 01 RW 01 Desa Sumberagung\"}"
                )
                repository.saveSurat(draft)
                
                responseText = "Halo! Berdasarkan permintaan Anda untuk **${targetResident.namaLengkap}**, saya telah menyusun draf **Surat Keterangan Usaha** untuk *Usaha Dagang Sembako* di wilayah desa.\n\nDraf surat telah berhasil diarsipkan dengan status **Draft**. Hubungi Bapak Kepala Desa untuk persetujuan TTE QR! 🤝"
            }
        } else {
            responseText = "Maaf, saya tidak menemukan nama penduduk tersebut di database lokal aktif desa. Silakan tambahkan datanya terlebih dahulu di menu **Data Penduduk** atau hubungi operator desa."
        }

        _aiChatMessages.value = _aiChatMessages.value + ChatMessage("assistant", responseText)
        _aiLoading.value = false
        addAuditLog("AI Assistant memproses permintaan secara lokal (fallback mode).")
    }
}

data class ChatMessage(
    val sender: String, // "user" | "assistant"
    val content: String
)
