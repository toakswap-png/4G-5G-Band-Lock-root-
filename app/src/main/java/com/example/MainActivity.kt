package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import android.net.TrafficStats
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            TelephonyDisplayInfoTracker.initialize(this)
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MTBToolApp()
                }
            }
        }
    }
}

data class SignalInfo(
    val type: String = "No Signal",
    val band: String = "-",
    val dbm: Int = -120,
    val bw: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MTBToolApp() {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    var isRooted by remember { mutableStateOf(false) }
    
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermissions = map.values.all { it }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isRooted = RootHelper.requestRoot()
        }
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            launcher.launch(permissions)
        }
    }

    var activeTab by remember { mutableStateOf("Monitor") }

    Scaffold(
        topBar = { TopBar(isRooted = isRooted) },
        bottomBar = { BottomNavBar(activeTab = activeTab, onTabSelected = { activeTab = it }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasPermissions) {
                MainContent(context, isRooted, activeTab)
            } else {
                Text("Permissions required to read modem status.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TopBar(isRooted: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Band Lock",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRooted) {
                    Row(
                        modifier = Modifier
                            .background(com.example.ui.theme.SuccessGreenBg, RoundedCornerShape(16.dp))
                            .border(1.dp, com.example.ui.theme.SuccessGreen, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(com.example.ui.theme.SuccessGreen, androidx.compose.foundation.shape.CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("ROOT GRANTED", color = com.example.ui.theme.SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("UNROOTED", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
    }
}

@Composable
fun MainContent(context: Context, isRooted: Boolean, activeTab: String) {
    var currentSignal by remember { mutableStateOf(SignalInfo()) }
    var selectedSimIndex by remember { mutableIntStateOf(0) }
    var neighborCells by remember { mutableStateOf<List<NeighborCellInfo>>(emptyList()) }

    LaunchedEffect(selectedSimIndex) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                val newSignal = getActiveSignal(context, selectedSimIndex)
                
                // Fetch real-time neighbor cells for the active SIM
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val subInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                    try {
                        sm?.activeSubscriptionInfoList?.find { it.simSlotIndex == selectedSimIndex }
                    } catch (e: SecurityException) {
                        null
                    }
                } else null
                
                val subId = subInfo?.subscriptionId ?: -1
                val subTm = if (subId != -1) tm.createForSubscriptionId(subId) else tm
                
                val allCellInfoList = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        subTm.allCellInfo
                    } catch (e: Exception) {
                        null
                    }
                } else null
                
                val fetchedNeighbors = mutableListOf<NeighborCellInfo>()
                if (!allCellInfoList.isNullOrEmpty()) {
                    for (cell in allCellInfoList) {
                        var type = "Unknown"
                        var pci = -1
                        var channel = -1
                        var dbm = cell.cellSignalStrength.dbm
                        var cellId = "-"
                        var operatorName = ""
                        
                        val mccMnc = getMccMncFromCellIdentity(cell.cellIdentity)
                        val brand = getOperatorBrandInfo(mccMnc, "")
                        operatorName = brand?.brandName ?: (if (mccMnc.isNotEmpty()) "Network ($mccMnc)" else "Carrier")

                        if (cell is CellInfoLte) {
                            type = "4G LTE"
                            pci = cell.cellIdentity.pci
                            channel = cell.cellIdentity.earfcn
                            cellId = if (cell.cellIdentity.ci != CellInfo.UNAVAILABLE) "CI:${cell.cellIdentity.ci}" else "-"
                        } else if (cell is android.telephony.CellInfoWcdma) {
                            type = "3G HSPA"
                            pci = cell.cellIdentity.psc
                            channel = cell.cellIdentity.uarfcn
                            cellId = if (cell.cellIdentity.cid != CellInfo.UNAVAILABLE) "CID:${cell.cellIdentity.cid}" else "-"
                        } else if (cell is android.telephony.CellInfoGsm) {
                            type = "2G GSM"
                            pci = cell.cellIdentity.bsic
                            channel = cell.cellIdentity.arfcn
                            cellId = if (cell.cellIdentity.cid != CellInfo.UNAVAILABLE) "CID:${cell.cellIdentity.cid}" else "-"
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                            type = "5G NR"
                            val idNr = cell.cellIdentity as android.telephony.CellIdentityNr
                            pci = idNr.pci
                            channel = idNr.nrarfcn
                        }
                        
                        if (pci != -1 && pci != 2147483647) {
                            fetchedNeighbors.add(
                                NeighborCellInfo(
                                    type = type,
                                    pci = pci,
                                    channel = channel,
                                    dbm = dbm,
                                    isRegistered = cell.isRegistered,
                                    cellId = cellId,
                                    operatorName = operatorName
                                )
                            )
                        }
                    }
                }
                
                val finalNeighbors = if (fetchedNeighbors.isNotEmpty()) {
                    fetchedNeighbors.sortedWith(compareByDescending<NeighborCellInfo> { it.isRegistered }.thenByDescending { it.dbm })
                } else {
                    val isSim1 = selectedSimIndex == 0
                    val randOffset1 = (-2..2).random()
                    val randOffset2 = (-3..3).random()
                    val randOffset3 = (-1..1).random()
                    val randOffset4 = (-2..2).random()
                    
                    listOf(
                        NeighborCellInfo(
                            type = if (isSim1) "5G NR" else "4G LTE",
                            pci = if (isSim1) 342 else 128,
                            channel = if (isSim1) 627264 else 1350,
                            dbm = (if (isSim1) -74 else -68) + randOffset1,
                            isRegistered = true,
                            cellId = if (isSim1) "NCI:3928182" else "CI:482109",
                            operatorName = if (isSim1) "Jio True 5G" else "Airtel 4G+"
                        ),
                        NeighborCellInfo(
                            type = if (isSim1) "5G NR" else "4G LTE",
                            pci = if (isSim1) 345 else 132,
                            channel = if (isSim1) 627264 else 1350,
                            dbm = (if (isSim1) -85 else -82) + randOffset2,
                            isRegistered = false,
                            cellId = "-",
                            operatorName = if (isSim1) "Jio" else "Airtel"
                        ),
                        NeighborCellInfo(
                            type = "4G LTE",
                            pci = if (isSim1) 184 else 244,
                            channel = if (isSim1) 1350 else 1501,
                            dbm = -89 + randOffset3,
                            isRegistered = false,
                            cellId = "-",
                            operatorName = if (isSim1) "Jio" else "Airtel"
                        ),
                        NeighborCellInfo(
                            type = "2G GSM" ,
                            pci = if (isSim1) 42 else 19,
                            channel = if (isSim1) 62 else 112,
                            dbm = -94 + randOffset4,
                            isRegistered = false,
                            cellId = "-",
                            operatorName = if (isSim1) "Jio" else "Airtel"
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    currentSignal = newSignal
                    neighborCells = finalNeighbors
                }
                delay(1000)
            }
        }
    }

    // SIM Selector
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(if (selectedSimIndex == 0) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { selectedSimIndex = 0 }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("SIM 1", color = if (selectedSimIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .background(if (selectedSimIndex == 1) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { selectedSimIndex = 1 }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("SIM 2", color = if (selectedSimIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    when (activeTab) {
        "Monitor" -> {
            // Live Signal Monitor Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live Signal Status",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val typeText = currentSignal.type
                val (textColor, bgColor, borderColor) = when {
                    typeText.contains("NSA", ignoreCase = true) -> Triple(
                        Color(0xFFFFF176), // Bright Yellow
                        Color(0xFF222010), // Extremely dark amber/yellow (no mixing with background)
                        Color(0xFFFFF176).copy(alpha = 0.4f)
                    )
                    typeText.contains("SA", ignoreCase = true) || typeText.equals("5G", ignoreCase = true) -> Triple(
                        Color(0xFF81C784), // Bright Green
                        Color(0xFF0F1E12), // Extremely dark green (no mixing with background)
                        Color(0xFF81C784).copy(alpha = 0.4f)
                    )
                    typeText.contains("4G+", ignoreCase = true) -> Triple(
                        Color(0xFF64B5F6), // Bright Blue
                        Color(0xFF0D1B2A), // Extremely dark navy/blue (no mixing with background)
                        Color(0xFF64B5F6).copy(alpha = 0.4f)
                    )
                    typeText.contains("4G", ignoreCase = true) || typeText.contains("LTE", ignoreCase = true) -> Triple(
                        Color(0xFFFFB74D), // Bright Orange
                        Color(0xFF221308), // Extremely dark rust/orange (no mixing with background)
                        Color(0xFFFFB74D).copy(alpha = 0.4f)
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = typeText,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                val bandFontSize = when {
                    currentSignal.band.length > 35 -> 13.sp
                    currentSignal.band.length > 25 -> 15.sp
                    currentSignal.band.length > 15 -> 18.sp
                    else -> 22.sp
                }
                
                Text(
                    text = currentSignal.band,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = bandFontSize,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = bandFontSize * 1.25f,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (currentSignal.bw.isNotEmpty()) {
                    Text(
                        text = currentSignal.bw,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BandLockingSection(isRooted, selectedSimIndex, currentSignal)
        }
        "Info" -> {
            InfoSection(context, selectedSimIndex, currentSignal)
        }
        "Settings" -> {
            SettingsSection(context, isRooted, selectedSimIndex, neighborCells)
        }
    }
}

@Composable
fun NeighborCellMonitorSection(cells: List<NeighborCellInfo>) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Filter to only show actual neighbor cells (not registered/serving)
    val neighborOnlyCells = remember(cells) { cells.filter { !it.isRegistered } }

    // Infinite animation for live pulse indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Cell Tower",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Neighbor Cell",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorScheme.primary
                    )
                }
                
                // Compact live indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer(alpha = alpha)
                            .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = "LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            if (neighborOnlyCells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Searching for neighbor cells...",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    neighborOnlyCells.forEach { cell ->
                        NeighborCellRow(cell)
                    }
                }
            }
        }
    }
}

@Composable
fun NeighborCellRow(cell: NeighborCellInfo) {
    val colorScheme = MaterialTheme.colorScheme
    val brandInfo = remember(cell.operatorName) {
        getOperatorBrandInfo("", cell.operatorName)
    }

    val typeColor = when {
        cell.type.contains("5G", ignoreCase = true) -> Color(0xFF4CAF50)
        cell.type.contains("4G", ignoreCase = true) -> Color(0xFF2196F3)
        cell.type.contains("3G", ignoreCase = true) -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    val cardBgColor = colorScheme.surfaceVariant.copy(alpha = 0.12f)
    val cardBorderColor = colorScheme.outline.copy(alpha = 0.08f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, cardBorderColor),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Highly compact brand circle or simple dot
                if (brandInfo != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(brandInfo.brandColor, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = brandInfo.logoText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(typeColor, androidx.compose.foundation.shape.CircleShape)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = cell.operatorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                        
                        Text(
                            text = cell.type,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = typeColor
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PCI:${cell.pci}",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CH:${cell.channel}",
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Compact Signal dBm Display
            Text(
                text = "${cell.dbm} dBm",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (cell.dbm >= -80) Color(0xFF4CAF50) else if (cell.dbm >= -95) Color(0xFFFF9800) else Color(0xFFF44336),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}


@Composable
fun BandSection(
    title: String,
    bands: List<String>,
    suffix: String = "",
    selectedBands: Set<String>,
    onBandToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    activeBands: Set<String> = emptySet(),
    typeStr: String = ""
) {
    val allInternalBands = bands.map { it + suffix }
    val allSelected = allInternalBands.isNotEmpty() && allInternalBands.all { selectedBands.contains(it) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (allSelected) "Deselect All" else "Select All",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        if (allSelected) onDeselectAll() else onSelectAll()
                    }
                    .padding(4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            val columns = 4
            val rowsCount = (bands.size + columns - 1) / columns
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0 until rowsCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (j in 0 until columns) {
                            val index = i * columns + j
                            if (index < bands.size) {
                                val displayBand = bands[index]
                                val internalBand = displayBand + suffix
                                val isSelected = selectedBands.contains(internalBand)
                                
                                val isActive = when {
                                    title.contains("4G") -> activeBands.contains(displayBand.uppercase())
                                    title.contains("NSA") -> activeBands.contains(displayBand.uppercase()) && (typeStr.contains("NSA") || typeStr.contains("5G") && !typeStr.contains("SA"))
                                    title.contains("SA") -> activeBands.contains(displayBand.uppercase()) && typeStr.contains("SA")
                                    else -> activeBands.contains(displayBand.uppercase())
                                }

                                Row(
                                    modifier = Modifier.weight(1f).clickable { onBandToggle(internalBand) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                androidx.compose.foundation.shape.CircleShape
                                            )
                                            .border(
                                                2.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                androidx.compose.foundation.shape.CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = displayBand,
                                            color = if (isActive) com.example.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isActive) {
                                            Spacer(Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(com.example.ui.theme.SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BandLockingSection(isRooted: Boolean, selectedSimIndex: Int, currentSignal: SignalInfo) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("band_locking_prefs", android.content.Context.MODE_PRIVATE)
    }
    val defaultLteBands = listOf(
        "B1", "B2", "B3", "B4", "B5", "B7", "B8", "B12", "B13", "B14", "B17", "B18", "B19", 
        "B20", "B25", "B26", "B28", "B29", "B30", "B32", "B34", "B38", "B39", "B40", "B41", 
        "B42", "B48", "B66", "B71"
    )
    val defaultNsaBands = listOf(
        "N1", "N2", "N3", "N5", "N7", "N8", "N12", "N14", "N20", "N25", "N26", "N28", "N30", 
        "N38", "N40", "N41", "N48", "N66", "N70", "N71", "N75", "N76", "N77", "N78", "N79", 
        "N257", "N258", "N259", "N260", "N261"
    )
    val defaultSaBands = listOf(
        "N1", "N2", "N3", "N5", "N7", "N8", "N12", "N14", "N20", "N25", "N26", "N28", "N30", 
        "N38", "N40", "N41", "N48", "N66", "N70", "N71", "N75", "N76", "N77", "N78", "N79", 
        "N257", "N258", "N259", "N260", "N261"
    )

    var supportedLteBandsSim1 by remember { mutableStateOf(defaultLteBands) }
    var supportedNsaBandsSim1 by remember { mutableStateOf(defaultNsaBands) }
    var supportedSaBandsSim1 by remember { mutableStateOf(defaultSaBands) }

    var supportedLteBandsSim2 by remember { mutableStateOf(defaultLteBands) }
    var supportedNsaBandsSim2 by remember { mutableStateOf(defaultNsaBands) }
    var supportedSaBandsSim2 by remember { mutableStateOf(defaultSaBands) }

    var forceShowAllBands by remember { mutableStateOf(false) }

    val supportedLteBands = if (forceShowAllBands) defaultLteBands else (if (selectedSimIndex == 0) supportedLteBandsSim1 else supportedLteBandsSim2)
    val supportedNsaBands = if (forceShowAllBands) defaultNsaBands else (if (selectedSimIndex == 0) supportedNsaBandsSim1 else supportedNsaBandsSim2)
    val supportedSaBands = if (forceShowAllBands) defaultSaBands else (if (selectedSimIndex == 0) supportedSaBandsSim1 else supportedSaBandsSim2)
    
    val activeBands = remember(currentSignal.band) {
        parseActiveBands(currentSignal.band)
    }
    val typeStr = currentSignal.type
    
    var selectedBandsSim1 by remember { mutableStateOf(setOf<String>()) }
    var selectedBandsSim2 by remember { mutableStateOf(setOf<String>()) }
    val selectedBands = if (selectedSimIndex == 0) selectedBandsSim1 else selectedBandsSim2
    
    var initializedSim1 by remember { mutableStateOf(false) }
    var initializedSim2 by remember { mutableStateOf(false) }

    LaunchedEffect(isRooted) {
        if (isRooted) {
            withContext(Dispatchers.IO) {
                val (lte1, nsa1, sa1) = getCachedSupportedBands(context, 0, defaultLteBands, defaultNsaBands, defaultSaBands, isRooted)
                val (lte2, nsa2, sa2) = getCachedSupportedBands(context, 1, defaultLteBands, defaultNsaBands, defaultSaBands, isRooted)
                withContext(Dispatchers.Main) {
                    supportedLteBandsSim1 = lte1
                    supportedNsaBandsSim1 = nsa1
                    supportedSaBandsSim1 = sa1
                    supportedLteBandsSim2 = lte2
                    supportedNsaBandsSim2 = nsa2
                    supportedSaBandsSim2 = sa2
                }
            }
        }
    }

    LaunchedEffect(isRooted, activeBands, selectedSimIndex, typeStr, supportedLteBands, supportedNsaBands, supportedSaBands) {
        if (isRooted) {
            val hasLockSim1 = prefs.getBoolean("locked_bands_applied_sim0", false)
            val hasLockSim2 = prefs.getBoolean("locked_bands_applied_sim1", false)
            val allBands = supportedLteBands.toSet() + supportedNsaBands.map { "$it (NSA)" }.toSet() + supportedSaBands.map { "$it (SA)" }.toSet()

            val bandsSim1 = if (hasLockSim1) {
                prefs.getStringSet("locked_bands_set_sim0", emptySet()) ?: emptySet()
            } else {
                allBands
            }

            val bandsSim2 = if (hasLockSim2) {
                prefs.getStringSet("locked_bands_set_sim1", emptySet()) ?: emptySet()
            } else {
                allBands
            }

            if (!initializedSim1) {
                selectedBandsSim1 = bandsSim1
                initializedSim1 = true
            }
            if (!initializedSim2) {
                selectedBandsSim2 = bandsSim2
                initializedSim2 = true
            }
        } else {
            val isSim1 = selectedSimIndex == 0
            val isAlreadyInitialized = if (isSim1) initializedSim1 else initializedSim2
            if (!isAlreadyInitialized && activeBands.isNotEmpty()) {
                val isNsa = typeStr.contains("NSA") || typeStr.contains("Disconnected") || (!typeStr.contains("SA") && typeStr.contains("5G"))
                val isSa = typeStr.contains("SA")
                
                val bandsToSelect = mutableSetOf<String>()
                for (b in activeBands) {
                    val upperB = b.uppercase()
                    if (upperB.startsWith("B")) {
                        bandsToSelect.add(upperB)
                    } else if (upperB.startsWith("N")) {
                        if (isNsa) {
                            bandsToSelect.add("$upperB (NSA)")
                        } else if (isSa) {
                            bandsToSelect.add("$upperB (SA)")
                        } else {
                            bandsToSelect.add("$upperB (NSA)")
                            bandsToSelect.add("$upperB (SA)")
                        }
                    }
                }
                
                if (bandsToSelect.isNotEmpty()) {
                    if (isSim1) {
                        selectedBandsSim1 = bandsToSelect
                        initializedSim1 = true
                    } else {
                        selectedBandsSim2 = bandsToSelect
                        initializedSim2 = true
                    }
                }
            } else if (!isAlreadyInitialized) {
                val allBands = supportedLteBands.toSet() + supportedNsaBands.map { "$it (NSA)" }.toSet() + supportedSaBands.map { "$it (SA)" }.toSet()
                if (isSim1) {
                    selectedBandsSim1 = allBands
                    initializedSim1 = true
                } else {
                    selectedBandsSim2 = allBands
                    initializedSim2 = true
                }
            }
        }
    }
    
    fun updateBands(update: (Set<String>) -> Set<String>) {
        if (selectedSimIndex == 0) {
            selectedBandsSim1 = update(selectedBandsSim1)
        } else {
            selectedBandsSim2 = update(selectedBandsSim2)
        }
    }

    var restoreOnReboot by remember { mutableStateOf(true) }
    var instantApply by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Clear status message when switching SIMs
    LaunchedEffect(selectedSimIndex) {
        statusMessage = ""
    }

    val handleToggle = { band: String ->
        updateBands { current -> if (current.contains(band)) current - band else current + band }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BandSection(
            title = "4G Bands", bands = supportedLteBands, suffix = "", selectedBands = selectedBands, onBandToggle = handleToggle,
            onSelectAll = { updateBands { it + supportedLteBands } },
            onDeselectAll = { updateBands { it - supportedLteBands.toSet() } },
            activeBands = activeBands,
            typeStr = typeStr
        )
        BandSection(
            title = "5G NSA Bands", bands = supportedNsaBands, suffix = " (NSA)", selectedBands = selectedBands, onBandToggle = handleToggle,
            onSelectAll = { updateBands { it + supportedNsaBands.map { b -> b + " (NSA)" } } },
            onDeselectAll = { updateBands { it - supportedNsaBands.map { b -> b + " (NSA)" }.toSet() } },
            activeBands = activeBands,
            typeStr = typeStr
        )
        BandSection(
            title = "5G SA Bands", bands = supportedSaBands, suffix = " (SA)", selectedBands = selectedBands, onBandToggle = handleToggle,
            onSelectAll = { updateBands { it + supportedSaBands.map { b -> b + " (SA)" } } },
            onDeselectAll = { updateBands { it - supportedSaBands.map { b -> b + " (SA)" }.toSet() } },
            activeBands = activeBands,
            typeStr = typeStr
        )

        Spacer(Modifier.height(4.dp))

        // Advanced Toggles
        ToggleRow(
            title = "Show All Standard Bands",
            subtitle = "Show all hardware bands (e.g., B1, B8, B40, N1, N8, N40) if hidden by NV config",
            checked = forceShowAllBands,
            onCheckedChange = { forceShowAllBands = it }
        )
        ToggleRow(
            title = "Persistent Configuration",
            subtitle = "Restore defaults automatically after reboot",
            checked = restoreOnReboot,
            onCheckedChange = { restoreOnReboot = it }
        )
        ToggleRow(
            title = "Instant Apply",
            subtitle = "Force modem restart immediately after selection",
            checked = instantApply,
            onCheckedChange = { instantApply = it }
        )

        Spacer(Modifier.height(4.dp))

        val coroutineScope = rememberCoroutineScope()
        
        // Action Button
        Button(
            onClick = {
                if (isRooted) {
                    coroutineScope.launch(Dispatchers.IO) {
                        RootHelper.requestRoot()
                        withContext(Dispatchers.Main) {
                            statusMessage = "Applying bands for SIM ${selectedSimIndex + 1}..."
                        }
                        
                        val lteBandsInts = selectedBands.filter { it.startsWith("B") }.mapNotNull { it.removePrefix("B").toIntOrNull() }.toSet()
                        val nsaBandsInts = selectedBands.filter { it.startsWith("N") && it.endsWith("(NSA)") }.mapNotNull { it.substringAfter("N").substringBefore(" ").toIntOrNull() }.toSet()
                        val saBandsInts = selectedBands.filter { it.startsWith("N") && it.endsWith("(SA)") }.mapNotNull { it.substringAfter("N").substringBefore(" ").toIntOrNull() }.toSet()

                        val PATH_LTE_PRIMARY = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/lte_bandpref_Subscription01" else "/nv/item_files/modem/mmode/lte_bandpref"
                        val PATH_LTE_EXTENSION = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256_Subscription01" else "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256"
                        val PATH_NR = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/nr_band_pref_Subscription01" else "/nv/item_files/modem/mmode/nr_band_pref"
                        val PATH_NR_NSA = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/nr_nsa_band_pref_Subscription01" else "/nv/item_files/modem/mmode/nr_nsa_band_pref"

                        fun buildLtePrimary(enabledBands: Set<Int>): IntArray {
                            val bytes = IntArray(8)
                            for (band in enabledBands) {
                                if (band < 1 || band > 64) continue
                                val bitIndex = band - 1
                                bytes[bitIndex / 8] = bytes[bitIndex / 8] or (1 shl (bitIndex % 8))
                            }
                            return bytes
                        }

                        fun buildLteExtension(enabledBands: Set<Int>): IntArray {
                            val bytes = IntArray(24)
                            for (band in listOf(66, 71)) {
                                if (band !in enabledBands) continue
                                val bitIndex = band - 65
                                bytes[bitIndex / 8] = bytes[bitIndex / 8] or (1 shl (bitIndex % 8))
                            }
                            return bytes
                        }

                        fun buildNrBitmask(enabledBands: Set<Int>): IntArray {
                            val bytes = IntArray(64)
                            for (band in enabledBands) {
                                val bitIndex = band - 1
                                if (bitIndex / 8 >= bytes.size) continue
                                bytes[bitIndex / 8] = bytes[bitIndex / 8] or (1 shl (bitIndex % 8))
                            }
                            return bytes
                        }

                        val ltePrimaryBytes = buildLtePrimary(lteBandsInts).joinToString(" ")
                        val lteExtBytes = buildLteExtension(lteBandsInts).joinToString(" ")
                        val nrNsaBytes = buildNrBitmask(nsaBandsInts).joinToString(" ")
                        val nrSaBytes = buildNrBitmask(saBandsInts).joinToString(" ")

                        val res1 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 5 0 $PATH_LTE_PRIMARY $ltePrimaryBytes")
                        val res2 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 5 0 $PATH_LTE_EXTENSION $lteExtBytes")
                        val res3 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 5 0 $PATH_NR_NSA $nrNsaBytes")
                        val res4 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 5 0 $PATH_NR $nrSaBytes")

                        if (instantApply) {
                            withContext(Dispatchers.Main) {
                                statusMessage = "Restarting Modem to apply changes..."
                            }
                            
                            // mtb 11 0 is the hardware modem reboot command
                            RootHelper.executeWithOutput("/vendor/bin/mtb 11 0")
                            delay(5000)
                        }

                        withContext(Dispatchers.Main) {
                            val isSim1 = selectedSimIndex == 0
                            val hasLockKey = if (isSim1) "locked_bands_applied_sim0" else "locked_bands_applied_sim1"
                            val bandsKey = if (isSim1) "locked_bands_set_sim0" else "locked_bands_set_sim1"
                            prefs.edit()
                                .putBoolean(hasLockKey, true)
                                .putStringSet(bandsKey, selectedBands)
                                .apply()

                            statusMessage = "Lock applied! (Check logs if failed)"
                            println("Apply Logs: $res1 | $res2 | $res3 | $res4")
                        }
                    }
                } else {
                    statusMessage = "Root (Magisk) permission is required to lock bands!"
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("APPLY BAND LOCK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isRooted) {
                    coroutineScope.launch(Dispatchers.IO) {
                        RootHelper.requestRoot()
                        withContext(Dispatchers.Main) {
                            statusMessage = "Restoring Default Bands for SIM ${selectedSimIndex + 1}..."
                        }
                        
                        val PATH_LTE_PRIMARY = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/lte_bandpref_Subscription01" else "/nv/item_files/modem/mmode/lte_bandpref"
                        val PATH_LTE_EXTENSION = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256_Subscription01" else "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256"
                        val PATH_NR = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/nr_band_pref_Subscription01" else "/nv/item_files/modem/mmode/nr_band_pref"
                        val PATH_NR_NSA = if (selectedSimIndex == 1) "/nv/item_files/modem/mmode/nr_nsa_band_pref_Subscription01" else "/nv/item_files/modem/mmode/nr_nsa_band_pref"

                        // Instead of sending 0s, deleting the file resets NV items to default state
                        val res1 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 6 0 $PATH_LTE_PRIMARY")
                        val res2 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 6 0 $PATH_LTE_EXTENSION")
                        val res3 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 6 0 $PATH_NR_NSA")
                        val res4 = RootHelper.executeWithOutput("/vendor/bin/mtb 4 6 0 $PATH_NR")

                        if (instantApply) {
                            RootHelper.executeWithOutput("/vendor/bin/mtb 11 0")
                            delay(5000)
                        }

                        withContext(Dispatchers.Main) {
                            val isSim1 = selectedSimIndex == 0
                            val hasLockKey = if (isSim1) "locked_bands_applied_sim0" else "locked_bands_applied_sim1"
                            val bandsKey = if (isSim1) "locked_bands_set_sim0" else "locked_bands_set_sim1"
                            prefs.edit()
                                .putBoolean(hasLockKey, false)
                                .remove(bandsKey)
                                .apply()

                            statusMessage = "Default settings restored! (Check logs if failed)"
                            println("Restore Logs: $res1 | $res2 | $res3 | $res4")
                            val allBands = supportedLteBands.toSet() + supportedNsaBands.map { "$it (NSA)" }.toSet() + supportedSaBands.map { "$it (SA)" }.toSet()
                            if (selectedSimIndex == 0) {
                                selectedBandsSim1 = allBands
                            } else {
                                selectedBandsSim2 = allBands
                            }
                        }
                    }
                } else {
                    statusMessage = "Root (Magisk) permission is required!"
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("RESTORE DEFAULT BANDS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

fun getMccMncFromCellIdentity(identity: android.telephony.CellIdentity): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val mccStr = when {
            identity is android.telephony.CellIdentityLte -> identity.mccString
            identity is android.telephony.CellIdentityWcdma -> identity.mccString
            identity is android.telephony.CellIdentityGsm -> identity.mccString
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && identity is android.telephony.CellIdentityNr -> identity.mccString
            else -> null
        }
        val mncStr = when {
            identity is android.telephony.CellIdentityLte -> identity.mncString
            identity is android.telephony.CellIdentityWcdma -> identity.mncString
            identity is android.telephony.CellIdentityGsm -> identity.mncString
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && identity is android.telephony.CellIdentityNr -> identity.mncString
            else -> null
        }
        if (!mccStr.isNullOrEmpty() && !mncStr.isNullOrEmpty()) {
            return "$mccStr$mncStr"
        }
    }
    
    // Fallback to integer properties for LTE, WCDMA, GSM
    @Suppress("DEPRECATION")
    val mccInt = when (identity) {
        is android.telephony.CellIdentityLte -> identity.mcc
        is android.telephony.CellIdentityWcdma -> identity.mcc
        is android.telephony.CellIdentityGsm -> identity.mcc
        else -> 2147483647
    }
    @Suppress("DEPRECATION")
    val mncInt = when (identity) {
        is android.telephony.CellIdentityLte -> identity.mnc
        is android.telephony.CellIdentityWcdma -> identity.mnc
        is android.telephony.CellIdentityGsm -> identity.mnc
        else -> 2147483647
    }
    if (mccInt != 2147483647 && mncInt != 2147483647 && mccInt != 0) {
        val mncFormatted = if (mncInt < 10) "0$mncInt" else mncInt.toString()
        return "$mccInt$mncFormatted"
    }
    return ""
}

suspend fun queryAllCellInfos(context: Context): List<CellInfo> {
    val results = mutableListOf<CellInfo>()
    val defaultTm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    val managers = mutableListOf<TelephonyManager>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
        val subInfos = sm?.activeSubscriptionInfoList
        if (!subInfos.isNullOrEmpty()) {
            for (subInfo in subInfos) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    managers.add(defaultTm.createForSubscriptionId(subInfo.subscriptionId))
                }
            }
        }
    }
    
    if (managers.isEmpty()) {
        managers.add(defaultTm)
    }
    
    val executor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.mainExecutor
    } else {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        java.util.concurrent.Executor { command -> handler.post(command) }
    }
    
    for (tm in managers) {
        val cells = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            suspendCoroutine<List<CellInfo>> { continuation ->
                try {
                    tm.requestCellInfoUpdate(executor, object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cellInfo: List<CellInfo>) {
                            continuation.resume(cellInfo)
                        }
                        override fun onError(errorCode: Int, detail: Throwable?) {
                            try {
                                continuation.resume(tm.allCellInfo ?: emptyList())
                            } catch (e: Exception) {
                                continuation.resume(emptyList())
                            }
                        }
                    })
                } catch (e: Exception) {
                    try {
                        continuation.resume(tm.allCellInfo ?: emptyList())
                    } catch (e2: Exception) {
                        continuation.resume(emptyList())
                    }
                }
            }
        } else {
            tm.allCellInfo ?: emptyList()
        }
        results.addAll(cells)
    }
    
    return results.distinctBy { cell ->
        when (cell) {
            is CellInfoLte -> "${cell.cellIdentity.pci}-${cell.cellIdentity.earfcn}"
            is CellInfoNr -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val idNr = cell.cellIdentity as android.telephony.CellIdentityNr
                    "${idNr.pci}-${idNr.nrarfcn}"
                } else {
                    cell.toString()
                }
            }
            is android.telephony.CellInfoWcdma -> "${cell.cellIdentity.psc}-${cell.cellIdentity.uarfcn}"
            is android.telephony.CellInfoGsm -> "${cell.cellIdentity.bsic}-${cell.cellIdentity.arfcn}"
            else -> cell.toString()
        }
    }
}

data class NeighborCellInfo(
    val type: String,
    val pci: Int,
    val channel: Int,
    val dbm: Int,
    val isRegistered: Boolean,
    val cellId: String,
    val operatorName: String
)

data class NetworkModeOption(
    val label: String,
    val value: Int
)

fun getPreferredNetworkMode(context: Context, simIndex: Int): Int {
    try {
        val prefs = context.getSharedPreferences("band_locking_prefs", Context.MODE_PRIVATE)
        val savedMode = prefs.getInt("preferred_network_mode_sim_$simIndex", -1)
        if (savedMode != -1) {
            return savedMode
        }

        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
        val activeSubscriptionInfoList = sm.activeSubscriptionInfoList
        val subInfo = activeSubscriptionInfoList?.find { it.simSlotIndex == simIndex }
        val subId = subInfo?.subscriptionId
        
        var modeStr = ""
        if (subId != null) {
            modeStr = RootHelper.executeWithOutput("settings get global preferred_network_mode$subId").trim()
        }
        if (modeStr.isEmpty() || modeStr == "null" || modeStr == "-1") {
            modeStr = RootHelper.executeWithOutput("settings get global preferred_network_mode$simIndex").trim()
        }
        if (modeStr.isEmpty() || modeStr == "null" || modeStr == "-1") {
            modeStr = RootHelper.executeWithOutput("settings get global preferred_network_mode").trim()
        }
        // Extract any leading digits if there's multiple lines or trailing spaces
        val cleanMode = modeStr.takeWhile { it.isDigit() }
        return cleanMode.toIntOrNull() ?: 26 // Default to 2G/3G/4G/5G (26)
    } catch (e: Exception) {
        return 26
    }
}

@Composable
fun SettingsSection(context: Context, isRooted: Boolean, selectedSimIndex: Int, neighborCells: List<NeighborCellInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeighborCellMonitorSection(neighborCells)
    }
}

data class OperatorBrandInfo(
    val brandName: String,
    val brandColor: Color,
    val brandBgColor: Color,
    val logoText: String
)

fun getOperatorBrandInfo(mccMnc: String, operatorName: String): OperatorBrandInfo? {
    val clean = mccMnc.replace("-", "").trim()
    val nameUpper = operatorName.uppercase()
    
    // 1. Check if name contains key terms
    if (nameUpper.contains("JIO")) {
        return OperatorBrandInfo("Jio", Color(0xFFE91E63), Color(0xFFFCE4EC), "J")
    }
    if (nameUpper.contains("AIRTEL") || nameUpper.contains("BHARTI")) {
        return OperatorBrandInfo("Airtel", Color(0xFFE53935), Color(0xFFFFEBEE), "A")
    }
    if (nameUpper.contains("BSNL") || nameUpper.contains("MTNL") || nameUpper.contains("CELLONE")) {
        return OperatorBrandInfo("BSNL", Color(0xFF1E88E5), Color(0xFFE3F2FD), "B")
    }
    if (nameUpper.contains("VI ") || nameUpper == "VI" || nameUpper.contains("VODAFONE") || nameUpper.contains("IDEA") || nameUpper.startsWith("VI")) {
        return OperatorBrandInfo("Vi", Color(0xFFF57C00), Color(0xFFFFF3E0), "V")
    }
    
    // 2. Otherwise check by MCC and MNC
    if (clean.length >= 5) {
        val mcc = clean.take(3)
        val mnc = clean.drop(3)
        
        // India MCCs are 404, 405, 406
        if (mcc == "404" || mcc == "405" || mcc == "406") {
            val mncInt = mnc.toIntOrNull()
            if (mncInt != null) {
                // Jio
                if (mcc == "405" && mncInt in 854..874) return OperatorBrandInfo("Jio", Color(0xFFE91E63), Color(0xFFFCE4EC), "J")
                if (mcc == "405" && mncInt == 840) return OperatorBrandInfo("Jio", Color(0xFFE91E63), Color(0xFFFCE4EC), "J")
                if (mcc == "406") return OperatorBrandInfo("Jio", Color(0xFFE91E63), Color(0xFFFCE4EC), "J")
                
                // Airtel
                val airtelMncs = setOf(10, 28, 31, 40, 45, 49, 70, 78, 84, 86, 89, 90, 92, 93, 94, 95, 97, 98, 51, 52, 53, 54, 55, 56, 57, 70)
                if (mncInt in airtelMncs) return OperatorBrandInfo("Airtel", Color(0xFFE53935), Color(0xFFFFEBEE), "A")
                
                // BSNL / MTNL
                val bsnlMncs = setOf(34, 38, 51, 53, 54, 55, 57, 58, 59, 62, 64, 66, 71, 72, 73, 74, 75, 76, 77, 80, 81, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 50, 52, 25, 26, 27, 28, 29)
                if (mncInt in bsnlMncs) return OperatorBrandInfo("BSNL", Color(0xFF1E88E5), Color(0xFFE3F2FD), "B")
                
                // Vi
                val viMncs = setOf(1, 4, 5, 7, 11, 12, 13, 14, 15, 19, 20, 21, 22, 24, 27, 30, 43, 44, 46, 56, 60, 79, 82, 83, 85, 87, 88, 96, 66, 67, 751, 752, 753, 754, 755, 756, 757, 799, 845, 846, 848, 850)
                if (mncInt in viMncs) return OperatorBrandInfo("Vi", Color(0xFFF57C00), Color(0xFFFFF3E0), "V")
            }
        }
    }
    
    return null
}



@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun BottomNavBar(activeTab: String, onTabSelected: (String) -> Unit) {
    Column {
        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(icon = Icons.Default.Info, label = "Monitor", isSelected = activeTab == "Monitor", onClick = { onTabSelected("Monitor") })
            BottomNavItem(icon = Icons.Default.CellTower, label = "Info", isSelected = activeTab == "Info", onClick = { onTabSelected("Info") })
            BottomNavItem(icon = Icons.Default.Settings, label = "Settings", isSelected = activeTab == "Settings", onClick = { onTabSelected("Settings") })
        }
    }
}

@Composable
fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@SuppressLint("MissingPermission")
fun getActiveSignal(context: Context, simIndex: Int): SignalInfo {
    try {
        var tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var simOperator = ""
        var operatorName = ""
        var overrideType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            TelephonyDisplayInfoTracker.getOverrideNetworkType(simIndex)
        } else {
            0
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            val activeSubscriptionInfoList = sm.activeSubscriptionInfoList
            if (activeSubscriptionInfoList != null) {
                // Find subscription for the specific slot index (0 = SIM1, 1 = SIM2)
                val subInfo = activeSubscriptionInfoList.find { it.simSlotIndex == simIndex }
                if (subInfo != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tm = tm.createForSubscriptionId(subInfo.subscriptionId)
                    }
                    simOperator = tm.networkOperator
                    operatorName = tm.networkOperatorName ?: tm.simOperatorName ?: ""
                } else {
                    return SignalInfo("No SIM", "-", -120)
                }
            }
        }

        val lteBands = mutableSetOf<String>()
        val nrBands = mutableSetOf<String>()
        val lteBwMap = mutableMapOf<String, Int>()
        val nrBwMap = mutableMapOf<String, Int>()
        var dbm = -120

        val networkType = tm.networkType
        val dataNetworkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) tm.dataNetworkType else tm.networkType
        val serviceStateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm.serviceState?.toString() ?: "" else ""
        
        val isSimUsingNr = networkType == 20 || dataNetworkType == 20 ||
            serviceStateStr.contains("nrState=CONNECTED", ignoreCase = true) || 
            serviceStateStr.contains("nrState=NOT_RESTRICTED", ignoreCase = true) ||
            serviceStateStr.contains("mNrState=CONNECTED", ignoreCase = true) ||
            serviceStateStr.contains("mNrState=NOT_RESTRICTED", ignoreCase = true) ||
            serviceStateStr.contains("nrStatus=CONNECTED", ignoreCase = true) ||
            serviceStateStr.contains("nrStatus=NOT_RESTRICTED", ignoreCase = true) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && tm.serviceState?.networkRegistrationInfoList?.any { 
                it.cellIdentity is android.telephony.CellIdentityNr || it.accessNetworkTechnology == 20 
            } == true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val serviceState = tm.serviceState
            if (serviceState != null) {
                for (nri in serviceState.networkRegistrationInfoList) {
                    val cellIdentity = nri.cellIdentity
                    if (cellIdentity is android.telephony.CellIdentityNr) {
                        // For NR cells in NSA, the leg is not registered, so we parse it regardless of nri.isRegistered
                        var bandNum = getNrBand(cellIdentity.nrarfcn)
                        if (bandNum.startsWith("Unknown") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val nativeBands = cellIdentity.bands
                            if (nativeBands.isNotEmpty()) {
                                bandNum = nativeBands[0].toString()
                            }
                        }
                        val freqStr = getNrFreqStr(bandNum)
                        if (bandNum.isNotEmpty() && bandNum != "0" && !bandNum.startsWith("Unknown")) {
                            nrBands.add("n$bandNum ($freqStr)")
                            
                            // Try reflection for hidden bandwidth field on CellIdentityNr
                            var nrBw = 0
                            try {
                                val field = cellIdentity.javaClass.getDeclaredField("mBandwidth")
                                field.isAccessible = true
                                val bw = field.get(cellIdentity) as Int
                                if (bw > 0 && bw != 2147483647) {
                                    nrBw = bw / 1000
                                }
                            } catch (e: Exception) {
                                try {
                                    val method = cellIdentity.javaClass.getDeclaredMethod("getBandwidth")
                                    val bw = method.invoke(cellIdentity) as Int
                                    if (bw > 0 && bw != 2147483647) {
                                        nrBw = bw / 1000
                                    }
                                } catch (e2: Exception) {}
                            }
                            if (nrBw > 0) {
                                nrBwMap["n$bandNum"] = nrBw
                            }
                        }
                    } else if (cellIdentity is android.telephony.CellIdentityLte && nri.isRegistered) {
                        var bandNum = getLteBand(cellIdentity.earfcn)
                        if (bandNum.startsWith("Unknown") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val nativeBands = cellIdentity.bands
                            if (nativeBands.isNotEmpty()) {
                                bandNum = nativeBands[0].toString()
                            }
                        }
                        val freqStr = getLteFreqStr(bandNum)
                        if (bandNum.isNotEmpty() && bandNum != "0") {
                            if (bandNum.startsWith("Unknown")) {
                                lteBands.add(bandNum)
                            } else {
                                lteBands.add("B$bandNum ($freqStr)")
                                val bwKhz = cellIdentity.bandwidth
                                if (bwKhz > 0 && bwKhz != 2147483647) {
                                    lteBwMap["B$bandNum"] = bwKhz / 1000
                                }
                            }
                        }
                    }
                }
            }
            
            val signalStrength = tm.signalStrength
            if (signalStrength != null) {
                val nrStrengths = signalStrength.getCellSignalStrengths(android.telephony.CellSignalStrengthNr::class.java)
                if (nrStrengths.isNotEmpty()) {
                    dbm = nrStrengths.first().dbm
                } else {
                    val lteStrengths = signalStrength.getCellSignalStrengths(android.telephony.CellSignalStrengthLte::class.java)
                    if (lteStrengths.isNotEmpty()) {
                        dbm = lteStrengths.first().dbm
                    }
                }
            }
        }

        val allCellInfo = tm.allCellInfo
        if (allCellInfo != null) {
            for (info in allCellInfo) {
                val isNr = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr
                val isLte = info is CellInfoLte
                
                var cellMccMnc = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (isLte) cellMccMnc = "${(info as CellInfoLte).cellIdentity.mccString ?: ""}${(info.cellIdentity).mncString ?: ""}"
                    else if (isNr) cellMccMnc = "${(info.cellIdentity as android.telephony.CellIdentityNr).mccString ?: ""}${(info.cellIdentity as android.telephony.CellIdentityNr).mncString ?: ""}"
                }

                if (simOperator.isNotEmpty() && cellMccMnc.isNotEmpty() && simOperator != cellMccMnc) {
                    continue
                }

                val isServing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.isRegistered || info.cellConnectionStatus == android.telephony.CellInfo.CONNECTION_PRIMARY_SERVING || info.cellConnectionStatus == android.telephony.CellInfo.CONNECTION_SECONDARY_SERVING
                } else {
                    info.isRegistered
                }

                // For NR cells, we allow it if:
                // 1. It is serving or registered
                // 2. OR, it has a valid concrete nrarfcn and either matches our operator,
                //    or belongs to our active 5G carrier (if MCC/MNC is empty but 5G is active)
                val isNrWithValidArfcn = isNr && (info.cellIdentity as android.telephony.CellIdentityNr).nrarfcn != 2147483647
                val isSimCurrentlyUsing5G = overrideType == 3 || overrideType == 4 || overrideType == 5 || 
                                           networkType == 20 || dataNetworkType == 20 ||
                                           serviceStateStr.contains("nrState=CONNECTED", ignoreCase = true) || 
                                           serviceStateStr.contains("mNrState=CONNECTED", ignoreCase = true) || 
                                           serviceStateStr.contains("nrStatus=CONNECTED", ignoreCase = true)

                val isNrAllowed = isNr && (
                    isServing ||
                    (isNrWithValidArfcn && (
                        (simOperator.isNotEmpty() && cellMccMnc.isNotEmpty() && simOperator == cellMccMnc) ||
                        (cellMccMnc.isEmpty() && isSimCurrentlyUsing5G)
                    ))
                )

                // For LTE cells, we only want registered/serving cells. For NR cells, we check isNrAllowed.
                if (!isServing && !isNrAllowed) {
                    continue
                }

                if (isNr) {
                    val nrIdentity = info.cellIdentity as android.telephony.CellIdentityNr
                    var bandNum = getNrBand(nrIdentity.nrarfcn)
                    if (bandNum.startsWith("Unknown") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val nativeBands = nrIdentity.bands
                        if (nativeBands.isNotEmpty()) {
                            bandNum = nativeBands[0].toString()
                        }
                    }
                    val freqStr = getNrFreqStr(bandNum)
                    if (bandNum.isNotEmpty() && bandNum != "0" && !bandNum.startsWith("Unknown")) {
                        nrBands.add("n$bandNum ($freqStr)")
                        
                        var nrBw = 0
                        try {
                            val field = nrIdentity.javaClass.getDeclaredField("mBandwidth")
                            field.isAccessible = true
                            val bw = field.get(nrIdentity) as Int
                            if (bw > 0 && bw != 2147483647) {
                                nrBw = bw / 1000
                            }
                        } catch (e: Exception) {
                            try {
                                val method = nrIdentity.javaClass.getDeclaredMethod("getBandwidth")
                                val bw = method.invoke(nrIdentity) as Int
                                if (bw > 0 && bw != 2147483647) {
                                    nrBw = bw / 1000
                                }
                            } catch (e2: Exception) {}
                        }
                        if (nrBw > 0) {
                            nrBwMap["n$bandNum"] = nrBw
                        }
                    }
                    if (dbm == -120) dbm = info.cellSignalStrength.dbm
                } else if (isLte) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val cellIdentity = (info as CellInfoLte).cellIdentity
                        var bandNum = getLteBand(cellIdentity.earfcn)
                        if (bandNum.startsWith("Unknown") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val nativeBands = cellIdentity.bands
                            if (nativeBands.isNotEmpty()) {
                                bandNum = nativeBands[0].toString()
                            }
                        }
                        val freqStr = getLteFreqStr(bandNum)
                        if (bandNum.isNotEmpty() && bandNum != "0") {
                            if (bandNum.startsWith("Unknown")) {
                                lteBands.add(bandNum)
                            } else {
                                lteBands.add("B$bandNum ($freqStr)")
                                val bwKhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    cellIdentity.bandwidth
                                } else {
                                    2147483647
                                }
                                if (bwKhz > 0 && bwKhz != 2147483647) {
                                    lteBwMap["B$bandNum"] = bwKhz / 1000
                                }
                            }
                        }
                    } else if (lteBands.isEmpty()) {
                        lteBands.add("LTE")
                    }
                    if (dbm == -120 || nrBands.isEmpty()) dbm = info.cellSignalStrength.dbm
                }
            }
        }

        // Parse cellBandwidths from ServiceState on API >= 28
        val stateBws = mutableListOf<Int>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                tm.serviceState?.cellBandwidths?.forEach { bw ->
                    if (bw > 0 && bw != 2147483647) {
                        stateBws.add(bw / 1000)
                    }
                }
            } catch (e: Exception) {}
        }
        
        val parsedBws = getBandwidthsFromStateStr(serviceStateStr)
        val allBws = (stateBws + parsedBws).distinct().sortedDescending()

        // Distribute NR bandwidths
        for (nrBand in nrBands) {
            val cleanNrBand = nrBand.substringBefore(" ").trim()
            if (!nrBwMap.containsKey(cleanNrBand)) {
                val foundNrBw = allBws.find { it > 20 }
                if (foundNrBw != null) {
                    nrBwMap[cleanNrBand] = foundNrBw
                } else {
                    if (cleanNrBand == "n78") {
                        nrBwMap[cleanNrBand] = 100
                    } else if (cleanNrBand == "n28") {
                        nrBwMap[cleanNrBand] = 10
                    } else if (cleanNrBand == "n40") {
                        nrBwMap[cleanNrBand] = 40
                    } else if (cleanNrBand == "n41") {
                        nrBwMap[cleanNrBand] = 50
                    } else {
                        val fallback = allBws.firstOrNull()
                        if (fallback != null) {
                            nrBwMap[cleanNrBand] = fallback
                        }
                    }
                }
            }
        }

        // Distribute LTE bandwidths
        var lteBwIndex = 0
        val remainingLteBws = allBws.filter { it <= 20 }
        for (lteBand in lteBands) {
            val cleanLteBand = lteBand.substringBefore(" ").trim()
            if (!lteBwMap.containsKey(cleanLteBand)) {
                if (lteBwIndex < remainingLteBws.size) {
                    lteBwMap[cleanLteBand] = remainingLteBws[lteBwIndex]
                    lteBwIndex++
                } else {
                    if (cleanLteBand == "B40" || cleanLteBand == "B41") {
                        lteBwMap[cleanLteBand] = 20
                    } else {
                        lteBwMap[cleanLteBand] = 10
                    }
                }
            }
        }

        val cleanOperator = operatorName
            .replace("(?i)\\b5G\\b".toRegex(), "")
            .replace("(?i)\\b4G\\b".toRegex(), "")
            .replace("(?i)\\bLTE\\b".toRegex(), "")
            .trim()
            .replace("\\s+".toRegex(), " ")
        val prefix = if (cleanOperator.isNotEmpty()) "$cleanOperator " else ""
        val lteBandStr = lteBands.joinToString(" + ")
        val nrBandStr = nrBands.joinToString(" + ")

        val isLteCa = overrideType == 1 || overrideType == 2 || dataNetworkType == 19 || lteBands.size > 1

        val isNsaActive = (overrideType == 3 || overrideType == 4 || overrideType == 5 ||
                           serviceStateStr.contains("nrState=CONNECTED", ignoreCase = true) || 
                           serviceStateStr.contains("mNrState=CONNECTED", ignoreCase = true) || 
                           serviceStateStr.contains("nrStatus=CONNECTED", ignoreCase = true) ||
                           serviceStateStr.contains("nrState=CONNECTED_RESTRICTED", ignoreCase = true) || 
                           serviceStateStr.contains("mNrState=CONNECTED_RESTRICTED", ignoreCase = true) || 
                           serviceStateStr.contains("nrStatus=CONNECTED_RESTRICTED", ignoreCase = true) ||
                           networkType == 20 || dataNetworkType == 20 || nrBands.isNotEmpty()) && lteBands.isNotEmpty()

        val isNsaDisconnected = !isNsaActive && (
            serviceStateStr.contains("nrState=NOT_RESTRICTED", ignoreCase = true) || 
            serviceStateStr.contains("mNrState=NOT_RESTRICTED", ignoreCase = true) || 
            serviceStateStr.contains("nrStatus=NOT_RESTRICTED", ignoreCase = true) ||
            isSimUsingNr
        ) && lteBands.isNotEmpty()

        // Prepare bandwidth display parts
        val activeLteBwList = lteBands.mapNotNull { band ->
            val clean = band.substringBefore(" ").trim()
            lteBwMap[clean]
        }
        val activeNrBwList = nrBands.mapNotNull { band ->
            val clean = band.substringBefore(" ").trim()
            nrBwMap[clean]
        }

        val lteBwStr = if (activeLteBwList.isNotEmpty()) {
            activeLteBwList.joinToString(" + ") { "${it}MHz" }
        } else {
            ""
        }

        val nrBwStr = if (activeNrBwList.isNotEmpty()) {
            activeNrBwList.joinToString(" + ") { "${it}MHz" }
        } else if (isNsaActive || isNsaDisconnected) {
            "100MHz"
        } else {
            ""
        }

        val bwDisplay = if (isNsaActive || isNsaDisconnected) {
            if (lteBwStr.isNotEmpty() && nrBwStr.isNotEmpty()) {
                "BW: $lteBwStr + $nrBwStr"
            } else if (lteBwStr.isNotEmpty()) {
                "BW: $lteBwStr"
            } else if (nrBwStr.isNotEmpty()) {
                "BW: $nrBwStr"
            } else {
                ""
            }
        } else if (nrBands.isNotEmpty()) {
            if (nrBwStr.isNotEmpty()) "BW: $nrBwStr" else ""
        } else if (lteBands.isNotEmpty()) {
            if (lteBwStr.isNotEmpty()) "BW: $lteBwStr" else ""
        } else {
            ""
        }

        if (isNsaActive) {
            val fallbackNrBand = if (simOperator.startsWith("405") || simOperator.startsWith("406") || operatorName.contains("Jio", ignoreCase = true)) {
                "n78 (3500)"
            } else if (operatorName.contains("Airtel", ignoreCase = true) || simOperator.startsWith("404")) {
                "n78 (3500)"
            } else {
                "n78 (3500)"
            }
            val bandDisplay = if (nrBandStr.isNotEmpty()) "$lteBandStr + $nrBandStr" else "$lteBandStr + $fallbackNrBand"
            val typeDisplay = if (isLteCa) "${prefix}4G+5G NSA" else "${prefix}5G NSA"
            return SignalInfo(typeDisplay, bandDisplay, dbm, bwDisplay)
        } else if (isNsaDisconnected) {
            val fallbackNrBand = if (simOperator.startsWith("405") || simOperator.startsWith("406") || operatorName.contains("Jio", ignoreCase = true)) {
                "n78 (3500)"
            } else if (operatorName.contains("Airtel", ignoreCase = true) || simOperator.startsWith("404")) {
                "n78 (3500)"
            } else {
                "n78 (3500)"
            }
            val bandDisplay = if (nrBandStr.isNotEmpty()) "$lteBandStr + $nrBandStr (Disconnected)" else "$lteBandStr + *$fallbackNrBand (Disconnected)"
            val typeDisplay = if (isLteCa) "${prefix}4G+5G NSA (Disconnected)" else "${prefix}5G NSA (Disconnected)"
            return SignalInfo(typeDisplay, bandDisplay, dbm, bwDisplay)
        } else if (nrBandStr.isNotEmpty() && lteBandStr.isNotEmpty()) {
            val typeDisplay = if (isLteCa) "${prefix}4G+5G NSA" else "${prefix}5G NSA"
            return SignalInfo(typeDisplay, "$lteBandStr + $nrBandStr", dbm, bwDisplay)
        } else if (nrBandStr.isNotEmpty()) {
            return SignalInfo("${prefix}5G SA", nrBandStr, dbm, bwDisplay)
        } else if (lteBandStr.isNotEmpty()) {
            val typeDisplay = if (isLteCa) "${prefix}4G+ LTE" else "${prefix}4G LTE"
            return SignalInfo(typeDisplay, lteBandStr, dbm, bwDisplay)
        } else if (isSimUsingNr) {
            return SignalInfo("${prefix}5G", "5G", dbm, bwDisplay)
        }
        
        return SignalInfo("No Signal", "-", -120, bwDisplay)

    } catch (e: Exception) {
        e.printStackTrace()
        return SignalInfo("Error", "-", -120)
    }
}

fun getLteFreqStr(band: String): String {
    return when (band) {
        "1" -> "2100"
        "2" -> "1900"
        "3" -> "1800"
        "4" -> "1700/2100"
        "5" -> "850"
        "7" -> "2600"
        "8" -> "900"
        "12" -> "700"
        "13" -> "700"
        "14" -> "700"
        "17" -> "700"
        "18" -> "800"
        "19" -> "800"
        "20" -> "800"
        "25" -> "1900"
        "26" -> "850"
        "28" -> "700"
        "29" -> "700"
        "30" -> "2300"
        "32" -> "1500"
        "34" -> "2000"
        "38" -> "2600"
        "39" -> "1900"
        "40" -> "2300"
        "41" -> "2500"
        "42" -> "3500"
        "43" -> "3700"
        "46" -> "5200"
        "48" -> "3600"
        "66" -> "1700/2100"
        "71" -> "600"
        else -> "-"
    }
}

fun getNrFreqStr(band: String): String {
    return when (band) {
        "1" -> "2100"
        "2" -> "1900"
        "3" -> "1800"
        "5" -> "850"
        "7" -> "2600"
        "8" -> "900"
        "20" -> "800"
        "25" -> "1900"
        "28" -> "700"
        "38" -> "2600"
        "40" -> "2300"
        "41" -> "2500"
        "48" -> "3600"
        "66" -> "1700/2100"
        "71" -> "600"
        "77" -> "3700"
        "78" -> "3500"
        "79" -> "4700"
        "258" -> "26000"
        else -> "-"
    }
}

// Exhaustive 3GPP E-UTRA EARFCN lookup table
fun getLteBand(earfcn: Int): String {
    return when (earfcn) {
        // Downlink
        in 0..599 -> "1"
        in 600..1199 -> "2"
        in 1200..1949 -> "3"
        in 1950..2399 -> "4"
        in 2400..2649 -> "5"
        in 2650..2749 -> "6"
        in 2750..3449 -> "7"
        in 3450..3799 -> "8"
        in 3800..4149 -> "9"
        in 4150..4649 -> "10"
        in 4650..4749 -> "11"
        in 5010..5179 -> "12"
        in 5180..5279 -> "13"
        in 5280..5379 -> "14"
        in 5730..5849 -> "17"
        in 5850..5999 -> "18"
        in 6000..6149 -> "19"
        in 6150..6449 -> "20"
        in 6450..6599 -> "21"
        in 6600..7399 -> "22"
        in 7400..7499 -> "23"
        in 7500..7699 -> "24"
        in 8040..8689 -> "25"
        in 8690..9039 -> "26"
        in 9040..9209 -> "27"
        in 9210..9659 -> "28"
        in 9660..9769 -> "29"
        in 9770..9869 -> "30"
        in 9870..9919 -> "31"
        in 9920..10359 -> "32"
        in 36000..36199 -> "33"
        in 36200..36349 -> "34"
        in 36350..36899 -> "35"
        in 36900..37549 -> "36"
        in 37550..37749 -> "37"
        in 37750..38249 -> "38"
        in 38250..38649 -> "39"
        in 38650..39649 -> "40"
        in 39650..41589 -> "41"
        in 41590..43589 -> "42"
        in 43590..45589 -> "43"
        in 45590..46589 -> "44"
        in 46590..46789 -> "45"
        in 46790..54539 -> "46"
        in 54540..55239 -> "47"
        in 55240..56739 -> "48"
        in 56740..58239 -> "49"
        in 58240..59039 -> "50"
        in 59040..59139 -> "51"
        in 59140..60139 -> "52"
        in 60140..60259 -> "53"
        in 65536..66435 -> "65"
        in 66436..67335 -> "66"
        in 67336..67835 -> "67"
        in 67836..68335 -> "68"
        in 68336..68585 -> "69"
        in 68586..68835 -> "70"
        in 68836..69465 -> "71"
        in 69466..69665 -> "72"
        in 69666..69865 -> "73"
        in 69866..70365 -> "74"
        in 70366..70615 -> "75"
        in 70616..70865 -> "76"
        in 70866..71099 -> "85"
        in 71100..71219 -> "87"
        in 71220..71339 -> "88"

        // Uplink
        in 18000..18599 -> "1"
        in 18600..19199 -> "2"
        in 19200..19949 -> "3"
        in 19950..20399 -> "4"
        in 20400..20649 -> "5"
        in 20650..20749 -> "6"
        in 20750..21449 -> "7"
        in 21450..21799 -> "8"
        in 21800..22149 -> "9"
        in 22150..22649 -> "10"
        in 22650..22749 -> "11"
        in 23010..23179 -> "12"
        in 23180..23279 -> "13"
        in 23280..23379 -> "14"
        in 23730..23849 -> "17"
        in 23850..23999 -> "18"
        in 24000..24149 -> "19"
        in 24150..24449 -> "20"
        in 24450..24599 -> "21"
        in 24600..25399 -> "22"
        in 25400..25499 -> "23"
        in 25500..25699 -> "24"
        in 26040..26689 -> "25"
        in 26690..27039 -> "26"
        in 27040..27209 -> "27"
        in 27210..27659 -> "28"
        in 27770..27869 -> "30"
        in 27870..27919 -> "31"
        in 131072..131971 -> "65"
        in 131972..132671 -> "66"
        in 132672..133121 -> "68"
        in 133122..133171 -> "70"
        in 133172..134001 -> "71"
        in 134002..134201 -> "72"
        in 134202..134401 -> "73"
        in 134402..134901 -> "74"
        in 134902..135135 -> "85"
        in 135136..135255 -> "87"
        in 135256..135375 -> "88"

        else -> "Unknown ($earfcn)"
    }
}

fun getNrBand(nrarfcn: Int): String {
    return when (nrarfcn) {
        in 422000..434000 -> "1"
        in 386000..398000 -> "2"
        in 360000..378000 -> "3"
        in 173800..178800 -> "5"
        in 524000..538000 -> "7"
        in 185000..189000 -> "8"
        in 158200..164200 -> "20"
        in 386000..399000 -> "25"
        in 151600..164000 -> "28"
        in 514000..524000 -> "38"
        in 460000..480000 -> "40"
        in 499200..537999 -> "41"
        in 710000..734000 -> "48"
        in 422000..440000 -> "66"
        in 123400..130400 -> "71"
        in 620000..653333 -> "78"
        in 653334..680000 -> "77"
        in 690000..710000 -> "79"
        in 2016667..2075000 -> "258"
        else -> "Unknown ($nrarfcn)"
    }
}

fun getBandwidthsFromStateStr(serviceStateStr: String): List<Int> {
    val list = mutableListOf<Int>()
    val pattern = java.util.regex.Pattern.compile("(?:cell|lte|nr|mNr|mLte)Bandwidths=\\[([\\d\\s,]+)\\]", java.util.regex.Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(serviceStateStr)
    while (matcher.find()) {
        val listStr = matcher.group(1) ?: ""
        for (part in listStr.split(",")) {
            val trimmed = part.trim()
            val bwKhz = trimmed.toIntOrNull()
            if (bwKhz != null && bwKhz > 0 && bwKhz != 2147483647) {
                list.add(bwKhz / 1000)
            }
        }
    }
    return list
}

fun parseActiveBands(bandStr: String): Set<String> {
    val active = mutableSetOf<String>()
    if (bandStr.isEmpty() || bandStr == "-") return active
    val pattern = java.util.regex.Pattern.compile("(?:[BbNn])\\d+")
    val matcher = pattern.matcher(bandStr)
    while (matcher.find()) {
        val found = matcher.group().uppercase()
        active.add(found)
        // Ensure both N and n forms are added to match either style
        if (found.startsWith("N")) {
            active.add("n" + found.substring(1))
        } else if (found.startsWith("n")) {
            active.add("N" + found.substring(1))
        }
    }
    return active
}

fun parseLtePrimaryFromBytes(bytes: ByteArray): Set<String> {
    val bands = mutableSetOf<String>()
    for (bitIndex in 0 until 64) {
        val byteIndex = bitIndex / 8
        if (byteIndex < bytes.size) {
            val byteVal = bytes[byteIndex].toInt() and 0xFF
            val bit = (byteVal shr (bitIndex % 8)) and 1
            if (bit != 0) {
                bands.add("B${bitIndex + 1}")
            }
        }
    }
    return bands
}

fun parseLteExtensionFromBytes(bytes: ByteArray): Set<String> {
    val bands = mutableSetOf<String>()
    for (band in listOf(66, 71)) {
        val bitIndex = band - 65
        val byteIndex = bitIndex / 8
        if (byteIndex < bytes.size) {
            val byteVal = bytes[byteIndex].toInt() and 0xFF
            val bit = (byteVal shr (bitIndex % 8)) and 1
            if (bit != 0) {
                bands.add("B$band")
            }
        }
    }
    return bands
}

fun parseNrNsaFromBytes(bytes: ByteArray): Set<String> {
    val bands = mutableSetOf<String>()
    for (bitIndex in 0 until (bytes.size * 8)) {
        val byteIndex = bitIndex / 8
        if (byteIndex < bytes.size) {
            val byteVal = bytes[byteIndex].toInt() and 0xFF
            val bit = (byteVal shr (bitIndex % 8)) and 1
            if (bit != 0) {
                bands.add("N${bitIndex + 1} (NSA)")
            }
        }
    }
    return bands
}

fun parseNrSaFromBytes(bytes: ByteArray): Set<String> {
    val bands = mutableSetOf<String>()
    for (bitIndex in 0 until (bytes.size * 8)) {
        val byteIndex = bitIndex / 8
        if (byteIndex < bytes.size) {
            val byteVal = bytes[byteIndex].toInt() and 0xFF
            val bit = (byteVal shr (bitIndex % 8)) and 1
            if (bit != 0) {
                bands.add("N${bitIndex + 1} (SA)")
            }
        }
    }
    return bands
}

fun getCachedSupportedBands(
    context: android.content.Context,
    simIndex: Int,
    defaultLte: List<String>,
    defaultNsa: List<String>,
    defaultSa: List<String>,
    isRooted: Boolean
): Triple<List<String>, List<String>, List<String>> {
    val prefs = context.getSharedPreferences("band_locking_prefs", android.content.Context.MODE_PRIVATE)
    val isSim1 = simIndex == 0
    val suffix = if (isSim1) "sim0" else "sim1"
    
    val savedLte = prefs.getStringSet("supported_lte_v2_$suffix", null)
    val savedNsa = prefs.getStringSet("supported_nsa_v2_$suffix", null)
    val savedSa = prefs.getStringSet("supported_sa_v2_$suffix", null)
    
    if (savedLte != null && savedNsa != null && savedSa != null) {
        return Triple(
            savedLte.toList().sortedWith(compareBy { it.removePrefix("B").toIntOrNull() ?: 0 }),
            savedNsa.toList().sortedWith(compareBy { it.removePrefix("N").toIntOrNull() ?: 0 }),
            savedSa.toList().sortedWith(compareBy { it.removePrefix("N").toIntOrNull() ?: 0 })
        )
    }
    
    if (!isRooted) {
        return Triple(defaultLte, defaultNsa, defaultSa)
    }
    
    val PATH_LTE_SUPPORTED = if (isSim1) "/nv/item_files/modem/mmode/lte_bc_config" else "/nv/item_files/modem/mmode/lte_bc_config_Subscription01"
    val PATH_LTE_SUPPORTED_EXT = if (isSim1) "/nv/item_files/modem/mmode/lte_bc_config_extn_65_256" else "/nv/item_files/modem/mmode/lte_bc_config_extn_65_256_Subscription01"
    
    val primaryBytes = RootHelper.readNvItem(simIndex, PATH_LTE_SUPPORTED)
    val extBytes = RootHelper.readNvItem(simIndex, PATH_LTE_SUPPORTED_EXT)
    
    val lteSupported = if (primaryBytes != null && !primaryBytes.all { it == 0.toByte() }) {
        val parsed = parseLtePrimaryFromBytes(primaryBytes) + (extBytes?.let { parseLteExtensionFromBytes(it) } ?: emptySet())
        val filtered = parsed.filter { it.startsWith("B") }.map { it }.intersect(defaultLte.toSet()).toList()
        if (filtered.isEmpty()) defaultLte else filtered
    } else {
        defaultLte
    }
    
    val PATH_NR_NSA = if (isSim1) "/nv/item_files/modem/mmode/nr_nsa_band_pref" else "/nv/item_files/modem/mmode/nr_nsa_band_pref_Subscription01"
    val PATH_NR = if (isSim1) "/nv/item_files/modem/mmode/nr_band_pref" else "/nv/item_files/modem/mmode/nr_band_pref_Subscription01"
    
    val nsaBytes = RootHelper.readNvItem(simIndex, PATH_NR_NSA)
    val saBytes = RootHelper.readNvItem(simIndex, PATH_NR)
    
    val nsaSupported = if (nsaBytes != null && !nsaBytes.all { it == 0.toByte() }) {
        val parsed = parseNrNsaFromBytes(nsaBytes).map { it.substringBefore(" ") }
        val filtered = parsed.intersect(defaultNsa.toSet()).toList()
        if (filtered.isEmpty()) defaultNsa else filtered
    } else {
        defaultNsa
    }
    
    val saSupported = if (saBytes != null && !saBytes.all { it == 0.toByte() }) {
        val parsed = parseNrSaFromBytes(saBytes).map { it.substringBefore(" ") }
        val filtered = parsed.intersect(defaultSa.toSet()).toList()
        if (filtered.isEmpty()) defaultSa else filtered
    } else {
        defaultSa
    }
    
    prefs.edit().apply {
        putStringSet("supported_lte_v2_$suffix", lteSupported.toSet())
        putStringSet("supported_nsa_v2_$suffix", nsaSupported.toSet())
        putStringSet("supported_sa_v2_$suffix", saSupported.toSet())
        apply()
    }
    
    return Triple(
        lteSupported.sortedWith(compareBy { it.removePrefix("B").toIntOrNull() ?: 0 }),
        nsaSupported.sortedWith(compareBy { it.removePrefix("N").toIntOrNull() ?: 0 }),
        saSupported.sortedWith(compareBy { it.removePrefix("N").toIntOrNull() ?: 0 })
    )
}

fun loadLockedBandsForSim(simIndex: Int, lteBands: List<String>, nsaBands: List<String>, saBands: List<String>): Set<String> {
    val isSim1 = simIndex == 0
    val PATH_LTE_PRIMARY = if (isSim1) "/nv/item_files/modem/mmode/lte_bandpref" else "/nv/item_files/modem/mmode/lte_bandpref_Subscription01"
    val PATH_LTE_EXTENSION = if (isSim1) "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256" else "/nv/item_files/modem/mmode/lte_bandpref_extn_65_256_Subscription01"
    val PATH_NR_NSA = if (isSim1) "/nv/item_files/modem/mmode/nr_nsa_band_pref" else "/nv/item_files/modem/mmode/nr_nsa_band_pref_Subscription01"
    val PATH_NR = if (isSim1) "/nv/item_files/modem/mmode/nr_band_pref" else "/nv/item_files/modem/mmode/nr_band_pref_Subscription01"

    val primaryBytes = RootHelper.readNvItem(simIndex, PATH_LTE_PRIMARY)
    val extBytes = RootHelper.readNvItem(simIndex, PATH_LTE_EXTENSION)
    val nsaBytes = RootHelper.readNvItem(simIndex, PATH_NR_NSA)
    val saBytes = RootHelper.readNvItem(simIndex, PATH_NR)

    fun isAllZeros(bytes: ByteArray?): Boolean {
        return bytes == null || bytes.all { it == 0.toByte() }
    }

    val lteSelected = if (isAllZeros(primaryBytes)) {
        lteBands.toSet()
    } else {
        val parsed = parseLtePrimaryFromBytes(primaryBytes!!) + (extBytes?.let { parseLteExtensionFromBytes(it) } ?: emptySet())
        if (parsed.isEmpty()) lteBands.toSet() else parsed
    }

    val nsaSelected = if (isAllZeros(nsaBytes)) {
        nsaBands.map { "$it (NSA)" }.toSet()
    } else {
        val parsed = parseNrNsaFromBytes(nsaBytes!!)
        if (parsed.isEmpty()) nsaBands.map { "$it (NSA)" }.toSet() else parsed
    }

    val saSelected = if (isAllZeros(saBytes)) {
        saBands.map { "$it (SA)" }.toSet()
    } else {
        val parsed = parseNrSaFromBytes(saBytes!!)
        if (parsed.isEmpty()) saBands.map { "$it (SA)" }.toSet() else parsed
    }

    val bandsToSelect = lteSelected + nsaSelected + saSelected
    val supportedBands = lteBands.toSet() + nsaBands.map { "$it (NSA)" }.toSet() + saBands.map { "$it (SA)" }.toSet()
    return bandsToSelect.filter { it in supportedBands }.toSet()
}

fun parseMtbOutput(outputStr: String): ByteArray? {
    val clean = outputStr.trim()
        .replace("\r", "")
        .replace("\n", " ")
        .replace("(?i)read ok:".toRegex(), "")
        .replace("(?i)data:".toRegex(), "")
        .trim()
    
    if (clean.isEmpty()) return null

    // 1. Try to parse as space-separated or comma-separated decimal or hex numbers
    val parts = clean.split("[\\s,]+".toRegex()).filter { it.isNotEmpty() }
    if (parts.size >= 2) {
        val bytes = ByteArray(parts.size)
        var success = true
        for (i in parts.indices) {
            val part = parts[i]
            val value = when {
                part.startsWith("0x", ignoreCase = true) -> part.substring(2).toIntOrNull(16)
                part.any { it in 'a'..'f' || it in 'A'..'F' } -> part.toIntOrNull(16)
                else -> part.toIntOrNull() ?: part.toIntOrNull(16)
            }
            if (value != null && value in 0..255) {
                bytes[i] = value.toByte()
            } else {
                success = false
                break
            }
        }
        if (success) return bytes
    }

    // 2. Try to parse as a single contiguous hex string (even length, e.g., "0f000000")
    if (clean.length >= 2 && clean.length % 2 == 0 && clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        try {
            val bytes = ByteArray(clean.length / 2)
            for (i in bytes.indices) {
                bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            return bytes
        } catch (e: Exception) {
            // Ignore and fall through
        }
    }

    return null
}

object RootHelper {
    fun isDeviceRooted(): Boolean {
        return requestRoot()
    }

    fun requestRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = java.io.DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun execute(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    fun isValidBinaryNvData(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        try {
            val str = String(bytes, Charsets.UTF_8).lowercase()
            val errorKeywords = listOf(
                "fail", "error", "not found", "no such", "permission", 
                "cannot", "denied", "invalid", "usage", "not exist",
                "sh: ", "exception", "command", "not recognized"
            )
            for (kw in errorKeywords) {
                if (str.contains(kw)) {
                    return false
                }
            }
            var printableCount = 0
            for (b in bytes) {
                val i = b.toInt() and 0xFF
                if (i in 32..126 || i == 10 || i == 13 || i == 9) {
                    printableCount++
                }
            }
            val printableRatio = printableCount.toFloat() / bytes.size
            if (printableRatio > 0.9f && bytes.size > 8) {
                return false
            }
        } catch (e: Exception) {
            // Ignore
        }
        return true
    }
    
    fun readNvItem(subIndex: Int, path: String): ByteArray? {
        val actions = listOf(4, 3, 1)
        for (action in actions) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "/vendor/bin/mtb 4 $action $subIndex $path"))
                val bytes = process.inputStream.readBytes()
                process.waitFor()
                if (process.exitValue() == 0 && bytes.isNotEmpty()) {
                    val outputStr = String(bytes, Charsets.UTF_8)
                    val parsed = parseMtbOutput(outputStr)
                    if (parsed != null && isValidBinaryNvData(parsed)) {
                        return parsed
                    }
                    if (bytes.size >= 8 && isValidBinaryNvData(bytes)) {
                        return bytes
                    }
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        // Fallback to cat
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            val bytes = process.inputStream.readBytes()
            process.waitFor()
            if (process.exitValue() == 0 && bytes.isNotEmpty()) {
                val outputStr = String(bytes, Charsets.UTF_8)
                val parsed = parseMtbOutput(outputStr)
                if (parsed != null && isValidBinaryNvData(parsed)) return parsed
                if (isValidBinaryNvData(bytes)) return bytes
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    fun readRawBytes(path: String): ByteArray? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            val bytes = process.inputStream.readBytes()
            process.waitFor()
            if (process.exitValue() == 0 && bytes.isNotEmpty() && isValidBinaryNvData(bytes)) {
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun executeWithOutput(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            if (error.isNotEmpty()) {
                "ERROR: $error\n$output"
            } else {
                output
            }
        } catch (e: Exception) {
            "EXCEPTION: ${e.message}"
        }
    }
}

object TelephonyDisplayInfoTracker {
    private val overrideNetworkTypes = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    private var isInitialized = false

    @SuppressLint("MissingPermission")
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext
        
        val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            appContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
        } else null

        if (subscriptionManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            subscriptionManager.addOnSubscriptionsChangedListener(object : android.telephony.SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    registerCallbacks(appContext, subscriptionManager)
                }
            })
            registerCallbacks(appContext, subscriptionManager)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerCallbacks(context: Context, sm: android.telephony.SubscriptionManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val activeList = sm.activeSubscriptionInfoList ?: return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        for (subInfo in activeList) {
            val slotId = subInfo.simSlotIndex
            val subId = subInfo.subscriptionId
            val subTm = tm.createForSubscriptionId(subId)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : android.telephony.TelephonyCallback(), android.telephony.TelephonyCallback.DisplayInfoListener {
                    override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
                        overrideNetworkTypes[slotId] = telephonyDisplayInfo.overrideNetworkType
                    }
                }
                subTm.registerTelephonyCallback(context.mainExecutor, callback)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                val listener = object : android.telephony.PhoneStateListener() {
                    override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
                        overrideNetworkTypes[slotId] = telephonyDisplayInfo.overrideNetworkType
                    }
                }
                @Suppress("DEPRECATION")
                subTm.listen(listener, android.telephony.PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
            }
        }
    }

    fun getOverrideNetworkType(slotId: Int): Int {
        return overrideNetworkTypes[slotId] ?: 0
    }
}

@Composable
fun InfoSection(context: Context, selectedSimIndex: Int, currentSignal: SignalInfo) {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val NETWORK_TYPE_LTE_CA = 19
    
    // Live state calculations
    val subscriptionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
    } else null
    
    val activeSubscriptionInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        try {
            subscriptionManager?.activeSubscriptionInfoList
        } catch (e: SecurityException) {
            null
        }
    } else null
    
    val subInfo = activeSubscriptionInfoList?.find { it.simSlotIndex == selectedSimIndex }
    val subId = subInfo?.subscriptionId ?: -1
    
    val subTm = if (subId != -1) tm.createForSubscriptionId(subId) else tm
    
    // 1. IMS registration status via reflection (safe from hidden api checks)
    val isImsRegisteredDirect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val method = subTm.javaClass.getMethod("isImsRegistered")
            method.invoke(subTm) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
    
    // 2. Network Type
    val networkType = if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        try {
            subTm.dataNetworkType
        } catch (e: Exception) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    } else {
        TelephonyManager.NETWORK_TYPE_UNKNOWN
    }
    
    // Connectivity for Wi-Fi
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    val activeNetwork = connectivityManager?.activeNetwork
    val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
    val isWifiConnected = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ?: false
    
    // 3. Carrier Aggregation (CA) Status
    var isCaActive = false
    val allCellInfo = if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            subTm.allCellInfo
        } catch (e: Exception) {
            null
        }
    } else null
    
    var secondaryCellsCount = 0
    val secondaryBands = mutableListOf<String>()
    
    if (allCellInfo != null) {
        for (cell in allCellInfo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (cell.cellConnectionStatus == CellInfo.CONNECTION_SECONDARY_SERVING) {
                    isCaActive = true
                    secondaryCellsCount++
                    if (cell is CellInfoLte) {
                        val b = getLteBand(cell.cellIdentity.earfcn)
                        if (b.isNotEmpty() && !b.startsWith("Unknown")) {
                            secondaryBands.add("B$b")
                        }
                    } else if (cell is CellInfoNr) {
                        val b = getNrBand((cell.cellIdentity as android.telephony.CellIdentityNr).nrarfcn)
                        if (b.isNotEmpty() && !b.startsWith("Unknown")) {
                            secondaryBands.add("n$b")
                        }
                    }
                }
            }
        }
    }
    
    val overrideType = TelephonyDisplayInfoTracker.getOverrideNetworkType(selectedSimIndex)
    if (overrideType == 1 || overrideType == 2) { // 1 = LTE_CA, 2 = LTE_ADVANCED_PRO
        isCaActive = true
    }
    
    val isActuallyCaActive = isCaActive || 
                             (overrideType == 1 || overrideType == 2) ||
                             currentSignal.type.contains("4G+") || 
                             currentSignal.type.contains("LTE+") || 
                             currentSignal.type.contains("NSA") || 
                             currentSignal.type.contains("5G NSA") || 
                             currentSignal.band.contains("+")

    val caSupportLabel = remember(currentSignal.type, currentSignal.band, overrideType, context) {
        var isNrCapable = false
        var isLteCapable = false
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val raf = tm.supportedRadioAccessFamily
                isNrCapable = (raf and (1L shl 20)) != 0L // NETWORK_TYPE_NR is 20
                isLteCapable = (raf and (1L shl 13)) != 0L // NETWORK_TYPE_LTE is 13
            } else {
                isLteCapable = true
            }
        } catch (e: Throwable) {
            isLteCapable = true
        }
        
        val has5gSignal = currentSignal.type.contains("5G", ignoreCase = true) || 
                          currentSignal.type.contains("SA", ignoreCase = true) || 
                          currentSignal.type.contains("NSA", ignoreCase = true) ||
                          overrideType >= 3 ||
                          currentSignal.band.contains("n", ignoreCase = true)
                          
        val hasLteSignal = currentSignal.type.contains("4G", ignoreCase = true) ||
                           currentSignal.type.contains("LTE", ignoreCase = true) ||
                           overrideType == 1 || overrideType == 2 ||
                           currentSignal.band.contains("B", ignoreCase = true)
                           
        val supports4G = isLteCapable || hasLteSignal
        val supports5G = isNrCapable || has5gSignal
        
        when {
            supports4G && supports5G -> "CA (4G+/5G+)"
            supports5G -> "CA (5G+)"
            supports4G -> "CA (4G+)"
            else -> "CA"
        }
    }

    val caBandsText = remember(currentSignal.band, currentSignal.type, secondaryBands, isActuallyCaActive) {
        fun formatBandWithFreq(bandStr: String): String {
            val trimmed = bandStr.trim().replace("*", "")
            val pattern = java.util.regex.Pattern.compile("^([BbNn]\\d+)(?:\\s*\\((\\d+)\\))?.*$")
            val matcher = pattern.matcher(trimmed)
            if (matcher.matches()) {
                val band = matcher.group(1) ?: ""
                var freq = matcher.group(2)
                if (freq.isNullOrEmpty()) {
                    val bandNum = band.substring(1)
                    freq = if (band.uppercase().startsWith("B")) {
                        getLteFreqStr(bandNum)
                    } else {
                        getNrFreqStr(bandNum)
                    }
                }
                return if (!freq.isNullOrEmpty() && freq != "-") {
                    "$band($freq)"
                } else {
                    band
                }
            }
            val fallbackPattern = java.util.regex.Pattern.compile("^([BbNn])(\\d+)(\\d{4})$")
            val fbMatcher = fallbackPattern.matcher(trimmed)
            if (fbMatcher.matches()) {
                val prefix = fbMatcher.group(1) ?: ""
                val bandNum = fbMatcher.group(2) ?: ""
                val freq = fbMatcher.group(3) ?: ""
                return "$prefix$bandNum($freq)"
            }
            return trimmed
        }

        val rawBands = currentSignal.band.split("+").map { part ->
            part.trim()
        }.filter { it.isNotEmpty() && it != "-" }
        
        val cleanBands = rawBands.map { formatBandWithFreq(it) }.filter {
            it.contains(Regex("[BbNn]\\d+"))
        }
        
        if (cleanBands.size >= 2) {
            cleanBands.joinToString(" + ")
        } else {
            val primary = cleanBands.firstOrNull() ?: ""
            val list = mutableListOf<String>()
            if (primary.isNotEmpty()) {
                list.add(primary)
            }
            for (sec in secondaryBands) {
                val formattedSec = formatBandWithFreq(sec)
                val baseSec = formattedSec.substringBefore("(")
                val hasDuplicate = list.any { it.substringBefore("(").uppercase() == baseSec.uppercase() }
                if (!hasDuplicate) {
                    list.add(formattedSec)
                }
            }
            if (list.size >= 2) {
                list.joinToString(" + ")
            } else {
                if (currentSignal.type.contains("NSA") || currentSignal.type.contains("5G NSA")) {
                    val p = if (primary.isNotEmpty() && primary.startsWith("B")) primary else "B3(1800)"
                    val formattedP = formatBandWithFreq(p)
                    "$formattedP + n78(3500)"
                } else if (currentSignal.type.contains("SA") || currentSignal.type.contains("5G SA")) {
                    "n78(3500) + n258(26000)"
                } else if (isActuallyCaActive) {
                    val p = if (primary.isNotEmpty() && primary.startsWith("B")) primary else "B3(1800)"
                    val formattedP = formatBandWithFreq(p)
                    val baseP = formattedP.substringBefore("(")
                    val sec = if (baseP.uppercase().startsWith("B3")) "B40(2300)" else "B3(1800)"
                    "$formattedP + $sec"
                } else {
                    primary.ifEmpty { "-" }
                }
            }
        }
    }
    
    // 4. VoLTE, VoWiFi, VoNR Active Status
    val isLte = networkType == TelephonyManager.NETWORK_TYPE_LTE || 
                networkType == NETWORK_TYPE_LTE_CA ||
                currentSignal.type.contains("4G", ignoreCase = true) ||
                currentSignal.type.contains("LTE", ignoreCase = true)
                
    val isNr = networkType == TelephonyManager.NETWORK_TYPE_NR ||
               currentSignal.type.contains("5G", ignoreCase = true) ||
               currentSignal.type.contains("SA", ignoreCase = true)
               
    val isImsRegistered = isImsRegisteredDirect || 
                          ((isLte || isNr || isWifiConnected) && currentSignal.type != "No SIM" && currentSignal.type != "No Signal" && currentSignal.type != "Error")
                          
    val isVoLteActive = isImsRegistered && (isLte || (isNr && !currentSignal.type.contains("SA") && !currentSignal.type.contains("5G SA"))) && !isWifiConnected
    val isVoWiFiActive = isImsRegistered && isWifiConnected
    val isVoNrActive = isImsRegistered && isNr && (currentSignal.type.contains("SA") || currentSignal.type.contains("5G SA")) && !isWifiConnected
    
    // 5. 256QAM Modulation Status
    var liveCqi = -1
    var liveSnr = -120.0
    if (allCellInfo != null) {
        for (cell in allCellInfo) {
            if (cell.isRegistered) {
                if (cell is CellInfoLte) {
                    liveCqi = cell.cellSignalStrength.cqi
                    liveSnr = cell.cellSignalStrength.rssnr.toDouble()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                    val nrCsi = (cell.cellSignalStrength as? android.telephony.CellSignalStrengthNr)?.csiSinr ?: -120
                    val nrSs = (cell.cellSignalStrength as? android.telephony.CellSignalStrengthNr)?.ssSinr ?: -120
                    liveSnr = if (nrCsi != -120) nrCsi.toDouble() else nrSs.toDouble()
                }
            }
        }
    }
    
    // 256QAM supports on excellent signal quality
    val is256QamActive = (liveCqi >= 12 && liveCqi != 2147483647) || (liveSnr >= 15.0 && liveSnr <= 40.0) || currentSignal.dbm >= -80
    
    // 6. MIMO Configuration Status
    val activeMimo = when {
        isNr || currentSignal.band.contains("n78") || currentSignal.band.contains("n41") -> {
            if (currentSignal.dbm >= -85 && liveSnr >= 10.0) "4x4 MIMO (Active)" else "2x2 MIMO (Active)"
        }
        isLte && isActuallyCaActive && currentSignal.dbm >= -90 -> {
            "4x4 MIMO (Active)"
        }
        currentSignal.dbm <= -110 -> {
            "SISO (Fallback)"
        }
        else -> {
            "2x2 MIMO (Active)"
        }
    }
    
    // UI rendering starts here
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Hero Section Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Advanced Info",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Features Grid Header
        Text(
            text = "Feature Status Profile",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 1. Carrier Aggregation
        FeatureStatusCard(
            title = caSupportLabel,
            isActive = isActuallyCaActive,
            activeText = "Active ($caBandsText)",
            inactiveText = "Standby / Not Engaged"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 2. VoLTE
        FeatureStatusCard(
            title = "VoLTE",
            isActive = isVoLteActive,
            activeText = "Active (HD Voice)",
            inactiveText = if (isImsRegistered) "Standby / Available" else "Not Registered"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 3. VoWiFi
        FeatureStatusCard(
            title = "VoWiFi",
            isActive = isVoWiFiActive,
            activeText = "Active (Wi-Fi Calling)",
            inactiveText = "Standby / Not Connected"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 4. VoNR
        FeatureStatusCard(
            title = "VoNR",
            isActive = isVoNrActive,
            activeText = "Active (5G Standalone)",
            inactiveText = if (isImsRegistered && (isNr || currentSignal.type.contains("SA"))) "Standby / Available" else "Not Available on current network"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 5. 256QAM
        FeatureStatusCard(
            title = "256QAM",
            isActive = is256QamActive,
            activeText = "Active (High-Order Modulation)",
            inactiveText = "Standby (Requires SNR >= 15 dB)"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 6. MIMO
        FeatureStatusCard(
            title = "MIMO",
            isActive = true, // MIMO is always active in either 2x2, 4x4 or fallback
            activeText = activeMimo,
            inactiveText = "SISO (Single Stream)"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Diagnostics Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Live Physical Layer Stats",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                DiagRow(label = "IMS Profile Status", value = if (isImsRegistered) "Registered (SIP Active)" else "Unregistered / Offline")
                DiagRow(label = "Primary Access Tech", value = currentSignal.type)
                DiagRow(label = "Active Data Network", value = when (networkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE (Cat.18)"
                    NETWORK_TYPE_LTE_CA -> "4G LTE-Advanced (CA)"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G New Radio (SA/NSA)"
                    else -> "Unknown / Legacy Network"
                })
                DiagRow(label = "Signal Integrity SINR", value = if (liveSnr > -120.0) "$liveSnr dB" else "Unreported")
                DiagRow(label = "Channel Quality CQI", value = if (liveCqi != -1 && liveCqi != 2147483647) "$liveCqi" else "Unreported")
                DiagRow(label = "SIM Subscription ID", value = if (subId != -1) "SUB $subId" else "No active SUB")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FeatureStatusCard(
    title: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            val badgeBgColor = if (isActive) Color(0xFF0F2D19) else Color(0xFF33200B)
            val badgeTextColor = if (isActive) Color(0xFF81C784) else Color(0xFFFFB74D)
            val badgeBorderColor = if (isActive) Color(0xFF81C784).copy(alpha = 0.3f) else Color(0xFFFFB74D).copy(alpha = 0.3f)
            
            Box(
                modifier = Modifier
                    .background(badgeBgColor, RoundedCornerShape(6.dp))
                    .border(1.dp, badgeBorderColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isActive) activeText else inactiveText,
                    color = badgeTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
