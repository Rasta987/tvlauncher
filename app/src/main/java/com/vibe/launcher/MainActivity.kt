package com.vibe.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Yahan jo package names daaloge wo launcher me nahi dikhenge. */
val HIDDEN = setOf(
    "com.mitv.tvhome.atv",
    "com.xiaomi.mitv.handbook",
    "com.mitv.tvhome.mitvplus",
)

data class AppItem(val pkg: String, val label: String, val intent: Intent, val image: Bitmap)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    fun query(cat: String) = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(cat), 0
    )
    val found = query(Intent.CATEGORY_LEANBACK_LAUNCHER).ifEmpty { query(Intent.CATEGORY_LAUNCHER) }
    return found
        .filter { it.activityInfo.packageName != ctx.packageName && it.activityInfo.packageName !in HIDDEN }
        .distinctBy { it.activityInfo.packageName }
        .map { ri ->
            val pkg = ri.activityInfo.packageName
            val img = (ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)).toBitmap(320, 180)
            AppItem(
                pkg, ri.loadLabel(pm).toString(),
                pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, ri.activityInfo.name),
                img
            )
        }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    var focused by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { apps = loadApps(ctx) }
    BackHandler { /* launcher kabhi band nahi hona chahiye */ }

    val clock by produceState("") {
        while (true) {
            value = SimpleDateFormat("EEE, d MMM  •  h:mm a", Locale.getDefault()).format(Date())
            delay(15_000)
        }
    }
    val current = apps.getOrNull(focused)

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B10))) {
        // Dynamic blurred background: focused app ki image
        Crossfade(current, label = "bg") { app ->
            if (app != null) {
                Image(
                    app.image.asImageBitmap(), null,
                    modifier = Modifier.fillMaxSize().blur(60.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x99000000), Color(0xCC000000), Color(0xF0000000)))
            )
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(clock, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                current?.label ?: "", color = Color.White,
                fontSize = 56.sp, fontWeight = FontWeight.Bold
            )
            Text("Press OK to open", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
            Spacer(Modifier.height(28.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppCard(app, onFocus = { focused = index }) {
                        runCatching { ctx.startActivity(app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppCard(app: AppItem, onFocus: () -> Unit, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "scale")
    val shape = RoundedCornerShape(16.dp)
    Image(
        app.image.asImageBitmap(), app.label,
        modifier = Modifier
            .size(width = 200.dp, height = 112.dp)
            .scale(scale)
            .clip(shape)
            .background(Color(0xFF1C1C24))
            .border(if (isFocused) 3.dp else 0.dp, Color.White, shape)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}
