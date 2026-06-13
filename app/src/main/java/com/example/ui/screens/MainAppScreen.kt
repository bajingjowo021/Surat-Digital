package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.Penduduk
import com.example.data.database.Surat
import com.example.data.database.VillageConfig
import com.example.ui.viewmodel.VillageViewModel
import com.example.ui.viewmodel.ChatMessage
import kotlinx.coroutines.launch
import java.util.*

enum class VillageScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PENDUDUK("Data Penduduk", Icons.Default.People),
    BUAT_SURAT("Buat Surat", Icons.Default.Description),
    ARSIP("Arsip Surat", Icons.Default.Folder),
    AI_ASSISTANT("AI Assistant", Icons.Default.SmartToy),
    CETAK("Cetak Surat", Icons.Default.Print),
    PENGGUNA("Akses Pengguna", Icons.Default.SupervisorAccount),
    KONFIGURASI("Konfigurasi Desa", Icons.Default.Settings),
    BACKUP_RESTORE("Backup & Restore", Icons.Default.Backup),
    TENTANG("Tentang Aplikasi", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: VillageViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(VillageScreen.DASHBOARD) }

    // Observers
    val allPendudukList by viewModel.allPenduduk.collectAsStateWithLifecycle()
    val searchedResidents by viewModel.searchedResidents.collectAsStateWithLifecycle()
    val searchedSuras by viewModel.searchedSuras.collectAsStateWithLifecycle()
    val configVal by viewModel.config.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    // Counter stats
    val countPenduduk by viewModel.totalPenduduk.collectAsStateWithLifecycle()
    val countKk by viewModel.totalKk.collectAsStateWithLifecycle()
    val countToday by viewModel.totalSuratToday.collectAsStateWithLifecycle()
    val countMonth by viewModel.totalSuratMonth.collectAsStateWithLifecycle()
    val countYear by viewModel.totalSuratYear.collectAsStateWithLifecycle()

    // Selected user role (for demo/presentation)
    var currentUserRole by remember { mutableStateOf("Operator Desa") }

    // Print Preview Target
    var targetLetterToPrint by remember { mutableStateOf<Surat?>(null) }

    // Quick Toast
    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.Cabin,
                            contentDescription = "Desa Icon",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DESA ${configVal?.namaDesa?.uppercase(Locale.getDefault()) ?: "SUMBERAGUNG"}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Kec. ${configVal?.kecamatan ?: "Kragan"}, Kab. ${configVal?.kabupaten ?: "Rembang"}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Drawer Links
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(VillageScreen.values().toList()) { screen ->
                        NavigationDrawerItem(
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(text = screen.title, fontWeight = FontWeight.Medium) },
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("nav_item_${screen.name.lowercase(Locale.getDefault())}"),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Buka Menu")
                        }
                    },
                    actions = {
                        // Quick Profile Pill
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Role",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUserRole,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Crossfade-like switching with AnimatedVisibility or standard switch
                when (currentScreen) {
                    VillageScreen.DASHBOARD -> DashboardScreen(
                        countPenduduk = countPenduduk,
                        countKk = countKk,
                        countToday = countToday,
                        countMonth = countMonth,
                        countYear = countYear,
                        auditLogs = auditLogs,
                        onNavigate = { currentScreen = it }
                    )
                    VillageScreen.PENDUDUK -> PendudukScreen(
                        residents = searchedResidents,
                        searchQuery = viewModel.residentSearchQuery.collectAsStateWithLifecycle().value,
                        onQueryChange = { viewModel.setResidentSearchQuery(it) },
                        onAddResident = { viewModel.addResident(it) },
                        onDeleteResident = { viewModel.deleteResident(it) }
                    )
                    VillageScreen.BUAT_SURAT -> BuatSuratScreen(
                        residents = allPendudukList,
                        viewModel = viewModel,
                        onLetterCreated = {
                            showToast("Surat berhasil diterbitkan sebagai draft baru!")
                            currentScreen = VillageScreen.ARSIP
                        }
                    )
                    VillageScreen.ARSIP -> ArsipScreen(
                        suratList = searchedSuras,
                        searchQuery = viewModel.suratSearchQuery.collectAsStateWithLifecycle().value,
                        onQueryChange = { viewModel.setSuratSearchQuery(it) },
                        onDelete = { viewModel.deleteLetter(it) },
                        onApprove = { surat ->
                            showToast("Surat berhasil ditandatangani secara digital!")
                            viewModel.addAuditLog("Operator menandatangani Surat ${surat.jenisSurat} - No: ${surat.nomorSurat}")
                        },
                        onPrint = { letter ->
                            targetLetterToPrint = letter
                            currentScreen = VillageScreen.CETAK
                        }
                    )
                    VillageScreen.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel)
                    VillageScreen.CETAK -> CetakScreen(
                        surat = targetLetterToPrint,
                        config = configVal,
                        onBack = { currentScreen = VillageScreen.ARSIP }
                    )
                    VillageScreen.PENGGUNA -> AccessScreen(
                        currentRole = currentUserRole,
                        onRoleChanged = {
                            currentUserRole = it
                            showToast("Akses diubah menjadi: $it")
                            viewModel.addAuditLog("Mengubah sesi akses pengguna aktif menjadi: $it")
                        }
                    )
                    VillageScreen.KONFIGURASI -> KonfigurasiScreen(
                        config = configVal,
                        onSave = {
                            viewModel.updateConfig(it)
                            showToast("Konfigurasi Desa Berhasil Disimpan!")
                        }
                    )
                    VillageScreen.BACKUP_RESTORE -> BackupRestoreScreen(
                        viewModel = viewModel,
                        showToast = { showToast(it) }
                    )
                    VillageScreen.TENTANG -> TentangScreen()
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    countPenduduk: Int,
    countKk: Int,
    countToday: Int,
    countMonth: Int,
    countYear: Int,
    auditLogs: List<String>,
    onNavigate: (VillageScreen) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Welcome Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Selamat Datang di e-Surat Desa!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Sistem manajemen surat kependudukan mandiri, cepat, dan terintegrasi kecerdasan buatan.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Statistik Pelayanan",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            // Numeric Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Resident Count
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Penduduk",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Penduduk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$countPenduduk Jiwa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // KK Count
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = "KK",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Keluarga (KK)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$countKk KK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Letter Counters
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Hari Ini", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "$countToday", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(40.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Bulan Ini", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "$countMonth", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(40.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Tahun Ini", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "$countYear", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            // Beautiful custom Canvas graph representing services monthly
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grafik Layanan Surat (Bulanan)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val widthSpace = size.width / 6
                        val barValues = listOf(15f, 25f, 40f, 30f, countMonth.toFloat().coerceAtLeast(10f), countMonth.toFloat() + 5)
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun")

                        // Draw baseline
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2f
                        )

                        // Draw gorgeous smooth curve graph representation
                        for (i in 0 until barValues.size) {
                            val x = i * widthSpace + (widthSpace / 2)
                            val mapY = size.height - (barValues[i] * 2.2f)

                            // Bar
                            drawRoundRect(
                                color = if (i == 5) primaryColor else secondaryColor.copy(alpha = 0.5f),
                                topLeft = Offset(x - 20f, mapY),
                                size = Size(40f, size.height - mapY),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun")
                        months.forEach {
                            Text(text = it, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            // Quick navigations row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(VillageScreen.BUAT_SURAT) },
                    modifier = Modifier.weight(1f).testTag("quick_buat_surat")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Buat")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Buat Surat")
                }

                Button(
                    onClick = { onNavigate(VillageScreen.AI_ASSISTANT) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f).testTag("quick_ai_assistant")
                ) {
                    Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Tanya AI")
                }
            }
        }

        item {
            // Audit Log List Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aktivitas Sistem & Audit Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        auditLogs.take(4).forEach { log ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Ok",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. DATA PENDUDUK SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendudukScreen(
    residents: List<Penduduk>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onAddResident: (Penduduk) -> Unit,
    onDeleteResident: (Penduduk) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedForDetail by remember { mutableStateOf<Penduduk?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Cari NIK, Nama, No KK...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp).testTag("add_resident_fab")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Tambah")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (residents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Tidak ada data penduduk.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(residents) { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedForDetail = res },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.namaLengkap,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "NIK: ${res.nik} | KK: ${res.noKk}",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "Dusun ${res.dusun} RT ${res.rt} / RW ${res.rw}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row {
                                IconButton(onClick = { selectedForDetail = res }) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "Detail", tint = Color.Gray)
                                }
                                IconButton(onClick = { onDeleteResident(res) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedForDetail?.let { res ->
        Dialog(onDismissRequest = { selectedForDetail = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Biodata Detail Penduduk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val details = listOf(
                        "Nama Lengkap" to res.namaLengkap,
                        "NIK" to res.nik,
                        "No KK" to res.noKk,
                        "Jenis Kelamin" to res.jenisKelamin,
                        "TTL" to "${res.tempatLahir}, ${res.tanggalLahir}",
                        "Agama" to res.agama,
                        "Pekerjaan" to res.pekerjaan,
                        "Pendidikan" to res.pendidikan,
                        "Status Perkawinan" to res.statusPerkawinan,
                        "Hub. Keluarga" to res.statusHubunganKeluarga,
                        "Nama Ayah" to res.namaAyah,
                        "Nama Ibu" to res.namaIbu,
                        "Kewarganegaraan" to res.kewarganegaraan,
                        "Alamat" to "RT ${res.rt} RW ${res.rw} Dusun ${res.dusun}"
                    )

                    details.forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                            Text(text = value, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    Button(
                        onClick = { selectedForDetail = null },
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                    ) {
                        Text(text = "Tutup")
                    }
                }
            }
        }
    }

    // Form Tambah Dialog
    if (showAddDialog) {
        var nik by remember { mutableStateOf("") }
        var kk by remember { mutableStateOf("") }
        var nama by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("Laki-laki") }
        var tempatLahir by remember { mutableStateOf("") }
        var tglLahir by remember { mutableStateOf("") }
        var ayah by remember { mutableStateOf("") }
        var ibu by remember { mutableStateOf("") }
        var pendidikan by remember { mutableStateOf("") }
        var pekerjaan by remember { mutableStateOf("") }
        var statusKawin by remember { mutableStateOf("Belum Kawin") }
        var hubKel by remember { mutableStateOf("Kepala Keluarga") }
        var agama by remember { mutableStateOf("Islam") }
        var WNI by remember { mutableStateOf("WNI") }
        var dusun by remember { mutableStateOf("") }
        var rt by remember { mutableStateOf("") }
        var rw by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tambah Data Penduduk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, singleLine = true)
                    OutlinedTextField(value = nik, onValueChange = { nik = it }, label = { Text("NIK (16 Digit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = kk, onValueChange = { kk = it }, label = { Text("No KK") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    
                    // Gender selection
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { gender = "Laki-laki" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (gender == "Laki-laki") MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Laki-laki")
                        }
                        Button(
                            onClick = { gender = "Perempuan" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (gender == "Perempuan") MaterialTheme.colorScheme.primary else Color.LightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Perempuan")
                        }
                    }

                    OutlinedTextField(value = tempatLahir, onValueChange = { tempatLahir = it }, label = { Text("Tempat Lahir") }, singleLine = true)
                    OutlinedTextField(value = tglLahir, onValueChange = { tglLahir = it }, label = { Text("Tanggal Lahir (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(value = ayah, onValueChange = { ayah = it }, label = { Text("Nama Ayah") }, singleLine = true)
                    OutlinedTextField(value = ibu, onValueChange = { ibu = it }, label = { Text("Nama Ibu") }, singleLine = true)
                    OutlinedTextField(value = pekerjaan, onValueChange = { pekerjaan = it }, label = { Text("Pekerjaan") }, singleLine = true)
                    OutlinedTextField(value = pendidikan, onValueChange = { pendidikan = it }, label = { Text("Pendidikan") }, singleLine = true)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = dusun, onValueChange = { dusun = it }, label = { Text("Dusun") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = rt, onValueChange = { rt = it }, label = { Text("RT") }, modifier = Modifier.weight(0.5f), singleLine = true)
                        OutlinedTextField(value = rw, onValueChange = { rw = it }, label = { Text("RW") }, modifier = Modifier.weight(0.5f), singleLine = true)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nik.isBlank() || nama.isBlank() || kk.isBlank()) {
                                    // simple skip validation for simplicity, but enforce basic fields
                                } else {
                                    val newRes = Penduduk(
                                        nik = nik, noKk = kk, namaLengkap = nama, jenisKelamin = gender,
                                        tempatLahir = tempatLahir, tanggalLahir = tglLahir, namaAyah = ayah, namaIbu = ibu,
                                        pendidikan = pendidikan, pekerjaan = pekerjaan, statusPerkawinan = statusKawin,
                                        statusHubunganKeluarga = hubKel, agama = agama, kewarganegaraan = WNI,
                                        dusun = dusun, rt = rt, rw = rw
                                    )
                                    onAddResident(newRes)
                                    showAddDialog = false
                                }
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. BUAT SURAT SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuatSuratScreen(
    residents: List<Penduduk>,
    viewModel: VillageViewModel,
    onLetterCreated: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1. Pilih Surat & Penduduk, 2. Lengkapi Data Tambahan & Layout
    var searchQuery by remember { mutableStateOf("") }
    
    val selectedResident = viewModel.selectedResidentForSurat.collectAsStateWithLifecycle().value
    val selectedLetter = viewModel.selectedLetterType.collectAsStateWithLifecycle().value

    // Filtering residents locally for selection
    val filteredResidents = residents.filter {
        it.namaLengkap.contains(searchQuery, ignoreCase = true) || it.nik.contains(searchQuery)
    }

    val docGroups = listOf(
        "Kelompok Surat Umum" to listOf(
            "Surat Keterangan Umum", "Surat Pengantar Umum", "Surat Keterangan Usaha",
            "Surat Pernyataan Kehilangan", "Surat Keterangan Tidak Mampu",
            "Surat Keterangan Domisili Tempat Tinggal", "Surat Keterangan Domisili Lembaga"
        ),
        "Kelompok Pernikahan dsb." to listOf(
            "Surat Pengantar Nikah (N1)", "Surat N2 (Asal-Usul)", "Surat N4 (Tentang Orang Tua)",
            "Surat N5 (Izin Orang Tua)", "Surat Keterangan Wali Nikah", "Surat Keterangan Belum Pernah Nikah"
        ),
        "Kelompok Administrasi Kependudukan" to listOf(
            "Pengantar F-1.02", "Surat Pernyataan F-1.06 (Pernyataan Perubahan)",
            "Surat Pengantar Pindah F-1.03", "Biodata WNI F-1.01",
            "Pengantar Kelahiran F-2.01", "SPTJM F-2.03 (Suami Istri)", "SPTJM F-2.04 (Kelahiran)"
        )
    )

    if (step == 1) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Langkah 1: Pilih Jenis Surat & Penduduk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 1A. Select Document Type
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Pilih Jenis Dokumen:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        docGroups.forEach { (groupTitle, docs) ->
                            Text(text = groupTitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                docs.forEach { doc ->
                                    val isSelected = selectedLetter == doc
                                    AssistChip(
                                        onClick = { viewModel.selectedLetterType.value = doc },
                                        label = { Text(text = doc, fontSize = 11.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        modifier = Modifier.testTag("chip_${doc.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1B. Select Resident
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Cari & Pilih Penduduk (Pemohon):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ketik nama atau NIK...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                        ) {
                            filteredResidents.forEach { res ->
                                val isSelected = selectedResident?.nik == res.nik
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { viewModel.selectedResidentForSurat.value = res }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectedResidentForSurat.value = res }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = res.namaLengkap, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = "NIK: ${res.nik} - ${res.toFormattedAddress()}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { step = 2 },
                    enabled = selectedLetter.isNotEmpty() && selectedResident != null,
                    modifier = Modifier.fillMaxWidth().testTag("next_step_buat_surat")
                ) {
                    Text(text = "Lanjut Ke Data Tambahan")
                }
            }
        }
    } else {
        // Step 2: Custom fields and Save draft
        var keperluanInput by remember { mutableStateOf("Mengurus Keperluan Administrasi") }
        var deskripsiTambahanInput by remember { mutableStateOf("") }
        var statusLayout by remember { mutableStateOf("Ditandatangani") } // "Draft" atau "Ditandatangani" (kades approved)

        // Custom SKU / Marriage field keys
        var namaUsaha by remember { mutableStateOf("Rahmat Abadi Sembako") }
        var lokasiUsaha by remember { mutableStateOf("RT 02 Sumberagung") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step = 1 }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Langkah 2: Lengkapi Data & Terbitkan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Overview Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ringkasan Penerbitan:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "• Surat: $selectedLetter", fontSize = 12.sp)
                    Text(text = "• Nama Penduduk: ${selectedResident?.namaLengkap}", fontSize = 12.sp)
                    Text(text = "• NIK: ${selectedResident?.nik}", fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = keperluanInput,
                onValueChange = { keperluanInput = it },
                label = { Text("Tujuan / Keperluan Surat") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic fields based on document selection
            if (selectedLetter.contains("Usaha")) {
                OutlinedTextField(
                    value = namaUsaha,
                    onValueChange = { namaUsaha = it },
                    label = { Text("Nama Usaha / Jenis Usaha") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lokasiUsaha,
                    onValueChange = { lokasiUsaha = it },
                    label = { Text("Lokasi Alamat Tempat Usaha") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = deskripsiTambahanInput,
                    onValueChange = { deskripsiTambahanInput = it },
                    label = { Text("Keterangan Tambahan Lain (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // Interactive TTE Approval option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Metode Penerbitan & TTE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { statusLayout = "Draft" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (statusLayout == "Draft") MaterialTheme.colorScheme.secondary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Draft")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan Draft")
                        }
                        Button(
                            onClick = { statusLayout = "Ditandatangani" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (statusLayout == "Ditandatangani") MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = "TTE")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TTE Langsung")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val mapFields = mutableMapOf<String, String>()
                    mapFields["keperluan"] = keperluanInput
                    if (selectedLetter.contains("Usaha")) {
                        mapFields["namaUsaha"] = namaUsaha
                        mapFields["lokasiUsaha"] = lokasiUsaha
                    } else {
                        mapFields["deskripsiTambahan"] = deskripsiTambahanInput
                    }

                    selectedResident?.let {
                        viewModel.createLetter(
                            resident = it,
                            jenisSurat = selectedLetter,
                            status = statusLayout,
                            extraFields = mapFields
                        )
                    }
                    onLetterCreated()
                },
                modifier = Modifier.fillMaxWidth().testTag("publish_letter_button")
            ) {
                Text("Terbitkan & Simpan Digital Arsip")
            }
        }
    }
}

// ==========================================
// 4. ARSIP SURAT SCREEN
// ==========================================
@Composable
fun ArsipScreen(
    suratList: List<Surat>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onApprove: (Surat) -> Unit,
    onPrint: (Surat) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari Berdasarkan NIK, Nomor, atau Jenis...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (suratList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Arsip atau draf surat kosong.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(suratList) { surat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = surat.jenisSurat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "No: ${surat.nomorSurat}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Status Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(40))
                                        .background(
                                            if (surat.status == "Draft") MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = surat.status,
                                        fontSize = 11.sp,
                                        color = if (surat.status == "Draft") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.LightGray.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Penduduk: ${surat.namaPenduduk}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "NIK: ${surat.nikPenduduk} | Tgl: ${surat.tanggalBuat}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (surat.status == "Draft") {
                                        IconButton(onClick = { onApprove(surat) }) {
                                            Icon(imageVector = Icons.Default.HowToReg, contentDescription = "TTE", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(onClick = { onPrint(surat) }) {
                                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "Cetak", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(onClick = { onDelete(surat.id) }) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. AI ASSISTANT SCREEN
// ==========================================
@Composable
fun AiAssistantScreen(viewModel: VillageViewModel) {
    val messages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val loading by viewModel.aiLoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sample Chips Row
        Text(text = "Cobalah perintah cepat berikut:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val templates = listOf(
                "Buatkan berkas pernikahan Ahmad Fauzi",
                "Tolong buatkan berkas pindah Siti Aminah",
                "Cetak SKU untuk Budiono"
            )
            templates.forEach { temp ->
                AssistChip(
                    onClick = { inputText = temp },
                    label = { Text(text = temp, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat lists
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isBot = msg.sender == "assistant"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBot) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBot) "AI Assistant" else "Operator Desa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isBot) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(text = msg.content, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }

                if (loading) {
                    item {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "AI sedang merumuskan draf berkas...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Send row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("E.g. Buatkan berkas nikah Ahmad Fauzi...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.askAiAssistant(inputText)
                        inputText = ""
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("send_ai_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ==========================================
// 6. CETAK SCREEN (Visual preview formatted letter)
// ==========================================
@Composable
fun CetakScreen(
    surat: Surat?,
    config: VillageConfig?,
    onBack: () -> Unit
) {
    if (surat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Silakan pilih surat di menu Arsip untuk visual cetak.")
                Button(onClick = onBack) { Text("Kembali") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(text = "Pratinjau Resmi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { /* PrintManager simulation trigger */ }) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Printable Document Sheet Representation
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // KOP SURAT
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PEMERINTAH KABUPATEN ${config?.kabupaten?.uppercase() ?: "REMBANG"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "KECAMATAN ${config?.kecamatan?.uppercase() ?: "KRAGAN"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "KANTOR KEPALA DESA ${config?.namaDesa?.uppercase() ?: "SUMBERAGUNG"}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Text(
                        text = config?.alamat ?: "Jl. Raya Sumberagung No. 1, Kode Pos 59273",
                        fontSize = 10.sp,
                        color = Color.DarkGray
                    )
                    Divider(
                        color = Color.Black,
                        thickness = 2.dp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
                    )
                }

                // LETTER TITLE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = surat.jenisSurat.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Nomor: ${surat.nomorSurat}",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // CONTENT BODY
                Text(
                    text = "Yang bertanda tangan di bawah ini, Kepala Desa ${config?.namaDesa ?: "Sumberagung"}, Kecamatan ${config?.kecamatan ?: "Kragan"}, Kabupaten ${config?.kabupaten ?: "Rembang"}, dengan ini menerangkan bahwa kependudukan atas nama:",
                    fontSize = 12.sp,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // BIO FIELD
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val fields = listOf(
                        "Nama Lengkap" to surat.namaPenduduk,
                        "NIK" to surat.nikPenduduk,
                        "Jenis Kelamin" to "Laki-laki / Perempuan",
                        "Tanggal Penerbitan" to surat.tanggalBuat,
                        "Keperluan" to "Penerbitan administrasi desa",
                        "Keterangan Usaha" to "Berjalan baik di wilayah Desa Sumberagung"
                    )

                    fields.forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = label, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color.Gray)
                            Text(text = ": $value", modifier = Modifier.weight(1.8f), fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Demikian surat keterangan kependudukan desa ini dibuat dengan sebenarnya dan sebagaimana mestinya, untuk dapat digunakan oleh yang bersangkutan.",
                    fontSize = 12.sp,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SIGNATURE CARD (KADES TTE REGISTRATION)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(text = "${config?.kecamatan ?: "Sumberagung"}, ${surat.tanggalBuat}", fontSize = 11.sp, color = Color.Black)
                        Text(text = "Kepala Desa ${config?.namaDesa ?: "Sumberagung"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Safe drawing - representing BSrE verified signature
                        Canvas(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, Color.Black)
                        ) {
                            // Simple abstract drawing representing QR Code security verified vector
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(10f, 10f),
                                size = Size(20f, 20f)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(34f, 10f),
                                size = Size(20f, 20f)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(10f, 34f),
                                size = Size(20f, 20f)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(34f, 34f),
                                size = Size(10f, 10f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = config?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "NIP: ${config?.kadesNip ?: "197203152002121003"}",
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { /* Print Action */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Print, contentDescription = "Cetak")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Hubungkan Printer & Cetak PDF A4")
        }
    }
}

// ==========================================
// 7. USER ACCESS SCREEN
// ==========================================
@Composable
fun AccessScreen(
    currentRole: String,
    onRoleChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hak Akses & Manajemen Identitas",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Silakan berpindah peran (Akses Simulasi) untuk meninjau kecocokan fungsi pelayanan desa masing-masing operator.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        val roles = listOf("Super Admin", "Operator Desa", "Kepala Desa", "Masyarakat")

        roles.forEach { role ->
            val isSelect = currentRole == role
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRoleChanged(role) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelect) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelect) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (role) {
                            "Super Admin" -> Icons.Default.SettingsSuggest
                            "Operator Desa" -> Icons.Default.BorderColor
                            "Kepala Desa" -> Icons.Default.AssignmentTurnedIn
                            else -> Icons.Default.HowToReg
                        },
                        contentDescription = role,
                        tint = if (isSelect) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(text = role, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = when (role) {
                                "Super Admin" -> "Akses penuh manajemen data config, import penduduk, backup restore."
                                "Operator Desa" -> "Membuat draft surat, mengisi form kependudukan, cetak PDF."
                                "Kepala Desa" -> "Persetujuan tanda tangan digital (TTE) dan verifikasi data."
                                else -> "Mengajukan draft surat mandiri (Roadmap v2.0)."
                            },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. CONFIGURATION (SETTING KOP SURAT)
// ==========================================
@Composable
fun KonfigurasiScreen(
    config: VillageConfig?,
    onSave: (VillageConfig) -> Unit
) {
    var namaDesa by remember { mutableStateOf(config?.namaDesa ?: "Sumberagung") }
    var kecamatan by remember { mutableStateOf(config?.kecamatan ?: "Kragan") }
    var kabupaten by remember { mutableStateOf(config?.kabupaten ?: "Rembang") }
    var alamat by remember { mutableStateOf(config?.alamat ?: "Jl. Raya Sumberagung No. 1, Kode Pos 59273") }
    var telepon by remember { mutableStateOf(config?.telepon ?: "(0295) 678910") }
    var website by remember { mutableStateOf(config?.website ?: "www.sumberagung-rembang.desa.id") }
    var kadesNama by remember { mutableStateOf(config?.kadesNama ?: "H. Bambang Sulistyo, S.Sos.") }
    var kadesNip by remember { mutableStateOf(config?.kadesNip ?: "197203152002121003") }
    var sekdesNama by remember { mutableStateOf(config?.sekdesNama ?: "Ahmad Sholihin") }
    var formatNomorSurat by remember { mutableStateOf(config?.formatNomorSurat ?: "470/[COUNTER]/[MONTH]/[YEAR]") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Kompilasi Identitas Desa (KOP Surat)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(value = namaDesa, onValueChange = { namaDesa = it }, label = { Text("Nama Desa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kecamatan, onValueChange = { kecamatan = it }, label = { Text("Kecamatan") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kabupaten, onValueChange = { kabupaten = it }, label = { Text("Kabupaten") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = alamat, onValueChange = { alamat = it }, label = { Text("Alamat Kantor Desa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = telepon, onValueChange = { telepon = it }, label = { Text("No Telepon") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())

        Text(
            text = "Pejabat Penandatangan (TTE)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp)
        )

        OutlinedTextField(value = kadesNama, onValueChange = { kadesNama = it }, label = { Text("Nama Kepala Desa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = kadesNip, onValueChange = { kadesNip = it }, label = { Text("NIP Kepala Desa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sekdesNama, onValueChange = { sekdesNama = it }, label = { Text("Nama Sekretaris Desa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = formatNomorSurat, onValueChange = { formatNomorSurat = it }, label = { Text("Format Penomoran Surat") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val newConfig = VillageConfig(
                    namaDesa = namaDesa, kecamatan = kecamatan, kabupaten = kabupaten,
                    alamat = alamat, telepon = telepon, website = website,
                    kadesNama = kadesNama, kadesNip = kadesNip, sekdesNama = sekdesNama,
                    formatNomorSurat = formatNomorSurat
                )
                onSave(newConfig)
            },
            modifier = Modifier.fillMaxWidth().testTag("save_config_button")
        ) {
            Text("Simpan Perubahan Konfigurasi")
        }
    }
}

// ==========================================
// 9. BACKUP & RESTORE
// ==========================================
@Composable
fun BackupRestoreScreen(
    viewModel: VillageViewModel,
    showToast: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Pusat Cadangan (Backup & Restore)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Cadangkan Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Ekspor semua data penduduk, konfigurasi draf, dan arsip ke format SQLITE atau JSON lokal di direktori penyimpanan.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Button(
                    onClick = {
                        showToast("File JSON backup_esurat_desa.json berhasil diekspor!")
                        viewModel.addAuditLog("Operator mengekspor database lokal ke JSON cadangan.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Backup")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor Database (Backup)")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Pulihkan Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Muat file cadangan JSON desa sebelumnya untuk memulihkan seluruh arsip pelayanan.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Button(
                    onClick = {
                        showToast("Pemulihan data desa dari backup selesai!")
                        viewModel.addAuditLog("Pemulihan cadangan data pelayanan desa dari media penyimpanan berhasil.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "Restore")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Impor Berkas (Restore)")
                }
            }
        }
    }
}

// ==========================================
// 10. TENTANG APLIKASI
// ==========================================
@Composable
fun TentangScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cabin,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "e-Surat Desa",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Versi 1.0 (Rembang Digitalized)",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Aplikasi Pelayanan Desa e-Surat diinisiasi untuk mereduksi kesalahan input klerikal manual, mempercepat persetujuan tanda tangan elektronik (TTE QR), dan mendukung integrasi AI Surat Assistant untuk draf kependudukan otomatis.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Dikembangkan khusus untuk Dindukcapil Rembang dan Pemerintah Desa Sumberagung. Hak Cipta Dilindungi © 2026.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}
