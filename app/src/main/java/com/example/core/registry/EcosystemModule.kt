package com.example.core.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

enum class ModuleLayer(val titleFa: String) {
    USER_INTERFACE("لایه تعاملی و کاربری"),
    INTELLIGENCE("لایه هوش مصنوعی و ایجنت"),
    PROTOCOL_BRIDGE("لایه پروتکل و اتصال"),
    INFRASTRUCTURE("لایه هسته و زیرساخت")
}

enum class ModuleExecutionMode(val badgeFa: String) {
    NATIVE_ACTIVE("نیتیو فعال"),
    REMOTE_EXTENSION_READY("آماده اتصال ریموت")
}

data class YasinModuleDescriptor(
    val id: String,
    val codeName: String,
    val titleFa: String,
    val roleFa: String,
    val phase2ContractFa: String,
    val layer: ModuleLayer,
    val executionMode: ModuleExecutionMode,
    val icon: ImageVector,
    val targetRoute: String? = null
)

object Routes {
    const val HUB = "hub"
    const val AI_AGENT = "ai_agent"
    const val PRESS = "press"
    const val DOCS_MCP = "docs_mcp"
    const val RELAY_SETTINGS = "relay_settings"
}

object YasinModuleRegistry {
    val modules: List<YasinModuleDescriptor> = listOf(
        YasinModuleDescriptor(
            id = "yasin_hub",
            codeName = "Yasin Hub",
            titleFa = "یاسین هاب",
            roleFa = "مرکز فرمان، ناوبری یکپارچه و رجیستری پلاگین‌پذیر تمام ماژول‌های اکوسیستم.",
            phase2ContractFa = "آماده میزبانی تب‌ها و ویجت‌های جدید ماژولار در نسخه‌های بعدی.",
            layer = ModuleLayer.USER_INTERFACE,
            executionMode = ModuleExecutionMode.NATIVE_ACTIVE,
            icon = Icons.Outlined.Hub,
            targetRoute = Routes.HUB
        ),
        YasinModuleDescriptor(
            id = "yasin_ai",
            codeName = "Yasin AI",
            titleFa = "یاسین هوش",
            roleFa = "گفتگوی هوشمند استریمینگ با Gemini از طریق Firebase AI Logic بدون نیاز به کلید API.",
            phase2ContractFa = "مبتنی بر واسط AiGateway؛ قابلیت افزودن مدل‌های سرور ریموت در فاز ۲.",
            layer = ModuleLayer.INTELLIGENCE,
            executionMode = ModuleExecutionMode.NATIVE_ACTIVE,
            icon = Icons.Outlined.AutoAwesome,
            targetRoute = Routes.AI_AGENT
        ),
        YasinModuleDescriptor(
            id = "yasin_agent",
            codeName = "Yasin Agent",
            titleFa = "یاسین ایجنت",
            roleFa = "اجرای خودکار دستورات ساختاریافته روی ماژول‌های گوشی (ساخت پیش‌نویس در Press و بررسی مخازن).",
            phase2ContractFa = "آماده اتصال به ابزارهای سروری Yasin-MCP از طریق Streamable HTTP.",
            layer = ModuleLayer.INTELLIGENCE,
            executionMode = ModuleExecutionMode.NATIVE_ACTIVE,
            icon = Icons.Outlined.SmartToy,
            targetRoute = Routes.AI_AGENT
        ),
        YasinModuleDescriptor(
            id = "yasin_press",
            codeName = "Yasin Press",
            titleFa = "یاسین پرس",
            roleFa = "استودیوی بومی و آفلاین تولید، دسته‌بندی، ویرایش با AI و اشتراک‌گذاری نوشته‌ها با دیتابیس Room.",
            phase2ContractFa = "واسط PressRepository آماده همگام‌سازی ابری و انتشار خودکار روی سرور.",
            layer = ModuleLayer.USER_INTERFACE,
            executionMode = ModuleExecutionMode.NATIVE_ACTIVE,
            icon = Icons.Outlined.Newspaper,
            targetRoute = Routes.PRESS
        ),
        YasinModuleDescriptor(
            id = "yasin_mcp",
            codeName = "Yasin MCP",
            titleFa = "یاسین MCP و اسناد",
            roleFa = "بازرسی زنده مخازن گیت‌هاب اکوسیستم، خواندن README و تحلیل فنی پروژه‌ها با هوش مصنوعی.",
            phase2ContractFa = "واسط EcosystemInspectorRepository آماده سوییچ به اندپوینت Streamable HTTP.",
            layer = ModuleLayer.PROTOCOL_BRIDGE,
            executionMode = ModuleExecutionMode.NATIVE_ACTIVE,
            icon = Icons.Outlined.Description,
            targetRoute = Routes.DOCS_MCP
        ),
        YasinModuleDescriptor(
            id = "yasin_relay",
            codeName = "Yasin Relay",
            titleFa = "یاسین ریلی",
            roleFa = "دروازه ارتباطی با سرور ریموت (Render / Hugging Face / Tunnel) و تست سلامت اتصال.",
            phase2ContractFa = "پیکربندی آدرس و تست زنده در دسترس است؛ استریم WebSocket/SSE در فاز ۲ فعال می‌شود.",
            layer = ModuleLayer.PROTOCOL_BRIDGE,
            executionMode = ModuleExecutionMode.REMOTE_EXTENSION_READY,
            icon = Icons.Outlined.CloudSync,
            targetRoute = Routes.RELAY_SETTINGS
        ),
        YasinModuleDescriptor(
            id = "yasin_core",
            codeName = "YasinCore",
            titleFa = "یاسین کُر",
            roleFa = "هسته قوانین مرکزی و همگام‌سازی وضعیت کلی اکوسیستم بین سرور و کلاینت‌های موبایل.",
            phase2ContractFa = "از طریق قرارداد RelayGateway به داشبورد مرکزی یاسین‌هاب متصل می‌شود.",
            layer = ModuleLayer.INFRASTRUCTURE,
            executionMode = ModuleExecutionMode.REMOTE_EXTENSION_READY,
            icon = Icons.Outlined.Memory,
            targetRoute = Routes.RELAY_SETTINGS
        ),
        YasinModuleDescriptor(
            id = "yasin_cli",
            codeName = "Yasin CLI",
            titleFa = "یاسین CLI",
            roleFa = "ارسال ایمن دستورات خط فرمان از طریق پالت دستورات موبایل به هاست/سرور ریموت.",
            phase2ContractFa = "نیازمند سرور میزبان یا Termux Bridge؛ رابط Command Palette در فاز ۲ اضافه می‌شود.",
            layer = ModuleLayer.INFRASTRUCTURE,
            executionMode = ModuleExecutionMode.REMOTE_EXTENSION_READY,
            icon = Icons.Outlined.Terminal,
            targetRoute = Routes.RELAY_SETTINGS
        ),
        YasinModuleDescriptor(
            id = "yasin_operation",
            codeName = "Yasin Operation",
            titleFa = "عملیات یاسین",
            roleFa = "مانیتورینگ سلامت سرویس‌ها، مشاهده لاگ‌های زنده و اجرای اکشن‌های عملیاتی سرور.",
            phase2ContractFa = "از طریق اندپوینت Diagnostics در سرور ریموت به اپ متصل خواهد شد.",
            layer = ModuleLayer.INFRASTRUCTURE,
            executionMode = ModuleExecutionMode.REMOTE_EXTENSION_READY,
            icon = Icons.Outlined.SettingsEthernet,
            targetRoute = Routes.RELAY_SETTINGS
        )
    )
}
