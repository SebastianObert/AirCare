package com.example.aircare

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

// Helper Warna AQI (Status Utama)
fun getAqiColor(status: String): Color {
    return when {
        status.contains("Baik", true) || status.contains("Good", true) -> Color(0xFF00C853)
        status.contains("Sedang", true) || status.contains("Moderate", true) -> Color(0xFFFFAB00)
        status.contains("Tidak Sehat", true) || status.contains("Unhealthy", true) -> Color(0xFFFF3D00)
        else -> Color(0xFFD50000)
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onChangeLocationClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRefreshLocationClick: () -> Unit,
    onInboxClick: () -> Unit,
    onCheckPredictionClick: () -> Unit // Parameter baru
) {
    // State Observation
    val location by viewModel.location.observeAsState("Memuat...")
    val lastUpdated by viewModel.lastUpdated.observeAsState("")
    val aqiValue by viewModel.aqiValue.observeAsState("0")
    val aqiStatus by viewModel.aqiStatus.observeAsState("-")

    // Warna Utama Dinamis
    val primaryColor = getAqiColor(aqiStatus)
    val animatedColor by animateColorAsState(targetValue = primaryColor, animationSpec = tween(800), label = "mainColor")
    val backgroundColor = Color(0xFFF8F9FA)

    // Weather
    val temperature by viewModel.temperature.observeAsState("--")
    val weatherDesc by viewModel.weatherDescription.observeAsState("-")
    val weatherIconUrl by viewModel.weatherIconUrl.observeAsState("")

    // Data Recommendation
    val recText by viewModel.recommendationText.observeAsState("")

    // Data Polutan
    val pm25 by viewModel.pm25Value.observeAsState("0")
    val co by viewModel.coValue.observeAsState("0")
    val o3 by viewModel.o3Value.observeAsState("0")
    val no2 by viewModel.no2Value.observeAsState("0")
    val so2 by viewModel.so2Value.observeAsState("0")

    val forecastList by viewModel.forecastData.observeAsState(emptyList())
    val isSaveReady by viewModel.isDataReadyToSave.observeAsState(false)

    val scrollState = rememberScrollState()
    var showAqiGuide by remember { mutableStateOf(false) }
    var selectedForecast by remember { mutableStateOf<DailyForecast?>(null) }


    if (showAqiGuide) {
        AqiScaleGuideDialog(onDismissRequest = { showAqiGuide = false })
    }
    selectedForecast?.let { forecast ->
        ForecastDetailDialog(forecast = forecast, onDismiss = { selectedForecast = null })
    }


    Scaffold(
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {

            // ================== 1. HEADER SECTION ==================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(animatedColor)
                    .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- Top Bar: Location and Inbox ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Spacer to balance the right icon
                        Spacer(modifier = Modifier.width(48.dp))

                        // Location button in the center
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { onChangeLocationClick() }
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Rounded.Search, contentDescription = "Search Location", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Inbox button on the right
                        IconButton(
                            onClick = onInboxClick,
                            modifier = Modifier.width(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_inbox),
                                contentDescription = "Inbox",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Updated Info ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (lastUpdated.isNotEmpty()) "Diperbarui: $lastUpdated" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        // TOMBOL REFRESH KECIL
                        if (lastUpdated.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onRefreshLocationClick,
                                modifier = Modifier.size(20.dp) // Ukuran kecil pas
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh GPS",
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- AQI & Weather Info ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kiri: AQI (With Radar Effect)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            // Container Utama untuk AQI dan Efek Ripple
                            Box(contentAlignment = Alignment.Center) {

                                // 1. EFEK RADAR (BACKGROUND)
                                RadarRippleEffect(color = Color.White)

                                // 2. LINGKARAN PUTIH UTAMA (FOREGROUND)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(Color.White, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { showAqiGuide = true }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("AQI", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = aqiValue,
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = animatedColor,
                                            letterSpacing = (-2).sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = aqiStatus,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Kanan: Weather
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Cuaca Saat Ini", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (weatherIconUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = weatherIconUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                                Text(
                                    text = temperature,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = weatherDesc,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ================== 2. BODY CONTENT ==================
            Column(modifier = Modifier.padding(24.dp)) {

                // --- Recommendation Banner ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(animatedColor.copy(alpha = 0.1f))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = animatedColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Rekomendasi", style = MaterialTheme.typography.labelLarge, color = animatedColor, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recText.ifEmpty { "Data belum tersedia." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Forecast Section ---
                Text("Ramalan Cuaca", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // --- Forecast Chart ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        factory = { context ->
                            LineChart(context).apply {
                                description.isEnabled = false
                                axisRight.isEnabled = false
                                setNoDataText("")
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.setDrawGridLines(false)
                                xAxis.textColor = AndroidColor.GRAY
                                xAxis.granularity = 1f
                                axisLeft.textColor = AndroidColor.GRAY
                                axisLeft.enableGridDashedLine(10f, 10f, 0f)
                                axisLeft.setDrawAxisLine(false)
                                legend.isEnabled = true
                                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
                                setTouchEnabled(true)
                                setExtraOffsets(0f, 0f, 0f, 10f)

                                val marker = CustomMarkerView(context, R.layout.marker_view)
                                this.marker = marker
                            }
                        },
                        update = { chart ->
                            if (forecastList.isNotEmpty()) {
                                val entriesMax = forecastList.mapIndexed { index, f ->
                                    Entry(index.toFloat(), f.tempMax.removeSuffix("°").toFloatOrNull() ?: 0f)
                                }
                                val entriesMin = forecastList.mapIndexed { index, f ->
                                    Entry(index.toFloat(), f.tempMin.removeSuffix("°").toFloatOrNull() ?: 0f)
                                }
                                val colorMax = AndroidColor.parseColor("#FF5252")
                                val colorMin = AndroidColor.parseColor("#448AFF")

                                val setMax = LineDataSet(entriesMax, "Max").apply {
                                    color = colorMax
                                    setCircleColor(colorMax)
                                    lineWidth = 3f
                                    circleRadius = 5f
                                    setDrawCircleHole(true)
                                    circleHoleRadius = 2.5f
                                    valueTextSize = 10f
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    setDrawValues(false)
                                }
                                val setMin = LineDataSet(entriesMin, "Min").apply {
                                    color = colorMin
                                    setCircleColor(colorMin)
                                    lineWidth = 3f
                                    circleRadius = 5f
                                    setDrawCircleHole(true)
                                    circleHoleRadius = 2.5f
                                    valueTextSize = 10f
                                    mode = LineDataSet.Mode.CUBIC_BEZIER
                                    setDrawValues(false)
                                }
                                chart.data = LineData(setMax, setMin)

                                chart.xAxis.apply {
                                    valueFormatter = IndexAxisValueFormatter(forecastList.map { it.day })
                                    labelCount = forecastList.size
                                }

                                chart.invalidate()
                            }
                        }
                    )
                    if (forecastList.isEmpty()) {
                        CircularProgressIndicator(
                            color = animatedColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Detailed Forecast Cards ---
                if (forecastList.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(forecastList) { forecast ->
                            ForecastCard(
                                forecast = forecast,
                                tintColor = animatedColor,
                                modifier = Modifier.clickable { selectedForecast = forecast }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Memuat ramalan...", color = Color.Gray)
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                // --- FLOATING BUBBLES SECTION ---
                Text("Komposisi Udara", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                var selectedBubble by remember { mutableStateOf<String?>("PM2.5") }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0xFFEFF3F6))
                                )
                            )
                    )

                    FloatingBubble(
                        title = "PM2.5", value = pm25, unit = "µg/m³",
                        bubbleColor = Color(0xFFE3F2FD), textColor = Color(0xFF1565C0),
                        size = 140.dp, initialAlign = BiasAlignment(0f, -0.2f),
                        moveRange = 40f, speedMillis = 4000,
                        modifier = Modifier.zIndex(if (selectedBubble == "PM2.5") 1f else 0f),
                        onClick = { selectedBubble = "PM2.5" }
                    )

                    FloatingBubble(
                        title = "CO", value = co, unit = "ppm",
                        bubbleColor = Color(0xFFE0F2F1), textColor = Color(0xFF00695C),
                        size = 110.dp, initialAlign = BiasAlignment(-0.7f, -0.8f),
                        moveRange = 30f, speedMillis = 3500,
                        modifier = Modifier.zIndex(if (selectedBubble == "CO") 1f else 0f),
                        onClick = { selectedBubble = "CO" }
                    )

                    FloatingBubble(
                        title = "NO₂", value = no2, unit = "ppb",
                        bubbleColor = Color(0xFFF3E5F5), textColor = Color(0xFF6A1B9A),
                        size = 110.dp, initialAlign = BiasAlignment(0.7f, -0.7f),
                        moveRange = 35f, speedMillis = 3000,
                        modifier = Modifier.zIndex(if (selectedBubble == "NO₂") 1f else 0f),
                        onClick = { selectedBubble = "NO₂" }
                    )

                    FloatingBubble(
                        title = "O₃", value = o3, unit = "ppb",
                        bubbleColor = Color(0xFFFFF3E0), textColor = Color(0xFFEF6C00),
                        size = 120.dp, initialAlign = BiasAlignment(-0.6f, 0.7f),
                        moveRange = 45f, speedMillis = 4500,
                        modifier = Modifier.zIndex(if (selectedBubble == "O₃") 1f else 0f),
                        onClick = { selectedBubble = "O₃" }
                    )

                    FloatingBubble(
                        title = "SO₂", value = so2, unit = "ppb",
                        bubbleColor = Color(0xFFFCE4EC), textColor = Color(0xFFAD1457),
                        size = 100.dp, initialAlign = BiasAlignment(0.7f, 0.6f),
                        moveRange = 25f, speedMillis = 3800,
                        modifier = Modifier.zIndex(if (selectedBubble == "SO₂") 1f else 0f),
                        onClick = { selectedBubble = "SO₂" }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                 // --- Tombol Prediksi Kesehatan ---
                OutlinedButton(
                    onClick = onCheckPredictionClick, // Menggunakan lambda
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, animatedColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedColor)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(24.dp))

                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cek Prediksi Kesehatan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Save Button ---
                Button(
                    onClick = onSaveClick,
                    enabled = isSaveReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedColor,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Simpan Riwayat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

//CUSTOM COMPONENTS

@Composable
fun ForecastCard(forecast: DailyForecast, tintColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = forecast.day,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            AsyncImage(
                model = forecast.iconUrl,
                contentDescription = "Weather Icon",
                modifier = Modifier.size(48.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = forecast.tempMax,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = forecast.tempMin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Text(
                text = forecast.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            ForecastDetailRow(iconRes = R.drawable.ic_humidity, value = forecast.humidity, tint = tintColor)
            ForecastDetailRow(iconRes = R.drawable.ic_wind, value = forecast.windSpeed, tint = tintColor)
            ForecastDetailRow(iconRes = R.drawable.ic_precipitation, value = forecast.precipitation, tint = tintColor)
        }
    }
}

@Composable
fun ForecastDetailRow(iconRes: Int, value: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

// EFEK RADAR / RIPPLE
@Composable
fun RadarRippleEffect(color: Color) {
    Box(contentAlignment = Alignment.Center) {
        RadarWave(color = color, delay = 0)
        RadarWave(color = color, delay = 1000)
        RadarWave(color = color, delay = 2000)
    }
}

@Composable
fun RadarWave(color: Color, delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = delay, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = delay, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

// FLOATING BUBBLE
@Composable
fun FloatingBubble(
    title: String,
    value: String,
    unit: String,
    bubbleColor: Color,
    textColor: Color,
    size: Dp,
    initialAlign: BiasAlignment,
    moveRange: Float,
    speedMillis: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")

    val dy by infiniteTransition.animateFloat(
        initialValue = -moveRange,
        targetValue = moveRange,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dy"
    )

    val dx by infiniteTransition.animateFloat(
        initialValue = -(moveRange / 2),
        targetValue = (moveRange / 2),
        animationSpec = infiniteRepeatable(
            animation = tween((speedMillis * 1.2).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dx"
    )

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = initialAlign
    ) {
        Card(
            modifier = Modifier
                .size(size)
                .offset(x = dx.dp, y = dy.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f)
                    )
                )

                Text(
                    text = value,
                    style = TextStyle(
                        fontSize = if(value.length > 3) 20.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    ),
                    modifier = Modifier.padding(vertical = 0.dp)
                )

                Text(
                    text = unit,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

// AQI DIALOG
@Composable
fun AqiScaleGuideDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Panduan Skala AQI", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AqiScaleRow(color = Color(0xFF00C853), range = "0-50", category = "Baik")
                AqiScaleRow(color = Color(0xFFFFAB00), range = "51-100", category = "Sedang")
                AqiScaleRow(color = Color(0xFFFF6D00), range = "101-150", category = "Tidak Sehat (Sensitif)")
                AqiScaleRow(color = Color(0xFFFF3D00), range = "151-200", category = "Tidak Sehat")
                AqiScaleRow(color = Color(0xFF6A1B9A), range = "201-300", category = "Sangat Tidak Sehat")
                AqiScaleRow(color = Color(0xFFB71C1C), range = "301+", category = "Berbahaya")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("TUTUP")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AqiScaleRow(color: Color, range: String, category: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color, CircleShape)
                .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = range,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        Text(category, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ForecastDetailDialog(forecast: DailyForecast, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Detail Ramalan Cuaca",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = forecast.day,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = forecast.iconUrl,
                    contentDescription = "Weather Icon",
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = forecast.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Max", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = forecast.tempMax,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Min", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = forecast.tempMin,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Detail Tambahan
                ForecastDetailRow(
                    iconRes = R.drawable.ic_humidity,
                    value = "Kelembaban: ${forecast.humidity}",
                    tint = Color.DarkGray
                )
                ForecastDetailRow(
                    iconRes = R.drawable.ic_wind,
                    value = "Kecepatan Angin: ${forecast.windSpeed}",
                    tint = Color.DarkGray
                )
                ForecastDetailRow(
                    iconRes = R.drawable.ic_precipitation,
                    value = "Curah Hujan: ${forecast.precipitation}",
                    tint = Color.DarkGray
                )
                ForecastDetailRow(
                    iconRes = R.drawable.ic_pressure,
                    value = "Tekanan: ${forecast.pressure}",
                    tint = Color.DarkGray
                )
                ForecastDetailRow(
                    iconRes = R.drawable.ic_uv,
                    value = "Indeks UV: ${forecast.uvIndex}",
                    tint = Color.DarkGray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TUTUP", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
