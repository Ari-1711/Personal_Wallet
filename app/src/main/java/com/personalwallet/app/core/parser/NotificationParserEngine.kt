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
        val parser = parsers.find { it.targetPackageName == packageName } ?: return null
        return parser.parse(title, text)
    }
}
