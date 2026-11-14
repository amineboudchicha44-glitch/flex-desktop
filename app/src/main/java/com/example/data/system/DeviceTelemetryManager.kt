package com.example.data.system

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class HardwareProfile(
    val deviceName: String,
    val manufacturer: String,
    val formFactor: String,
    val osVersion: String,
    val isChromebook: Boolean,
    val isLowRamDevice: Boolean,
    val totalRamMb: Long,
    val availRamMb: Long
)

data class NetworkStatus(
    val isOnline: Boolean,
    val transportLabel: String
)

data class BatteryStatus(
    val levelPercent: Int,
    val isCharging: Boolean
)

data class InstalledAndroidApp(
    val packageName: String,
    val label: String,
    val category: String
)

class DeviceTelemetryManager(private val context: Context) {

    private val appContext = context.applicationContext

    fun readHardwareProfile(): HardwareProfile {
        val pm = appContext.packageManager
        val isArc = pm.hasSystemFeature("org.chromium.arc") ||
            pm.hasSystemFeature("org.chromium.arc.device_management")
        val model = Build.MODEL ?: "Android Device"
        val manufacturer = Build.MANUFACTURER ?: "Generic"
        val isChromebook = isArc ||
            model.contains("Chromebook", ignoreCase = true) ||
            Build.DEVICE.contains("hatch", ignoreCase = true) ||
            Build.DEVICE.contains("octopus", ignoreCase = true) ||
            Build.DEVICE.contains("snappy", ignoreCase = true)

        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val isLowRam = am.isLowRamDevice || totalMb <= 4096L

        val screenDp = appContext.resources.configuration.smallestScreenWidthDp
        val formFactor = when {
            isChromebook -> "chromebook"
            screenDp >= 600 -> "tablet"
            else -> "phone"
        }

        val prettyName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }

        return HardwareProfile(
            deviceName = prettyName,
            manufacturer = manufacturer,
            formFactor = formFactor,
            osVersion = "Android ${Build.VERSION.RELEASE}",
            isChromebook = isChromebook,
            isLowRamDevice = isLowRam,
            totalRamMb = totalMb,
            availRamMb = availMb
        )
    }

    fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun currentStatus(): NetworkStatus {
            val network = cm.activeNetwork
            val caps = if (network != null) cm.getNetworkCapabilities(network) else null
            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val label = when {
                !hasInternet -> "Offline"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                else -> "Connected"
            }
            return NetworkStatus(isOnline = hasInternet, transportLabel = label)
        }

        trySend(currentStatus())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentStatus())
            }

            override fun onLost(network: Network) {
                trySend(currentStatus())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(currentStatus())
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    fun observeBatteryStatus(): Flow<BatteryStatus> = callbackFlow {
        fun parseIntent(intent: Intent?): BatteryStatus {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            return BatteryStatus(levelPercent = pct, isCharging = charging)
        }

        val initial = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(parseIntent(initial))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(parseIntent(intent))
            }
        }
        appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    suspend fun loadInstalledLauncherApps(): List<InstalledAndroidApp> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val selfPkg = appContext.packageName
        resolved
            .asSequence()
            .filter { it.activityInfo.packageName != selfPkg }
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
                val category = inferCategory(pkg, label)
                InstalledAndroidApp(
                    packageName = pkg,
                    label = label,
                    category = category
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchInstalledApp(packageName: String): Boolean {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun inferCategory(pkg: String, label: String): String {
        val haystack = "$pkg $label".lowercase()
        return when {
            haystack.contains("chrome") || haystack.contains("browser") || haystack.contains("mail") || haystack.contains("docs") -> "Web & Work"
            haystack.contains("camera") || haystack.contains("photo") || haystack.contains("music") || haystack.contains("video") || haystack.contains("youtube") -> "Media"
            haystack.contains("setting") || haystack.contains("clock") || haystack.contains("calc") || haystack.contains("file") -> "Utilities"
            else -> "Android Apps"
        }
    }
}
