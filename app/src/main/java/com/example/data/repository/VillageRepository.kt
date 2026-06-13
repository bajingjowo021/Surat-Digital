package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class VillageRepository(
    private val pendudukDao: PendudukDao,
    private val suratDao: SuratDao,
    private val configDao: VillageConfigDao
) {
    val allPenduduk: Flow<List<Penduduk>> = pendudukDao.getAllPenduduk()
    val allSurat: Flow<List<Surat>> = suratDao.getAllSurat()
    val config: Flow<VillageConfig?> = configDao.getConfig()

    fun searchPenduduk(query: String): Flow<List<Penduduk>> {
        return pendudukDao.searchPenduduk("%$query%")
    }

    fun searchSurat(query: String): Flow<List<Surat>> {
        return suratDao.searchSurat("%$query%")
    }

    suspend fun getPendudukByNik(nik: String): Penduduk? {
        return pendudukDao.getPendudukByNik(nik)
    }

    suspend fun getPendudukByNama(nama: String): Penduduk? {
        return pendudukDao.getPendudukByNama("%$nama%")
    }

    suspend fun getSuratById(id: Int): Surat? {
        return suratDao.getSuratById(id)
    }

    suspend fun getSuratByQrCode(code: String): Surat? {
        return suratDao.getSuratByQrCode(code)
    }

    suspend fun insertPenduduk(penduduk: Penduduk) {
        pendudukDao.insert(penduduk)
    }

    suspend fun deletePenduduk(penduduk: Penduduk) {
        pendudukDao.delete(penduduk)
    }

    suspend fun saveSurat(surat: Surat): Long {
        return suratDao.insertSurat(surat)
    }

    suspend fun deleteSuratById(id: Int) {
        suratDao.deleteSuratById(id)
    }

    suspend fun saveConfig(config: VillageConfig) {
        configDao.insertConfig(config)
    }

    fun countPendudukFlow(): Flow<Int> = pendudukDao.countPendudukFlow()
    fun countKkFlow(): Flow<Int> = pendudukDao.countKkFlow()

    fun countToday(today: String): Flow<Int> = suratDao.countToday(today)
    fun countMonth(monthPattern: String): Flow<Int> = suratDao.countMonth(monthPattern)
    fun countYear(yearPattern: String): Flow<Int> = suratDao.countYear(yearPattern)

    suspend fun seedDatabaseIfNeeded() {
        if (pendudukDao.countPenduduk() == 0) {
            // Seed residents (Penduduk)
            val initialPenduduk = listOf(
                Penduduk(
                    nik = "3317011212950001",
                    noKk = "3317010101900001",
                    namaLengkap = "Ahmad Fauzi",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1995-12-12",
                    namaAyah = "Supardi",
                    namaIbu = "Siti Rahayu",
                    pendidikan = "S1 Teknik Informatika",
                    pekerjaan = "Karyawan Swasta",
                    statusPerkawinan = "Belum Kawin",
                    statusHubunganKeluarga = "Anak",
                    agama = "Islam",
                    kewarganegaraan = "WNI",
                    dusun = "Krajan",
                    rt = "02",
                    rw = "01"
                ),
                Penduduk(
                    nik = "3317015504980002",
                    noKk = "3317010101900002",
                    namaLengkap = "Siti Aminah",
                    jenisKelamin = "Perempuan",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1998-04-15",
                    namaAyah = "Kurniawan",
                    namaIbu = "Hartati",
                    pendidikan = "SMA",
                    pekerjaan = "Mengurus Rumah Tangga",
                    statusPerkawinan = "Belum Kawin",
                    statusHubunganKeluarga = "Anak",
                    agama = "Islam",
                    kewarganegaraan = "WNI",
                    dusun = "Krajan",
                    rt = "01",
                    rw = "01"
                ),
                Penduduk(
                    nik = "3317011508880003",
                    noKk = "3317010101900003",
                    namaLengkap = "Budiono",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Sleman",
                    tanggalLahir = "1988-08-15",
                    namaAyah = "Sumardji",
                    namaIbu = "Sumarni",
                    pendidikan = "SD Sederajat",
                    pekerjaan = "Petani / Pekebun",
                    statusPerkawinan = "Kawin",
                    statusHubunganKeluarga = "Kepala Keluarga",
                    agama = "Islam",
                    kewarganegaraan = "WNI",
                    dusun = "Mulyoharjo",
                    rt = "03",
                    rw = "02"
                ),
                Penduduk(
                    nik = "3317010405800004",
                    noKk = "3317010101900004",
                    namaLengkap = "Anang Hermansyah",
                    jenisKelamin = "Laki-laki",
                    tempatLahir = "Rembang",
                    tanggalLahir = "1980-05-04",
                    namaAyah = "Dahlan",
                    namaIbu = "Siti Aminah",
                    pendidikan = "D3 Ekonomi",
                    pekerjaan = "Pedagang",
                    statusPerkawinan = "Kawin",
                    statusHubunganKeluarga = "Kepala Keluarga",
                    agama = "Islam",
                    kewarganegaraan = "WNI",
                    dusun = "Mulyoharjo",
                    rt = "01",
                    rw = "02"
                ),
                Penduduk(
                    nik = "3317016209990005",
                    noKk = "3317010101900001",
                    namaLengkap = "Dewi Lestari",
                    jenisKelamin = "Perempuan",
                    tempatLahir = "Semarang",
                    tanggalLahir = "1999-09-22",
                    namaAyah = "Handoyo",
                    namaIbu = "Setyowati",
                    pendidikan = "S1 Akuntansi",
                    pekerjaan = "Wiraswasta",
                    statusPerkawinan = "Belum Kawin",
                    statusHubunganKeluarga = "Kepala Keluarga",
                    agama = "Kristen",
                    kewarganegaraan = "WNI",
                    dusun = "Ploso",
                    rt = "04",
                    rw = "03"
                )
            )
            pendudukDao.insertAll(initialPenduduk)
        }

        if (configDao.getConfigSync() == null) {
            // Seed default Village Config
            val defaultConfig = VillageConfig(
                id = 1,
                namaDesa = "Sumberagung",
                kecamatan = "Kragan",
                kabupaten = "Rembang",
                alamat = "Jl. Raya Sumberagung No. 1, Kode Pos 59273",
                telepon = "(0295) 678910",
                website = "www.sumberagung-rembang.desa.id",
                kadesNama = "H. Bambang Sulistyo, S.Sos.",
                kadesNip = "197203152002121003",
                sekdesNama = "Ahmad Sholihin",
                formatNomorSurat = "470/[COUNTER]/[MONTH]/[YEAR]"
            )
            configDao.insertConfig(defaultConfig)

            // Let's also preseed a few completed letters to represent historical data
            val seededLetters = listOf(
                Surat(
                    nomorSurat = "470/001/06/2026",
                    jenisSurat = "Surat Keterangan Usaha",
                    nikPenduduk = "3317011508880003",
                    namaPenduduk = "Budiono",
                    tanggalBuat = "2026-06-10",
                    operator = "Sari Operator",
                    penandatanganNama = "H. Bambang Sulistyo, S.Sos.",
                    penandatanganJabatan = "Kepala DesaSumberagung",
                    status = "Ditandatangani",
                    qrVerifiedCode = UUID.randomUUID().toString(),
                    dataTambahanJson = "{\"namaUsaha\": \"Toko Kelontong Berkah\", \"lokasiUsaha\": \"Pasar Kliwon Sumberagung\", \"keperluan\": \"Persyaratan Pengajuan KUR BRI\"}"
                ),
                Surat(
                    nomorSurat = "470/002/06/2026",
                    jenisSurat = "Surat Keterangan Tidak Mampu",
                    nikPenduduk = "3317015504980002",
                    namaPenduduk = "Siti Aminah",
                    tanggalBuat = "2026-06-11",
                    operator = "Sari Operator",
                    penandatanganNama = "H. Bambang Sulistyo, S.Sos.",
                    penandatanganJabatan = "Kepala Desa Sumberagung",
                    status = "Ditandatangani",
                    qrVerifiedCode = UUID.randomUUID().toString(),
                    dataTambahanJson = "{\"keperluan\": \"Keringanan Biaya Pendidikan Kuliah\", \"penghasilan\": \"Rp 800.000\"}"
                )
            )
            for (letter in seededLetters) {
                suratDao.insertSurat(letter)
            }
        }
    }
}
