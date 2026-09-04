package com.personalwallet.app.core.parser

import com.personalwallet.app.core.parser.rules.GoPayParserRule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationParserEngine @Inject constructor() {
    
    // Daftar semua aturan regex aktif
    private val parsers: List<NotificationParser> = listOf(
        GoPayParserRule()
        // Anda bisa menambahkan BCA, DANA, dll. di sini nantinya
    )

    fun parse(packageName: String, title: String, text: String): ParsedTransaction? {
        // Cari parser yang sesuai dengan packageName notifikasi
        val parser = parsers.find { it.targetPackageName == packageName } ?: return null
        return parser.parse(title, text)
    }
}
