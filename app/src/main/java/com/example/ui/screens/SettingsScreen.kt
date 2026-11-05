package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.KodanimaGraphicsEngine
import com.example.model.AppScreen
import com.example.model.AppUiMode
import com.example.model.PressureCurve
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.KodanimaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: KodanimaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs = uiState.preferences
    val tabletSettings = prefs.tabletSettings

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = DarkSurface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Configurações do Kodanima 2D",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Aparência, Mesa Digitalizadora, Teclado e Motor",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Appearance Mode (Mobile vs PC)
            SectionHeader(title = "Aparência da Interface", subtitle = "Escolha o layout ideal para o seu dispositivo")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mobile Mode Card
                val isMobile = prefs.uiMode == AppUiMode.MOBILE
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isMobile) 2.dp else 1.dp,
                            color = if (isMobile) CyanAccent else BorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.setUiMode(AppUiMode.MOBILE) }
                        .testTag("mode_mobile_card"),
                    color = if (isMobile) DarkSurfaceHighlight else DarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMobile) CyanAccent else DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isMobile) Color.Black else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aparência Mobile",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMobile) CyanAccent else TextPrimary
                                )
                                if (isMobile) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Controles táteis compactos com barra de ferramentas flutuante, timeline horizontal deslizável e navegação ergonômica com uma mão.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // PC Desktop Mode Card
                val isPc = prefs.uiMode == AppUiMode.PC
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isPc) 2.dp else 1.dp,
                            color = if (isPc) CyanAccent else BorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.setUiMode(AppUiMode.PC) }
                        .testTag("mode_pc_card"),
                    color = if (isPc) DarkSurfaceHighlight else DarkSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPc) CyanAccent else DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DesktopWindows,
                                contentDescription = null,
                                tint = if (isPc) Color.Black else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aparência PC (Desktop Studio)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPc) CyanAccent else TextPrimary
                                )
                                if (isPc) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mesma interface do Kodanima2D de computador: barra de menus clássica (Arquivo, Editar, Camadas), doca lateral fixa com atalhos, painéis acopláveis e suporte a mesa digitalizadora.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Timeline Mode: Heritage Junior vs Heritage Profissional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isJr = prefs.timelineLayoutMode == com.example.model.TimelineLayoutMode.HERITAGE_JUNIOR
                    val isPro = prefs.timelineLayoutMode == com.example.model.TimelineLayoutMode.HERITAGE_PROFISSIONAL

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (isJr) CyanAccent else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setTimelineLayoutMode(com.example.model.TimelineLayoutMode.HERITAGE_JUNIOR) },
                        color = if (isJr) DarkSurfaceHighlight else DarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Heritage Junior", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isJr) CyanAccent else TextPrimary)
                            Text("Timeline flutuante compacta", fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (isPro) CyanAccent else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setTimelineLayoutMode(com.example.model.TimelineLayoutMode.HERITAGE_PROFISSIONAL) },
                        color = if (isPro) DarkSurfaceHighlight else DarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Heritage Profissional", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isPro) CyanAccent else TextPrimary)
                            Text("Timeline de ponta a ponta", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }

            // Section 1.5: Temas Oficiais do Kodanima 2D
            SectionHeader(
                title = "Temas da Interface",
                subtitle = "Selecione o esquema de cores padrão ou temas clássicos do Kodanima 2D"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.model.KodanimaTheme.values().forEach { themeOption ->
                    val isSelected = prefs.theme == themeOption
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(themeOption.accent) else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setTheme(themeOption) }
                            .testTag("theme_card_${themeOption.name.lowercase()}"),
                        color = if (isSelected) Color(themeOption.panel) else DarkSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Theme Color Palette Preview Swatches
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(themeOption.bg))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(themeOption.panel))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(themeOption.accent))
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = themeOption.namePt,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(themeOption.accent) else TextPrimary
                                    )
                                    Text(
                                        text = when (themeOption) {
                                            com.example.model.KodanimaTheme.ROXO_BLACK -> "Fundo preto arroxeado com realces em violeta elétrico (Padrão)"
                                            com.example.model.KodanimaTheme.PINK_VINHO -> "Paleta profunda de vinho ameixa com realces rosa pastel"
                                            com.example.model.KodanimaTheme.BRANCO_CINZA_AZULADO -> "Interface clara de estúdio com cinza azulado suave"
                                            com.example.model.KodanimaTheme.LARANJA_AMADEIRADO -> "Tons quentes de madeira e acabamento âmbar clássico"
                                        },
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selecionado",
                                    tint = Color(themeOption.accent),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Tablet & Stylus Driver Compatibility
            SectionHeader(
                title = "Mesa Digitalizadora & Stylus",
                subtitle = "Drivers compatíveis com Wacom, XP-Pen, Huion, Samsung S-Pen e Xiaomi Smart Pen"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Pressure Curve
                    Text("Curva de Pressão da Caneta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PressureCurve.values().forEach { curve ->
                            val isSelected = tabletSettings.pressureCurve == curve
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else DarkSurfaceVariant)
                                    .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.updateTabletSettings(tabletSettings.copy(pressureCurve = curve))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = curve.labelPt.substringBefore(" ("),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    // Strict Palm Rejection
                    SettingToggleRow(
                        title = "Rejeição Rigorosa de Palma",
                        subtitle = "Desenha apenas quando a ponta da caneta stylus toca a tela",
                        checked = tabletSettings.palmRejectionStrict,
                        onCheckedChange = {
                            viewModel.updateTabletSettings(tabletSettings.copy(palmRejectionStrict = it))
                        }
                    )

                    // Auto Detect Eraser
                    SettingToggleRow(
                        title = "Detecção de Borracha da Caneta",
                        subtitle = "Alterna para a borracha automaticamente com a ponta traseira ou botão lateral",
                        checked = tabletSettings.autoDetectStylusEraser,
                        onCheckedChange = {
                            viewModel.updateTabletSettings(tabletSettings.copy(autoDetectStylusEraser = it))
                        }
                    )

                    // Stylus Hover Cursor
                    SettingToggleRow(
                        title = "Indicador de Ponta (Stylus Hover)",
                        subtitle = "Exibe o ponto do pincel ao aproximar a caneta da mesa",
                        checked = tabletSettings.enableStylusHover,
                        onCheckedChange = {
                            viewModel.updateTabletSettings(tabletSettings.copy(enableStylusHover = it))
                        }
                    )
                }
            }

            // Section 3: Keyboard & Mouse Support
            SectionHeader(
                title = "Teclado e Mouse",
                subtitle = "Suporte plug-and-play para teclado e mouse USB / Bluetooth"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingToggleRow(
                        title = "Atalhos de Teclado Físico",
                        subtitle = "B (Caneta), E (Borracha), G (Balde), Espaço (Play), Ctrl+Z (Desfazer), Ctrl+S (Salvar)",
                        checked = tabletSettings.keyboardShortcutsEnabled,
                        onCheckedChange = {
                            viewModel.updateTabletSettings(tabletSettings.copy(keyboardShortcutsEnabled = it))
                        }
                    )

                    SettingToggleRow(
                        title = "Zoom com a Roda do Mouse (Scroll)",
                        subtitle = "Girar a roda do mouse aproxima ou afasta a área de desenho",
                        checked = tabletSettings.mouseScrollZoomEnabled,
                        onCheckedChange = {
                            viewModel.updateTabletSettings(tabletSettings.copy(mouseScrollZoomEnabled = it))
                        }
                    )
                }
            }

            // Section 4: Engine Status (Vulkan / OpenGL ES 3.2)
            SectionHeader(title = "Motor Gráfico Acelerado", subtitle = "Substituição do DirectX Windows para Android")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                color = DarkSurface
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Pipeline Skia Hardware (Vulkan / OpenGL ES)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "Double Buffering Ativo • Anti-Aliasing MSAA Ativo • 60/120 FPS",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Compatível com todas as GPUs Adreno, Mali e PowerVR",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanAccent,
                checkedTrackColor = CyanAccent.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceHighlight
            )
        )
    }
}
