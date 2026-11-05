package com.example.model

enum class AppScreen {
    HOME,
    EDITOR,
    SETTINGS
}

enum class AppUiMode(val labelPt: String, val descriptionPt: String) {
    MOBILE(
        "Aparência Mobile",
        "Otimizada para telas de toque com botões ao alcance do polegar e timeline inferior deslizável."
    ),
    PC(
        "Aparência PC (Desktop)",
        "Interface clássica do Kodanima2D de computador: barra de menus superior, painéis laterais de camadas e ferramentas, suporte a atalhos de teclado e mesa digitalizadora."
    )
}

enum class TimelineLayoutMode(val id: Int, val labelPt: String, val descriptionPt: String) {
    HERITAGE_JUNIOR(
        0,
        "Heritage Junior",
        "Timeline pequena, compacta e centralizada no rodapé como no clássico do Windows."
    ),
    HERITAGE_PROFISSIONAL(
        1,
        "Heritage Profissional",
        "Timeline de ponta a ponta (vai de um canto ao outro da tela) com mini-ícones compactos."
    )
}

enum class KodanimaTheme(
    val id: Int,
    val namePt: String,
    val bg: Long,
    val panel: Long,
    val accent: Long,
    val text: Long
) {
    ROXO_BLACK(0, "Roxo Black (Padrão)", 0xFF140A20, 0xFF1D162D, 0xFF987EDC, 0xFFEDE7F8),
    PINK_VINHO(1, "Pink Vinho", 0xFF2D1927, 0xFF372330, 0xFFBE699A, 0xFFF4E6EF),
    BRANCO_CINZA_AZULADO(2, "Branco com Cinza Azulado", 0xFFEEEEEE, 0xFFF6F7FB, 0xFF4E6FAD, 0xFF222831),
    LARANJA_AMADEIRADO(3, "Laranja Amadeirado", 0xFF2B221B, 0xFF392C22, 0xFFE09B5F, 0xFFF7E9D8);

    companion object {
        fun fromId(id: Int): KodanimaTheme = values().firstOrNull { it.id == id } ?: ROXO_BLACK
    }
}

enum class PressureCurve(val labelPt: String, val exponent: Float) {
    SOFT("Macio (Alta resposta)", 0.6f),
    LINEAR("Linear (Padrão)", 1.0f),
    FIRM("Firme (Mais controle)", 1.6f)
}

data class TabletDriverSettings(
    val pressureCurve: PressureCurve = PressureCurve.LINEAR,
    val enableStylusHover: Boolean = true,
    val autoDetectStylusEraser: Boolean = true,
    val palmRejectionStrict: Boolean = true,
    val keyboardShortcutsEnabled: Boolean = true,
    val mouseScrollZoomEnabled: Boolean = true
)

data class AppPreferences(
    val uiMode: AppUiMode = AppUiMode.MOBILE,
    val theme: KodanimaTheme = KodanimaTheme.ROXO_BLACK,
    val timelineLayoutMode: TimelineLayoutMode = TimelineLayoutMode.HERITAGE_PROFISSIONAL,
    val tabletSettings: TabletDriverSettings = TabletDriverSettings(),
    val autoSaveIntervalSeconds: Int = 30,
    val showFpsCounter: Boolean = true
)
