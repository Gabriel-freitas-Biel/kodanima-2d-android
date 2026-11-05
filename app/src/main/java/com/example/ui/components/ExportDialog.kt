package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ExportState

@Composable
fun ExportDialog(
    exportState: ExportState,
    onExportMp4: (() -> Unit)? = null,
    onExportGif: () -> Unit,
    onExportSpriteSheet: () -> Unit,
    onExportPngFrame: () -> Unit,
    onExportFramePackZip: (() -> Unit)? = null,
    onExportProjectFile: () -> Unit,
    onDismissState: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("export_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exportar Animação",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status message / progress
                when (exportState) {
                    is ExportState.Progress -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = exportState.message,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { exportState.percent },
                                color = CyanAccent,
                                trackColor = DarkSurfaceHighlight,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is ExportState.Success -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = exportState.message,
                                    color = NeonGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = exportState.filename,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is ExportState.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OrangeAccent.copy(alpha = 0.15f))
                                .border(1.dp, OrangeAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = OrangeAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = exportState.error,
                                color = OrangeAccent,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    ExportState.Idle -> { /* Idle */ }
                }

                // Export Options List
                if (onExportMp4 != null) {
                    ExportOptionCard(
                        icon = Icons.Default.Animation,
                        title = "Vídeo MP4 (Hardware H.264 AVC)",
                        subtitle = "Vídeo de alta qualidade renderizado por hardware com áudio da timeline",
                        accentColor = CyanAccent,
                        onClick = onExportMp4
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                ExportOptionCard(
                    icon = Icons.Default.Animation,
                    title = "GIF Animado",
                    subtitle = "Animação em loop pronta para WhatsApp, Discord e redes",
                    accentColor = NeonPink,
                    onClick = onExportGif
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExportOptionCard(
                    icon = Icons.Default.Image,
                    title = "Quadro Atual (PNG)",
                    subtitle = "Salvar quadro atual com transparência ou fundo branco",
                    accentColor = OrangeAccent,
                    onClick = onExportPngFrame
                )

                if (onExportFramePackZip != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExportOptionCard(
                        icon = Icons.Default.GridOn,
                        title = "Pacote de Imagens (ZIP)",
                        subtitle = "Arquivo compactado com todos os quadros renderizados individualmente",
                        accentColor = NeonGreen,
                        onClick = onExportFramePackZip
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExportOptionCard(
                    icon = Icons.Default.GridOn,
                    title = "Sprite Sheet (PNG)",
                    subtitle = "Grade com todos os quadros para motores de jogos (Godot, Unity)",
                    accentColor = NeonGreen,
                    onClick = onExportSpriteSheet
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExportOptionCard(
                    icon = Icons.Default.Description,
                    title = "Projeto Kodanima (.kodanima)",
                    subtitle = "Salvar arquivo de projeto editável com camadas e histórico",
                    accentColor = Color(0xFFB388FF),
                    onClick = onExportProjectFile
                )
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
