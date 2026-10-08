package org.sharkdroid

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.sharkdroid.ui.DetailData
import org.sharkdroid.ui.DetailScreen
import org.sharkdroid.ui.FilterValidity
import org.sharkdroid.ui.MainScreen
import org.sharkdroid.ui.MainSheet
import org.sharkdroid.ui.MainUiState
import org.sharkdroid.ui.RootStatus
import org.sharkdroid.ui.SavedCapture
import org.sharkdroid.ui.SavedScreen
import org.sharkdroid.ui.SettingsScreen
import org.sharkdroid.ui.SharkTheme
import org.sharkdroid.ui.StatsScreen
import org.sharkdroid.ui.ThemeMode
import java.io.File

/**
 * JVM screenshot tests: real Compose UI rendered by Robolectric's native graphics,
 * fed with fake data. Output: -PscreenshotDir (default ../out/screenshots).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w400dp-h860dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val dir = File(System.getProperty("sharkdroid.screenshotDir") ?: "build/screenshots").apply { mkdirs() }
    private fun out(name: String) = File(dir, "$name.png").absolutePath

    private val capturing = MainUiState(
        capture = CaptureManager.State.CAPTURING, root = RootStatus.OK, iface = FakeData.ifaces[1],
        fileName = "cap_wlan0_20261008_130211.pcapng", packets = 64, bytes = 31_744, durationMs = 83_000, written = 48_212,
    )

    @Composable
    private fun Main(
        state: MainUiState = capturing, rows: List<PacketRow> = FakeData.rows(), dark: Boolean = false,
        sheet: MainSheet = MainSheet.NONE, filterExpanded: Boolean = false,
        query: String = "", validity: FilterValidity = FilterValidity.EMPTY, error: String? = null,
    ) {
        SharkTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false) {
            MainScreen(
                state = state, ifaces = FakeData.ifaces, ifacesLoading = false, rows = rows, rowsVersion = rows.size,
                filterQuery = query, filterValidity = validity, filterError = error,
                filterHistory = listOf("tls.handshake.type == 1", "dns && ip.addr == 192.168.31.1", "tcp.analysis.flags"),
                captureFilter = "", snaplen = Tools.DEFAULT_SNAPLEN, autoScrollDefault = false,
                snackbar = SnackbarHostState(),
                onFilterQuery = {}, onApplyFilter = {}, onClearHistory = {}, onSelectIface = {}, onRefreshIfaces = {},
                onRetryRoot = {}, onStart = {}, onStop = {}, onPacketClick = {}, onMenu = {}, onMessage = {},
                initialSheet = sheet, initialFilterExpanded = filterExpanded,
            )
        }
    }

    private fun shot(name: String, screen: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent(content)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1500)
        if (screen) captureScreenRoboImage(out(name)) else compose.onRoot().captureRoboImage(out(name))
    }

    @Test fun mainLight() = shot("01_main_light") { Main() }

    @Test @Config(qualifiers = "+night") fun mainDark() = shot("02_main_dark") { Main(dark = true) }

    @Test fun mainEmpty() = shot("03_main_empty") {
        Main(state = MainUiState(root = RootStatus.OK, iface = FakeData.ifaces[1]), rows = emptyList())
    }

    @Test fun interfaceSheet() = shot("04_interface_sheet", screen = true) {
        Main(state = MainUiState(root = RootStatus.OK, iface = FakeData.ifaces[1]), rows = emptyList(), sheet = MainSheet.IFACES)
    }

    @Test fun startSheet() = shot("05_start_capture_sheet", screen = true) {
        Main(state = MainUiState(root = RootStatus.OK, iface = FakeData.ifaces[2]), rows = emptyList(), sheet = MainSheet.START)
    }

    @Test fun filterSearchValid() = shot("06_display_filter_valid") {
        Main(state = capturing.copy(capture = CaptureManager.State.IDLE), filterExpanded = true, query = "tls.handshake.type == 1",
            validity = FilterValidity.VALID)
    }

    @Test fun filterSearchInvalid() = shot("07_display_filter_invalid") {
        Main(state = capturing.copy(capture = CaptureManager.State.IDLE), filterExpanded = true, query = "tls.handshake.typo == 1",
            validity = FilterValidity.INVALID, error = "\"tls.handshake.typo\" is neither a field nor a protocol name.")
    }

    @Test fun detail() {
        val (roots, bytes) = FakeData.detail()
        shot("08_packet_detail") {
            SharkTheme(ThemeMode.LIGHT, dynamicColor = false) {
                DetailScreen(42, "TLSv1.3 · 192.168.31.105 → 93.184.215.14", DetailData(roots, bytes, loading = false),
                    SnackbarHostState(), {}, {}, { _, _ -> }, {}, initialSelected = FakeData.selectedSni(roots))
            }
        }
    }

    @Test @Config(qualifiers = "+night") fun detailDark() {
        val (roots, bytes) = FakeData.detail()
        shot("09_packet_detail_dark") {
            SharkTheme(ThemeMode.DARK, dynamicColor = false) {
                DetailScreen(42, "TLSv1.3 · 192.168.31.105 → 93.184.215.14", DetailData(roots, bytes, loading = false),
                    SnackbarHostState(), {}, {}, { _, _ -> }, {}, initialSelected = FakeData.selectedSni(roots), hexExpanded = true)
            }
        }
    }

    @Test fun saved() = shot("10_saved_captures") {
        val now = 1_791_436_800_000L
        val items = listOf(
            SavedCapture(File("cap_wlan0_20261008_130211.pcapng"), 48_212, now, isCurrent = true, isLive = true),
            SavedCapture(File("cap_rmnet_data2_20261008_121540.pcapng"), 2_811_904, now - 3_600_000, false, false),
            SavedCapture(File("cap_any_20261007_224402.pcapng"), 15_204_352, now - 52_000_000, false, false),
            SavedCapture(File("imp_20261006_093011_http-sample.pcapng"), 25_600, now - 140_000_000, false, false),
            SavedCapture(File("cap_tun0_20261005_181233.pcapng"), 731_136, now - 240_000_000, false, false),
        )
        SharkTheme(ThemeMode.LIGHT, dynamicColor = false) { SavedScreen(items, SnackbarHostState(), {}, {}, {}, {}, {}) }
    }

    @Test fun stats() = shot("11_statistics") {
        SharkTheme(ThemeMode.LIGHT, dynamicColor = false) {
            StatsScreen("cap_wlan0_20261008_130211.pcapng", 1_284, 912_384, 83_000,
                listOf("TLSv1.3" to 512, "TCP" to 341, "QUIC" to 203, "DNS" to 118, "HTTP" to 44, "UDP" to 31, "ICMP" to 20, "ARP" to 15),
                false, {}, {})
        }
    }

    @Test fun settings() = shot("12_settings") {
        SharkTheme(ThemeMode.LIGHT, dynamicColor = false) {
            SettingsScreen(ThemeMode.SYSTEM, true, Tools.DEFAULT_SNAPLEN, true, "1.1.0-ws4.6.9", {}, {}, {}, {}, {}, {}, {})
        }
    }

    @Test @Config(qualifiers = "en-rUS-w400dp-h860dp-xxhdpi") fun mainEnglish() = shot("13_main_english") { Main() }

    @Test fun launcherIcon() = shot("00_launcher_icon") { IconSheet() }

    @Composable
    private fun AdaptiveIcon(size: Int, shape: Shape, themed: Boolean = false) {
        // Adaptive icon layers are 108dp; launchers show the inner 72dp through a mask.
        val layer = (size * 108f / 72f).dp
        Box(Modifier.size(size.dp).clip(shape), contentAlignment = Alignment.Center) {
            if (themed) {
                Box(Modifier.requiredSize(layer).background(Color(0xFFD7E8EA)))
                Image(painterResource(R.drawable.ic_launcher_monochrome), null, Modifier.requiredSize(layer),
                    colorFilter = ColorFilter.tint(Color(0xFF00504F)))
            } else {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(layer))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(layer))
            }
        }
    }

    @Composable
    private fun IconSheet() {
        SharkTheme(ThemeMode.LIGHT, dynamicColor = false) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SharkDroid launcher icon", style = MaterialTheme.typography.titleLarge)
                    Text("Original adaptive icon · vector · not the Wireshark logo", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    AdaptiveIcon(200, RoundedCornerShape(30))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Labeled("Circle") { AdaptiveIcon(84, CircleShape) }
                        Labeled("Squircle") { AdaptiveIcon(84, RoundedCornerShape(38)) }
                        Labeled("Rounded") { AdaptiveIcon(84, RoundedCornerShape(16)) }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Labeled("Themed (Android 13+)") { AdaptiveIcon(84, CircleShape, themed = true) }
                        Labeled("Home screen") {
                            Box(Modifier.size(150.dp, 84.dp).clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF3E5F8A), Color(0xFFB98A6B)))),
                                contentAlignment = Alignment.Center) {
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    AdaptiveIcon(52, CircleShape); AdaptiveIcon(52, CircleShape, themed = true)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Notification icon", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF0E7C86)), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_stat_capture), null, Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun Labeled(label: String, content: @Composable () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
