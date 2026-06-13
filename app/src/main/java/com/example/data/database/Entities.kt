package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penduduk")
data class Penduduk(
    @PrimaryKey val nik: String,
    val noKk: String,
    val namaLengkap: String,
    val jenisKelamin: String, // "Laki-laki" | "Perempuan"
    val tempatLahir: String,
    val tanggalLahir: String, // String format e.g. "1995-08-17"
    val namaAyah: String,
    val namaIbu: String,
    val pendidikan: String,
    val pekerjaan: String,
    val statusPerkawinan: String, // "Belum Kawin" | "Kawin" | "Cerai Hidup" | "Cerai Mati"
    val statusHubunganKeluarga: String, // "Kepala Keluarga" | "Istri" | "Anak" | etc.
    val agama: String,
    val kewarganegaraan: String, // "WNI" | "WNA"
    val dusun: String,
    val rt: String,
    val rw: String
) {
    fun toFormattedAddress(): String {
        return "RT $rt / RW $rw, Dusun $dusun"
    }
}

@Entity(tableName = "surat_arsip")
data class Surat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomorSurat: String,
    val jenisSurat: String, // e.g., "Surat Keterangan Usaha", "N1", etc.
    val nikPenduduk: String,
    val namaPenduduk: String,
    val tanggalBuat: String, // e.g. "2026-06-13"
    val operator: String,
    val penandatanganNama: String,
    val penandatanganJabatan: String,
    val status: String, // "Draft" | "Disetujui" | "Ditandatangani"
    val qrVerifiedCode: String, // Unique UUID string for QR Code Verification
    val dataTambahanJson: String // Custom editable JSON string for fields specific to this letter type
)

@Entity(tableName = "village_config")
data class VillageConfig(
    @PrimaryKey val id: Int = 1, // Single-row configuration
    val namaDesa: String,
    val kecamatan: String,
    val kabupaten: String,
    val alamat: String,
    val telepon: String,
    val website: String,
    val kadesNama: String,
    val kadesNip: String,
    val sekdesNama: String,
    val formatNomorSurat: String
)
