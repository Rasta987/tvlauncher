package com.vibe.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class AppItem(
    val pkg: String,
    val label: String,
    val intent: Intent,
    val image: Bitmap,
    val accent: Color,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }
}

private fun accentOf(bmp: Bitmap): Color {
    val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
    return Color(px).copy(alpha = 1f)
}

/** Banner ho to wahi, warna icon ko 16:9 tile par center karke banata hai. */
private fun tileFor(d: Drawable, isBanner: Boolean): Pair<Bitmap, Color> {
    val w = 480; val h = 270
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    if (isBanner) {
        d.setBounds(0, 0, w, h); d.draw(c)
        return out to accentOf(out)
    }
    val iconBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, 128, 128); d.draw(Canvas(iconBmp))
    val accent = accentOf(iconBmp)
    val base = lerp(Color(0xFF15151C), accent, 0.35f)
    c.drawColor(base.toArgb())
    val s = 150
    d.setBounds((w - s) / 2, (h - s) / 2, (w + s) / 2, (h + s) / 2); d.draw(c)
    return out to accent
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
            val banner = ri.activityInfo.loadBanner(pm)
            val (img, accent) = tileFor(banner ?: ri.loadIcon(pm), banner != null)
            AppItem(
                pkg, ri.loadLabel(pm).toString(),
                pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, ri.activityInfo.name),
                img, accent
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
    BackHandler { }

    val time by produceState("") {
        while (true) {
            value = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            delay(10_000)
        }
    }
    val date by produceState("") {
        while (true) {
            value = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
            delay(60_000)
        }
    }

    val current = apps.getOrNull(focused)
    val accent by animateColorAsState(
        current?.accent ?: Color(0xFF2A2A40), tween(600), label = "accent"
    )
    val top = lerp(Color(0xFF0A0A0F), accent, 0.55f)
    val bottom = Color(0xFF050508)

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        // Soft glow, top-left se
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(300f, 200f),
                    radius = 1400f
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(top = 40.dp, bottom = 36.dp)) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Home", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(time, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                    Text(date, color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Hero
            Column(Modifier.padding(horizontal = 72.dp)) {
                Text(
                    current?.label ?: "",
                    color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Press OK to open",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 72.dp, vertical = 24.dp)
            ) {
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppCard(app, onFocus = { focused = index }) {
                        runCatching { ctx.startActivity(app.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            }
        }
    }
}


@Composable
fun AppCard(app: AppItem, onFocus: () -> Unit, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.12f else 1f, tween(180), label = "scale")
    val dim by animateFloatAsState(if (isFocused) 1f else 0.72f, tween(180), label = "dim")
    val shape = RoundedCornerShape(18.dp)
    Image(
        app.image.asImageBitmap(), app.label,
        modifier = Modifier
            .size(width = 224.dp, height = 126.dp)
            .scale(scale)
            .shadow(if (isFocused) 24.dp else 0.dp, shape, ambientColor = app.accent, spotColor = app.accent)
            .clip(shape)
            .border(if (isFocused) 3.dp else 0.dp, Color.White, shape)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
        alpha = dim
    )
}
