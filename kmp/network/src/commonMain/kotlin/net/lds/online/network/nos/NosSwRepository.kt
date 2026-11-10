package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.lds.online.network.ApiException

/**
 * Web `/sw` area: left-menu sites + per-site device table.
 * Server renders HTML (no JSON list endpoint) — parse `#s_sw` and `/sw/{id}` table.
 */
class NosSwRepository(
    private val client: HttpClient,
) {
    suspend fun sites(): List<SwSite> {
        val body = client.get(NosEndpoints.SW).bodyAsText()
        if (isLoginHtml(body)) {
            throw ApiException.clientError(401, "Сессия истекла")
        }
        return parseSwSites(body)
    }

    suspend fun sitePage(id: Int): SwSitePage {
        val body = client.get(NosEndpoints.swSite(id)).bodyAsText()
        if (isLoginHtml(body)) {
            throw ApiException.clientError(401, "Сессия истекла")
        }
        return parseSwSitePage(id, body)
    }

    private fun isLoginHtml(body: String): Boolean =
        body.contains("Форма авторизации") ||
            body.contains("id=\"signin\"") ||
            body.contains("id='signin'")
}

internal fun parseSwSites(html: String): List<SwSite> {
    val menu = SITE_MENU_BLOCK.find(html)?.groupValues?.get(1) ?: html
    return SITE_LINK.findAll(menu).mapNotNull { match ->
        val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val name = decodeHtml(match.groupValues[2]).trim()
        SwSite(id = id, name = name)
    }.distinctBy { it.id }.toList()
}

internal fun parseSwSitePage(id: Int, html: String): SwSitePage {
    val title = SITE_TITLE.find(html)?.groupValues?.get(1)?.let(::decodeHtml)?.trim()
        .orEmpty()
        .ifBlank { "Узел $id" }
    val devices = TR_ROW.findAll(html).mapNotNull { match ->
        val rowClass = match.groupValues[1]
        val rowHtml = match.groupValues[2]
        parseDeviceRow(rowHtml, down = rowClass.contains("red"))
    }.toList()
    return SwSitePage(id = id, title = title, devices = devices)
}

private fun parseDeviceRow(rowHtml: String, down: Boolean): SwDevice? {
    val cells = TD_CELL.findAll(rowHtml).map { it.groupValues[1] }.toList()
    if (cells.size < 6) return null
    val ip = IP_HREF.find(cells.getOrNull(2).orEmpty())?.groupValues?.get(1)
        ?: IP_HIDDEN.find(rowHtml)?.groupValues?.get(1)
        ?: return null
    val number = NUMBER_AFTER_IP_HIDDEN.find(rowHtml)?.groupValues?.get(1)
        ?: plainText(cells[1]).filter { it.isDigit() }.ifBlank { "—" }
    val addressLines = plainText(cells[3]).lines().map { it.trim() }.filter { it.isNotEmpty() }
    val vlan = plainText(cells[4]).replace('\n', ' ').trim()
    val switchType = plainText(cells.getOrNull(5).orEmpty()).trim()
    val place = plainText(cells.getOrNull(6).orEmpty()).trim()
    return SwDevice(
        number = number.trim(),
        ip = ip,
        address = addressLines.getOrNull(0).orEmpty(),
        siteHint = addressLines.getOrNull(1).orEmpty(),
        vlan = vlan,
        switchType = switchType,
        place = place,
        down = down,
    )
}

private fun plainText(html: String): String =
    decodeHtml(
        html
            .replace(Regex("""<(br|/br|BR)\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " "),
    )
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .trim()

private fun decodeHtml(value: String): String =
    value
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

private val SITE_MENU_BLOCK = Regex(
    """<div[^>]*id=['"]s_sw['"][^>]*>([\s\S]*?)</div>""",
    RegexOption.IGNORE_CASE,
)

private val SITE_LINK = Regex(
    """<a[^>]*class=['"][^'"]*left_menu_a[^'"]*['"][^>]*href=['"]/sw/(\d+)['"][^>]*>([\s\S]*?)</a>""",
    RegexOption.IGNORE_CASE,
)

private val SITE_TITLE = Regex(
    """main_label[^>]*>\s*<a[^>]*>([\s\S]*?)</a>""",
    RegexOption.IGNORE_CASE,
)

private val TR_ROW = Regex(
    """<tr\s+class=['"]([^'"]*)['"]\s*>([\s\S]*?)</tr>""",
    RegexOption.IGNORE_CASE,
)

private val TD_CELL = Regex(
    """<td[^>]*>([\s\S]*?)</td>""",
    RegexOption.IGNORE_CASE,
)

private val IP_HREF = Regex(
    """/sw/snmp/([0-9.]+)""",
    RegexOption.IGNORE_CASE,
)

private val IP_HIDDEN = Regex(
    """id=['"]ip['"]\s+value=['"]([0-9.]+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val NUMBER_AFTER_IP_HIDDEN = Regex(
    """id=['"]ip['"]\s+value=['"][0-9.]+['"]\s*>\s*(\d+)""",
    RegexOption.IGNORE_CASE,
)
