package com.personalwallet.app.core.parser

import com.personalwallet.app.core.parser.rules.BcaParserRule
import com.personalwallet.app.core.parser.rules.DanaParserRule
import com.personalwallet.app.core.parser.rules.GoPayParserRule
import com.personalwallet.app.core.parser.rules.ShopeePayParserRule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationParserEngine @Inject constructor() {
    
    // Daftar semua aturan regex aktif untuk e-wallet & m-banking Indonesia
    private val parsers: List<NotificationParser> = listOf(
        GoPayParserRule(),
        BcaParserRule(),
        DanaParserRule(),
        ShopeePayParserRule()
    )

    fun parse(packageName: String, title: String, text: String): ParsedTransaction? {
        // DUKUNGAN UNTUK SIMULASI ADB TERMINAL (Package com.android.shell atau android)
        // Jika notifikasi dikirim via command ADB shell, uji terhadap semua parser yang ada
        if (packageName == "com.android.shell" || packageName == "android" || packageName.contains("shell")) {
            for (parser in parsers) {
                val result = parser.parse(title, text)
                if (result != null) return result
            }
        }

        // Jalur Produksi Resmi: Cari parser spesifik berdasarkan packageName
        val parser = parsers.find { it.targetPackageName == packageName } ?: return null
        return parser.parse(title, text)
    }
}
