package com.example.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.SceneBlueprint
import com.example.data.ProjectEntity
import java.io.File

// Core Color Palette: Elegant Dark (Material 3 Dark Purple & Charcoal)
val SpaceDarkBg = Color(0xFF0F0F0F)
val SpaceCardBg = Color(0xFF1C1B1F)
val CosmicCyan = Color(0xFFD0BCFF) // Main lavender accent
val CosmicOrange = Color(0xFFF2B8B5) // M3 warm error/warning soft coral color
val DeepViolet = Color(0xFF4F378B) // Dark theme deep purple
val TextLight = Color(0xFFE6E1E5)
val TextMuted = Color(0xFF938F99)
val BorderColor = Color(0xFF49454F)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: AtmosForgeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val projectHistory by viewModel.projectHistory.collectAsStateWithLifecycle()

    val title by viewModel.titleState.collectAsStateWithLifecycle()
    val inputText by viewModel.inputTextState.collectAsStateWithLifecycle()
    val inputType by viewModel.inputTypeState.collectAsStateWithLifecycle()
    val durationMode by viewModel.durationModeState.collectAsStateWithLifecycle()
    val customDurationSeconds by viewModel.customDurationSecondsState.collectAsStateWithLifecycle()
    val intensity by viewModel.intensityState.collectAsStateWithLifecycle()
    val atmosphere by viewModel.atmosphereState.collectAsStateWithLifecycle()
    val voiceStyle by viewModel.voiceStyleState.collectAsStateWithLifecycle()
    val natureBlend by viewModel.natureBlendState.collectAsStateWithLifecycle()
    val fxBlend by viewModel.fxBlendState.collectAsStateWithLifecycle()
    val sampleRate by viewModel.sampleRateState.collectAsStateWithLifecycle()
    val enableTextVfx by viewModel.enableTextVfxState.collectAsStateWithLifecycle()
    val uploadedAudioPath by viewModel.uploadedAudioPathState.collectAsStateWithLifecycle()
    val uploadedAudioName by viewModel.uploadedAudioNameState.collectAsStateWithLifecycle()
    val uploadedAudioDuration by viewModel.uploadedAudioDurationState.collectAsStateWithLifecycle()
    val uploadedMixVolume by viewModel.uploadedMixVolumeState.collectAsStateWithLifecycle()
    val playbackOption by viewModel.playbackOptionState.collectAsStateWithLifecycle()

    val audioLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleUploadedAudioUri(context, uri) { path, name, durationSec ->
                viewModel.setUploadedAudio(path, name, durationSec)
            }
        }
    }

    // Media player states
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playProgress by viewModel.playProgress.collectAsStateWithLifecycle()
    val playDurationMs by viewModel.playDurationMs.collectAsStateWithLifecycle()
    val playCurrentPositionMs by viewModel.playCurrentPositionMs.collectAsStateWithLifecycle()

    var showHistorySection by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDarkBg)
    ) {
        // Outer glow canvas ambience background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x0A9F86FF), // Soft Purple mist
                radius = size.width * 1.2f,
                center = Offset(0f, 0f)
            )
            drawCircle(
                color = Color(0x064F378B), // Subtle Deep Violet glow
                radius = size.width * 0.9f,
                center = Offset(size.width, size.height * 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // BRAND HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = R.drawable.img_honeycomb_plant_1779304950968,
                        contentDescription = "Vocins App Icon",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Vocins AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = CosmicCyan,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "BY SOMSER ALI • TLOGZ.TOP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                IconButton(
                    onClick = { showHistorySection = !showHistorySection },
                    modifier = Modifier
                        .testTag("history_toggle_button")
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SpaceCardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = if (showHistorySection) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = "Project History",
                        tint = TextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Main Workspace
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO EXPLANATION BANNER
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(SpaceCardBg, Color(0xFF132431))
                                    )
                                )
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Procedural Audio Architect",
                                    color = CosmicCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Generate deep cinema voice ambience, natural environments, and motion sound design from story scripts. Completely device-native synthesis—no instruments, purely atmosphere.",
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // INPUT PANEL
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "NARRATIVE INPUTS",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Input Title Field
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { viewModel.titleState.value = it },
                                    label = { Text("Soundtrack Project Title") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = TextLight,
                                        focusedBorderColor = CosmicCyan,
                                        unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = CosmicCyan,
                                        unfocusedLabelColor = TextMuted
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Tabbed Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SpaceDarkBg)
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                        .padding(4.dp)
                                ) {
                                    val tabs = listOf("Script", "Story", "Timeline", "Subtitle")
                                    tabs.forEach { tab ->
                                        val selected = tab == inputType
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) DeepViolet else Color.Transparent)
                                                .clickable { viewModel.inputTypeState.value = tab }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tab,
                                                color = if (selected) Color(0xFFEADDFF) else Color(0xFFCAC4D0),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Main Textarea
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { viewModel.inputTextState.value = it },
                                    placeholder = { Text("Paste content matching tab above...") },
                                    minLines = 4,
                                    maxLines = 8,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = TextLight,
                                        focusedBorderColor = CosmicCyan,
                                        unfocusedBorderColor = BorderColor,
                                        focusedPlaceholderColor = TextMuted,
                                        unfocusedPlaceholderColor = TextMuted
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("narrative_input_field")
                                )
                            }
                        }
                    }

                    // SYNTHESIS CONTROLS / GENERATION PANEL
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "SYNTHESIS CHARACTER",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Voice style Choice
                                Text("Voice Atmosphere Style", color = TextLight, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val voiceStyles = listOf(
                                        "Male Humming",
                                        "Female Humming",
                                        "Mixed Choir",
                                        "Breath Atmosphere",
                                        "Ambient Voices"
                                    )
                                    voiceStyles.forEach { style ->
                                        val active = style == voiceStyle
                                        FilterChip(
                                            selected = active,
                                            onClick = { viewModel.voiceStyleState.value = style },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CosmicCyan.copy(0.12f),
                                                containerColor = SpaceDarkBg,
                                                labelColor = TextMuted,
                                                selectedLabelColor = CosmicCyan
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = active,
                                                borderColor = BorderColor,
                                                selectedBorderColor = CosmicCyan
                                            ),
                                            label = { Text(style, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Atmosphere Environment Selection
                                Text("Acoustic Space Environment", color = TextLight, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val atmospheres = listOf("Natural", "Spiritual", "Cinematic", "Dark", "Documentary")
                                    atmospheres.forEach { space ->
                                        val active = space == atmosphere
                                        FilterChip(
                                            selected = active,
                                            onClick = { viewModel.atmosphereState.value = space },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CosmicCyan.copy(0.12f),
                                                containerColor = SpaceDarkBg,
                                                labelColor = TextMuted,
                                                selectedLabelColor = CosmicCyan
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = active,
                                                borderColor = BorderColor,
                                                selectedBorderColor = CosmicCyan
                                            ),
                                            label = { Text(space, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Sliders Core: Nature & FX Blends
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Nature Bed Volume", color = TextLight, fontSize = 13.sp)
                                    Text("${natureBlend.toInt()}%", color = CosmicCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = natureBlend,
                                    onValueChange = { viewModel.natureBlendState.value = it },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CosmicCyan,
                                        activeTrackColor = CosmicCyan,
                                        inactiveTrackColor = Color(0xFF352F3F)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cinematic FX Blends", color = TextLight, fontSize = 13.sp)
                                    Text("${fxBlend.toInt()}%", color = CosmicCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = fxBlend,
                                    onValueChange = { viewModel.fxBlendState.value = it },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CosmicCyan,
                                        activeTrackColor = CosmicCyan,
                                        inactiveTrackColor = Color(0xFF352F3F)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Divider(color = BorderColor, thickness = 1.dp)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Intensity Choice
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dynamic Intensity", color = TextLight, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SpaceDarkBg)
                                            .padding(2.dp)
                                    ) {
                                        listOf("Low", "Medium", "High").forEach { level ->
                                            val active = level == intensity
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (active) DeepViolet else Color.Transparent)
                                                    .clickable { viewModel.intensityState.value = level }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    level,
                                                    fontSize = 11.sp,
                                                    color = if (active) Color.White else TextMuted,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Duration Configuration Toggles Note
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Soundscape Duration", color = TextLight, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SpaceDarkBg)
                                            .padding(2.dp)
                                    ) {
                                        listOf("Auto", "Custom").forEach { mode ->
                                            val active = mode == durationMode
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (active) DeepViolet else Color.Transparent)
                                                    .clickable { viewModel.durationModeState.value = mode }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    mode,
                                                    fontSize = 11.sp,
                                                    color = if (active) Color.White else TextMuted,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = durationMode == "Custom") {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Set Scale (sec)", color = TextMuted, fontSize = 11.sp)
                                            Text("$customDurationSeconds s", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = customDurationSeconds.toFloat(),
                                            onValueChange = { viewModel.customDurationSecondsState.value = it.toInt() },
                                            valueRange = 15f..120f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CosmicCyan,
                                                activeTrackColor = CosmicCyan,
                                                inactiveTrackColor = Color(0xFF352F3F)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sample Rate selector
                                
                                 
                                 Spacer(modifier = Modifier.height(14.dp))
                                 HorizontalDivider(color = BorderColor.copy(0.4f), thickness = 1.dp)
                                 Spacer(modifier = Modifier.height(14.dp))

                                 // Text-based VFX configuration toggle
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     verticalAlignment = Alignment.CenterVertically,
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                         Text("Smart Text-Based VFX", color = TextLight, fontSize = 13.sp)
                                         Text(
                                             text = "Adds footsteps, door creaks, wind contextually based on script keywords.", 
                                             color = TextMuted, 
                                             fontSize = 11.sp
                                         )
                                     }
                                     Switch(
                                         checked = enableTextVfx,
                                         onCheckedChange = { viewModel.enableTextVfxState.value = it },
                                         modifier = Modifier.testTag("enable_text_vfx_switch"),
                                         colors = SwitchDefaults.colors(
                                             checkedThumbColor = CosmicCyan,
                                             checkedTrackColor = DeepViolet,
                                             uncheckedThumbColor = TextMuted,
                                             uncheckedTrackColor = SpaceDarkBg
                                         )                                      )
                                  }

                                  Spacer(modifier = Modifier.height(14.dp))
                                  HorizontalDivider(color = BorderColor.copy(0.4f), thickness = 1.dp)
                                  Spacer(modifier = Modifier.height(14.dp))

                                  // Sample Rate selector Row
                                  Row(
                                      modifier = Modifier.fillMaxWidth(),
                                      verticalAlignment = Alignment.CenterVertically,
                                      horizontalArrangement = Arrangement.SpaceBetween
                                  ) {
                                      Text("Export Sample Rate", color = TextLight, fontSize = 13.sp)
                                      Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SpaceDarkBg)
                                            .padding(2.dp)
                                    ) {
                                        listOf(44100, 48000).forEach { rate ->
                                            val active = rate == sampleRate
                                            val rateLabel = if (rate == 44100) "44.1 kHz" else "48 kHz"
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (active) DeepViolet else Color.Transparent)
                                                    .clickable { viewModel.setSampleRate(rate) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    rateLabel,
                                                    fontSize = 11.sp,
                                                    color = if (active) Color.White else TextMuted,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // AUDIO UPLOAD & RECORD LAUNCHER CARD
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .border(1.dp, BorderColor.copy(0.4f), RoundedCornerShape(16.dp))
                                .testTag("audio_upload_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📥",
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "External Record / Audio Upload",
                                        color = CosmicCyan,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Upload a poem recitation, vocal track or custom recording to blend into your cinematic soundscapes.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                if (uploadedAudioPath == null) {
                                    // Empty state: clickable select field
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SpaceDarkBg)
                                            .clickable { audioLauncher.launch("audio/*") }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "🎵",
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Select Audio File (WAV, MP3, AAC)",
                                                color = TextLight,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                } else {
                                    // Active state: display track meta + volume controls
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SpaceDarkBg)
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🎵",
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = uploadedAudioName ?: "Selected Track",
                                                    color = TextLight,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearUploadedAudio() },
                                                modifier = Modifier.size(24.dp).testTag("clear_uploaded_audio")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Loaded Track",
                                                    tint = CosmicOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Length: ${uploadedAudioDuration ?: 0} seconds",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Blended Volume", color = TextLight, fontSize = 11.sp)
                                            Text("${uploadedMixVolume.toInt()}%", color = CosmicCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = uploadedMixVolume,
                                            onValueChange = { viewModel.uploadedMixVolumeState.value = it },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CosmicCyan,
                                                activeTrackColor = CosmicCyan,
                                                inactiveTrackColor = Color(0xFF352F3F)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TRIGGER GENERATE SOUNDSCAPE BUTTON
                    item {
                        Button(
                            onClick = { viewModel.startGenerationPipeline() },
                            enabled = uiState !is GenerationUiState.Analyzing && uiState !is GenerationUiState.Synthesizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("generate_soundscape_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicCyan,
                                contentColor = Color(0xFF381E72),
                                disabledContainerColor = CosmicCyan.copy(0.4f),
                                disabledContentColor = Color(0xFF381E72).copy(0.4f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (uiState is GenerationUiState.Analyzing || uiState is GenerationUiState.Synthesizing) {
                                CircularProgressIndicator(color = Color(0xFF381E72), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "FORGING SOUNDTRACK...",
                                    color = Color(0xFF381E72),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = "\u2728", // Sparkles icon
                                    fontSize = 18.sp,
                                    color = Color(0xFF381E72)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FORGE SOUNDTRACK",
                                    color = Color(0xFF381E72),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // STATE & RESULTS DISPLAY PANEL
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (val state = uiState) {
                                is GenerationUiState.Idle -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Awaiting narrative script in the editor.",
                                            color = TextMuted,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                is GenerationUiState.Analyzing -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            LinearProgressIndicator(
                                                color = CosmicCyan,
                                                trackColor = BorderColor,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "GEMINI COGNITIVE PIPELINE",
                                                color = CosmicCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = state.step,
                                                color = TextLight,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Constructing timeline scenes and ambient acoustics blueprint.",
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                is GenerationUiState.Synthesizing -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = CosmicOrange)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "DSP SYNTHESIS FORGE ACTIVE",
                                                color = CosmicOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Procedurally synthesizing ${state.blueprints.size} narrative scenes in 16-bit PCM Linear Mono. Mixing voice pads and LFO wind/rain tracks.",
                                                color = TextLight,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                                is GenerationUiState.BlueprintReady -> {
                                    // Audio blueprint generated but synthesis files deleted
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.startGenerationPipeline() },
                                            colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange)
                                        ) {
                                            Text("Acoustics Blueprint is ready. Click to Re-Synth WAV", color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        BlueprintSceneBreakdown(state.blueprints)
                                    }
                                }
                                is GenerationUiState.Ready -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // THE PLAYER CARD
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "SYNTHESIZED SOUNDTRACK",
                                                            color = CosmicCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Text(
                                                            text = title,
                                                            color = Color.White,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    // Sample rate tag
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .background(BorderColor)
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            if (sampleRate == 44100) "44.1kHz WAV" else "48kHz WAV",
                                                            fontSize = 10.sp,
                                                            color = TextLight,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                if (uploadedAudioPath != null) {
                                                    Text(
                                                        text = "PLAYBACK OUTPUT SELECTION",
                                                        color = TextMuted,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        listOf(
                                                            "blended" to "📥 With Uploaded Audio",
                                                            "pure" to "🎵 Pure Soundscape"
                                                        ).forEach { (opt, label) ->
                                                            val selected = playbackOption == opt
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(
                                                                        if (selected) CosmicCyan.copy(0.15f)
                                                                        else Color.Transparent
                                                                    )
                                                                    .border(
                                                                        1.dp,
                                                                        if (selected) CosmicCyan else BorderColor.copy(0.4f),
                                                                        RoundedCornerShape(8.dp)
                                                                    )
                                                                    .clickable { viewModel.setPlaybackOption(opt) }
                                                                    .padding(vertical = 10.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = label,
                                                                    color = if (selected) CosmicCyan else TextLight,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }

                                                // THE WAVEFORM CANVAS
                                                WaveformProgressView(
                                                    playProgress = playProgress,
                                                    onSeek = { viewModel.seekToPosition(it) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(64.dp)
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Timing numbers under progress
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        formatTimeMs(playCurrentPositionMs),
                                                        color = CosmicCyan,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        formatTimeMs(playDurationMs),
                                                        color = TextMuted,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // MEDIA PLAYBACK BUTTONS
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Move back button
                                                    Text(
                                                        text = "< 10s",
                                                        fontSize = 13.sp,
                                                        color = TextLight,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier
                                                            .clickable { viewModel.seekToPosition((playProgress - 0.1f).coerceIn(0f, 1f)) }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    FilledIconButton(
                                                        onClick = { viewModel.togglePlayback() },
                                                        modifier = Modifier.size(56.dp).testTag("master_play_button"),
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = CosmicCyan,
                                                            contentColor = Color.Black
                                                        )
                                                    ) {
                                                        Text(
                                                            text = if (isPlaying) "\u23F8" else "\u25B6", // Clean unicode Paues/Play
                                                            fontSize = 24.sp,
                                                            color = Color.Black,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    // Move forward button
                                                    Text(
                                                        text = "10s >",
                                                        fontSize = 13.sp,
                                                        color = TextLight,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier
                                                            .clickable { viewModel.seekToPosition((playProgress + 0.1f).coerceIn(0f, 1f)) }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Divider(color = BorderColor, thickness = 1.dp)

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // DOWNLOAD AND EXPORT OPTIONS
                                                Text(
                                                    "EXPORT TARGET PATHS",
                                                    color = TextMuted,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Shared app export triggers
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (uploadedAudioPath != null) {
                                                        ExportTrackRow("Blended Soundtrack (with Uploaded Ambient)", state.files["full"])
                                                        ExportTrackRow("Pure Soundtrack (Soundscape Only)", state.files["pure"])
                                                    } else {
                                                        ExportTrackRow("Full soundtrack", state.files["full"])
                                                    }
                                                    ExportTrackRow("Vocal layer", state.files["vocal"])
                                                    ExportTrackRow("Nature bed layer", state.files["nature"])
                                                    ExportTrackRow("Cinematic SFX layer", state.files["fx"])
                                                    ExportTrackRow("ZIP Export Package (Complete)", state.files["zip"], color = CosmicOrange)
                                                }
                                            }
                                        }

                                        // SCENE ANALYSIS BREAKDOWN
                                        BlueprintSceneBreakdown(state.blueprints)
                                    }
                                }
                                is GenerationUiState.Error -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C131A)),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFF8C2230), RoundedCornerShape(16.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, "Error", tint = Color(0xFFFF5252))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Forge Processing Error", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(state.message, color = TextLight, fontSize = 13.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // COLLAPSIBLE SIDE HISTORY HISTORY SIDE DRAWER
                AnimatedVisibility(
                    visible = showHistorySection,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .background(SpaceCardBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "FORGE ARCHIVES",
                                    color = CosmicCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (projectHistory.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearAllHistory() }) {
                                        Icon(Icons.Default.Delete, "Clear history", tint = CosmicOrange, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (projectHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Archives are empty.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(projectHistory) { historyProject ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SpaceDarkBg)
                                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                                .clickable { viewModel.selectProjectFromHistory(historyProject) }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    historyProject.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${historyProject.inputType} • ${historyProject.atmosphere}",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteProject(historyProject) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    "Delete project",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
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
    }
}

/**
 * Custom circular wave canvas rendering the uncompressed audio peaks.
 */
@Composable
fun WaveformProgressView(
    playProgress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generated static peaks values mapping the physical looks of sound tracks
    val peakHeights = remember {
        listOf(
            0.2f, 0.35f, 0.5f, 0.4f, 0.15f, 0.6f, 0.75f, 0.85f, 0.45f, 0.2f,
            0.1f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f, 0.5f, 0.3f, 0.15f, 0.45f,
            0.6f, 0.7f, 0.55f, 0.35f, 0.2f, 0.4f, 0.65f, 0.85f, 0.9f, 0.7f,
            0.5f, 0.3f, 0.45f, 0.6f, 0.8f, 0.75f, 0.55f, 0.35f, 0.15f, 0.25f
        )
    }

    val safeProgress = if (playProgress.isNaN() || playProgress.isInfinite()) 0f else playProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width
                    if (w > 0) {
                        val clickedProgress = (offset.x / w).coerceIn(0f, 1f)
                        if (!clickedProgress.isNaN() && !clickedProgress.isInfinite()) {
                            onSeek(clickedProgress)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width > 0f && height > 0f) {
                val numBars = peakHeights.size
                val barGap = 4.dp.toPx()
                val totalGapWidth = barGap * (numBars - 1)

                if (width > totalGapWidth) {
                    val barWidth = (width - totalGapWidth) / numBars
                    if (barWidth > 0f) {
                        for (i in 0 until numBars) {
                            val ratio = (i.toFloat() / numBars)
                            val active = ratio <= safeProgress

                            val barHeight = peakHeights[i] * height
                            val barColor = if (active) CosmicCyan else TextMuted.copy(0.35f)

                            val x = i * (barWidth + barGap)
                            val y = (height - barHeight) / 2f

                            // Draw solid bar rounded corners
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                            )
                        }
                    }
                }

                // Scrub physical play line indicator
                val scrubX = width * safeProgress
                if (!scrubX.isNaN() && !scrubX.isInfinite()) {
                    drawLine(
                        color = CosmicOrange,
                        start = Offset(scrubX, 0f),
                        end = Offset(scrubX, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun ExportTrackRow(
    label: String,
    file: File?,
    color: Color = CosmicCyan
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpaceDarkBg)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = file != null) {
                file?.let { triggerSystemShare(context, it) }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = if (file != null) color else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                label,
                color = if (file != null) TextLight else TextMuted,
                fontSize = 13.sp,
                fontWeight = if (label.contains("ZIP")) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (file != null) color.copy(0.12f) else BorderColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (file != null) "EXPORT" else "FORGING...",
                color = if (file != null) color else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BlueprintSceneBreakdown(scenesList: List<SceneBlueprint>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DETECTED TIMELINE SCENES",
                    color = CosmicCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF352F3F))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = String.format("%02d SCENES FOUND", scenesList.size),
                        color = CosmicCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            scenesList.forEachIndexed { idx, scene ->
                val cardBg = if (idx == 0) Color(0xFF2B2930) else Color(0xFF211F26)
                val isFirst = idx == 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .then(
                            if (isFirst) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = CosmicCyan.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            } else Modifier
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isFirst) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(CosmicCyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column {
                                    Text(
                                        text = "${scene.startTime} — ${scene.endTime}",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "The Narrative Scene ${scene.sceneNumber}",
                                        color = TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Ethereal status icon
                            Text(
                                text = if (isFirst) "\u2139" else "\u266B", // Ethereal info/music
                                fontSize = 16.sp,
                                color = if (isFirst) CosmicCyan else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Emotion badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = if (isFirst) 14.dp else 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SpaceDarkBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = scene.emotion.uppercase(),
                                    fontSize = 9.sp,
                                    color = CosmicCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SpaceDarkBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ENERGY: ${scene.energy.uppercase()}",
                                    fontSize = 9.sp,
                                    color = if (scene.energy == "high") CosmicOrange else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Layers Descriptions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = if (isFirst) 14.dp else 0.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpaceDarkBg.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LayerBlueprintRow(Icons.Default.Info, "Vocal Layer", scene.vocalLayer)
                            LayerBlueprintRow(Icons.Default.Info, "Nature Layer", scene.natureLayer)
                            LayerBlueprintRow(Icons.Default.Info, "Cinema FX", scene.fxLayer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayerBlueprintRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, content: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = CosmicCyan, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$label: $content", color = TextLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

// Helpers
private fun formatTimeMs(ms: Int): String {
    val secTotal = ms / 1000
    val min = secTotal / 60
    val sec = secTotal % 60
    return String.format("%02d:%02d", min, sec)
}

/**
 * Android system file sharing trigger utilizing standard modern FileProvider.
 * This completely matches professional standards and bypasses storage permission blocks.
 */
private fun triggerSystemShare(context: android.content.Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".zip")) "application/zip" else "audio/wav"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Vocins AI: Share Soundtrack"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Helper function to parse Uri, copy stream to cache, and query duration.
 */
private fun handleUploadedAudioUri(
    context: android.content.Context, 
    uri: Uri, 
    onProcessed: (String, String, Int) -> Unit
) {
    try {
        var name = "uploaded_audio.wav"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }

        // Copy content stream to standard secure cache dir
        val tempFile = java.io.File(
            context.cacheDir, 
            "temp_uploaded_${System.currentTimeMillis()}_${name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
        )
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Parse track duration using system retriever
        val retriever = android.media.MediaMetadataRetriever()
        var durationSec = 30 // Safe default representation
        try {
            retriever.setDataSource(context, Uri.fromFile(tempFile))
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr != null) {
                durationSec = (durationStr.toLong() / 1000).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        onProcessed(tempFile.absolutePath, name, durationSec)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
