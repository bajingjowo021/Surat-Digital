package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendudukDao {
    @Query("SELECT * FROM penduduk ORDER BY namaLengkap ASC")
    fun getAllPenduduk(): Flow<List<Penduduk>>

    @Query("SELECT * FROM penduduk WHERE namaLengkap LIKE :query OR nik LIKE :query OR noKk LIKE :query")
    fun searchPenduduk(query: String): Flow<List<Penduduk>>

    @Query("SELECT * FROM penduduk WHERE nik = :nik LIMIT 1")
    suspend fun getPendudukByNik(nik: String): Penduduk?

    @Query("SELECT * FROM penduduk WHERE namaLengkap LIKE :nama LIMIT 1")
    suspend fun getPendudukByNama(nama: String): Penduduk?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(penduduk: List<Penduduk>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(penduduk: Penduduk)

    @Delete
    suspend fun delete(penduduk: Penduduk)

    @Query("SELECT COUNT(*) FROM penduduk")
    fun countPendudukFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT noKk) FROM penduduk")
    fun countKkFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM penduduk")
    suspend fun countPenduduk(): Int
}

@Dao
interface SuratDao {
    @Query("SELECT * FROM surat_arsip ORDER BY id DESC")
    fun getAllSurat(): Flow<List<Surat>>

    @Query("SELECT * FROM surat_arsip WHERE namaPenduduk LIKE :query OR nomorSurat LIKE :query OR jenisSurat LIKE :query ORDER BY id DESC")
    fun searchSurat(query: String): Flow<List<Surat>>

    @Query("SELECT * FROM surat_arsip WHERE id = :id")
    suspend fun getSuratById(id: Int): Surat?

    @Query("SELECT * FROM surat_arsip WHERE qrVerifiedCode = :code")
    suspend fun getSuratByQrCode(code: String): Surat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurat(surat: Surat): Long

    @Query("DELETE FROM surat_arsip WHERE id = :id")
    suspend fun deleteSuratById(id: Int)

    @Query("SELECT COUNT(*) FROM surat_arsip WHERE tanggalBuat = :today")
    fun countToday(today: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM surat_arsip WHERE tanggalBuat LIKE :monthPattern")
    fun countMonth(monthPattern: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM surat_arsip WHERE tanggalBuat LIKE :yearPattern")
    fun countYear(yearPattern: String): Flow<Int>
}

@Dao
interface VillageConfigDao {
    @Query("SELECT * FROM village_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<VillageConfig?>

    @Query("SELECT * FROM village_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): VillageConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VillageConfig)
}
