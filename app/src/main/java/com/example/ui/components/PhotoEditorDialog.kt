package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

enum class AiPhotoStyle(val label: String, val promptDescription: String, val iconName: String) {
    ORIGINAL("Original", "Preserve natural photo without transformation", "photo"),
    FUTURISTIC_NEON("Futuristic Neon", "Neon cyan and magenta cyberpunk lighting with glowing visor", "flash_on"),
    CYBERPUNK("Cyberpunk", "High-tech augmented reality implants and neon rain reflections", "memory"),
    ANIME("Anime-Inspired", "Solo Leveling manhwa high-contrast hand-drawn aesthetic", "brush"),
    GAMING("Gaming HUD", "Sci-fi tactical interface display and energy shield aura", "sports_esports"),
    CINEMATIC("Cinematic", "Dramatic rim lighting, dark volumetric fog, and lens flare", "movie"),
    FANTASY_SCI_FI("Fantasy / Sci-Fi", "Monarch purple shadow mist and awakened sovereign eyes", "auto_awesome"),
    MINIMALIST("Minimalist", "Monochrome studio lighting with clean sharp shadows", "contrast"),
    PROFESSIONAL("Professional", "Polished association executive hunter portrait", "badge"),
    NATURE("Nature Hunter", "Emerald wilderness camouflage and forest gate lighting", "forest")
}

@Composable
fun PhotoEditorDialog(
    initialUri: Uri? = null,
    isCoverPhoto: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (uri: String, selectedStyle: String, prompt: String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var selectedStyle by remember { mutableStateOf(AiPhotoStyle.ORIGINAL) }
    var customPrompt by remember { mutableStateOf("") }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var isProcessingAi by remember { mutableStateOf(false) }
    var showAiConsentDialog by remember { mutableStateOf(false) }

    // Android Native Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            rotationAngle = 0f
            zoomScale = 1f
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCoverPhoto) "COVER PHOTO WORKSHOP" else "HUNTER AVATAR STUDIO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Gallery import, crop, zoom & AI styling",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Preview Frame
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCoverPhoto) 160.dp else 220.dp),
                            shape = if (isCoverPhoto) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(if (isCoverPhoto) RoundedCornerShape(12.dp) else RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .rotate(rotationAngle)
                                            .scale(zoomScale),
                                        contentScale = if (isCoverPhoto) ContentScale.Crop else ContentScale.Fit
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No Photo Selected",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                // Active AI Style Badge Indicator
                                if (selectedStyle != AiPhotoStyle.ORIGINAL) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "AI: ${selectedStyle.label}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gallery Selection Button
                    item {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedUri == null) "Select from Device Gallery" else "Choose Different Photo",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Zoom & Rotate Controls
                    if (selectedUri != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ZOOM & ROTATION",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        IconButton(
                                            onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RotateRight,
                                                contentDescription = "Rotate 90°",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Slider(
                                            value = zoomScale,
                                            onValueChange = { zoomScale = it },
                                            valueRange = 0.8f..2.5f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // AI Photo & Avatar Styling Presets
                    item {
                        Text(
                            text = "AI STYLING PRESETS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AiPhotoStyle.entries) { style ->
                                val isSel = selectedStyle == style
                                Card(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            selectedStyle = style
                                            if (style != AiPhotoStyle.ORIGINAL) {
                                                showAiConsentDialog = true
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(
                                        if (isSel) 2.dp else 1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = when (style) {
                                                AiPhotoStyle.ORIGINAL -> Icons.Default.Photo
                                                AiPhotoStyle.FUTURISTIC_NEON -> Icons.Default.FlashOn
                                                AiPhotoStyle.CYBERPUNK -> Icons.Default.Memory
                                                AiPhotoStyle.ANIME -> Icons.Default.Brush
                                                AiPhotoStyle.GAMING -> Icons.Default.SportsEsports
                                                AiPhotoStyle.CINEMATIC -> Icons.Default.Movie
                                                AiPhotoStyle.FANTASY_SCI_FI -> Icons.Default.AutoAwesome
                                                AiPhotoStyle.MINIMALIST -> Icons.Default.Contrast
                                                AiPhotoStyle.PROFESSIONAL -> Icons.Default.Badge
                                                AiPhotoStyle.NATURE -> Icons.Default.Forest
                                            },
                                            contentDescription = null,
                                            tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = style.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom AI Prompt Field
                    if (selectedStyle != AiPhotoStyle.ORIGINAL) {
                        item {
                            Column {
                                Text(
                                    text = "CUSTOM AI PROMPT (OPTIONAL)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = { customPrompt = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("e.g. Add glowing monarch violet eyes and dark aura", fontSize = 11.sp) },
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val uriStr = selectedUri?.toString() ?: ""
                            onSave(uriStr, selectedStyle.name, customPrompt)
                            onDismiss()
                        },
                        enabled = selectedUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // AI Consent Disclosure Dialog
    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { showAiConsentDialog = false },
            title = {
                Text("AI Photo Transformation Consent", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your photo will be processed with Gemini AI styling (${selectedStyle.label}) on secure backend infrastructure. Images are never used for model training and are encrypted in transit and at rest.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAiConsentDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("I Consent", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedStyle = AiPhotoStyle.ORIGINAL
                    showAiConsentDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
