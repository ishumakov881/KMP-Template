package net.lds.online.network.nos

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import net.lds.online.network.ApiException

/**
 * Web `/sw` area: left-menu sites + per-site device table.
 * Server renders HTML (no JSON list endpoint) — parse `#s_sw` and `/sw/{id}` table.
 */
class NosSwRepository(
    private val client: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    /** Web `/sw/pic/{path}` — day thumbs or hour/day/week/month/year set. */
    suspend fun picPage(path: String): SwPicPage {
        val body = client.get(NosEndpoints.swPic(path)).bodyAsText()
        if (isLoginHtml(body)) {
            throw ApiException.clientError(401, "Сессия истекла")
        }
        return parseSwPicPage(body)
    }

    /**
     * Native SNMP detail (web `/sw/snmp/{ip}` without browser):
     * shell HTML → `nb_id`, then `ajaxsnmp.php` `label_off` + `new_port_description`.
     */
    suspend fun snmpPage(ip: String): SwSnmpPage {
        val shell = client.get(NosEndpoints.swSnmp(ip)).bodyAsText()
        if (isLoginHtml(shell)) {
            throw ApiException.clientError(401, "Сессия истекла")
        }
        val nbId = NB_ID_HIDDEN.find(shell)?.groupValues?.get(1)
            ?: throw ApiException.clientError(404, "Нет nb_id для SNMP $ip")

        val labelResponse = client.submitForm(
            url = NosEndpoints.AJAX_SNMP,
            formParameters = Parameters.build {
                append("mode", "label_off")
                append("ip", ip)
            },
        ).bodyAsText()
        val labelHtml = (json.parseToJsonElement(labelResponse) as? JsonObject)
            ?.get("label")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()

        val portsResponse = client.submitForm(
            url = NosEndpoints.AJAX_SNMP,
            formParameters = Parameters.build {
                append("mode", "new_port_description")
                append("nb_id", nbId)
                append("ip", ip)
                append("online", "false")
            },
        ).bodyAsText()
        val portsObj = json.parseToJsonElement(portsResponse) as? JsonObject
            ?: throw ApiException.clientError(502, "Пустой ответ SNMP ports")
        val portsHtml = (portsObj["ports"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val free = (portsObj["free"] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonPrimitive)?.intOrNull
                ?: (el as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        }.orEmpty()
        val freeCount = (portsObj["free_count"] as? JsonPrimitive)?.intOrNull
            ?: (portsObj["free_count"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
            ?: free.size

        return SwSnmpPage(
            ip = ip,
            nbId = nbId,
            label = htmlToPlain(labelHtml),
            freePorts = free,
            freeCount = freeCount,
            ports = parseSnmpPortsHtml(portsHtml),
        )
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

internal fun parseSwPicPage(html: String): SwPicPage {
    val content = PIC_CONTENT.find(html)?.groupValues?.get(1) ?: html
    val title = PIC_TITLE.find(content)?.groupValues?.get(1)?.let(::decodeHtml)?.trim().orEmpty()
    val linked = PIC_LINKED_IMG.findAll(content).map { match ->
        val href = match.groupValues[1]
        val src = stripQuery(match.groupValues[2])
        SwPicGraph(
            imageUrl = absoluteNosAssetUrl(src),
            nextPath = href.removePrefix("/sw/pic/").trimStart('/').takeIf { it.isNotBlank() },
        )
    }.toList()
    if (linked.isNotEmpty()) {
        return SwPicPage(title = title, graphs = linked)
    }
    val bare = PIC_BARE_IMG.findAll(content).map { match ->
        SwPicGraph(
            imageUrl = absoluteNosAssetUrl(stripQuery(match.groupValues[1])),
            nextPath = null,
        )
    }.toList()
    return SwPicPage(title = title, graphs = bare)
}

private fun stripQuery(path: String): String = path.substringBefore('?')

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
    val mrtgPath = MRTG_IMG.find(rowHtml)?.groupValues?.get(1)
        ?: "/inc/other/rrd/$ip/nolegend.png"
    val picPath = PIC_HREF.find(rowHtml)?.groupValues?.get(1)
        ?: "/sw/pic/$ip"
    return SwDevice(
        number = number.trim(),
        ip = ip,
        address = addressLines.getOrNull(0).orEmpty(),
        siteHint = addressLines.getOrNull(1).orEmpty(),
        vlan = vlan,
        switchType = switchType,
        place = place,
        down = down,
        mrtgUrl = absoluteNosAssetUrl(mrtgPath),
        picUrl = absoluteNosAssetUrl(picPath),
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

private val MRTG_IMG = Regex(
    """src=['"]([^'"]*inc/other/rrd/[^'"]+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val PIC_HREF = Regex(
    """href=['"](/sw/pic/[0-9.]+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val PIC_CONTENT = Regex(
    """id=['"]spin['"][^>]*>([\s\S]*)""",
    RegexOption.IGNORE_CASE,
)

private val PIC_TITLE = Regex(
    """<h2>\s*([\s\S]*?)\s*</h2>""",
    RegexOption.IGNORE_CASE,
)

private val PIC_LINKED_IMG = Regex(
    """<a[^>]*href=['"](/sw/pic/[^'"]+)['"][^>]*>\s*<img[^>]*src=['"]([^'"]+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val PIC_BARE_IMG = Regex(
    """<img[^>]*src=['"]([^'"]*inc/other/rrd/[^'"]+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val NB_ID_HIDDEN = Regex(
    """id=['"]nb_id['"]\s+value=['"](\d+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val SNMP_PORT_ROW = Regex(
    """<tr\b[^>]*>([\s\S]*?)</tr>""",
    RegexOption.IGNORE_CASE,
)

private val SNMP_PORT_INDEX = Regex(
    """id=['"]l_(\d+)['"]""",
    RegexOption.IGNORE_CASE,
)

private val SNMP_PORT_BG = Regex(
    """bgcolor=['"]([^'"]+)['"]""",
    RegexOption.IGNORE_CASE,
)

internal fun parseSnmpPortsHtml(portsHtml: String): List<SwSnmpPort> =
    SNMP_PORT_ROW.findAll(portsHtml).mapNotNull { match ->
        val row = match.groupValues[1]
        val index = SNMP_PORT_INDEX.find(row)?.groupValues?.get(1)?.toIntOrNull()
            ?: return@mapNotNull null
        val cells = TD_CELL.findAll(row).map { plainText(it.groupValues[1]) }.toList()
        // checkbox | name | type | vlan | description | tag
        SwSnmpPort(
            index = index,
            name = cells.getOrNull(1).orEmpty(),
            type = cells.getOrNull(2).orEmpty(),
            vlan = cells.getOrNull(3).orEmpty(),
            description = cells.getOrNull(4).orEmpty(),
            tag = cells.getOrNull(5).orEmpty(),
            bgColor = SNMP_PORT_BG.find(row)?.groupValues?.get(1),
        )
    }.toList()

internal fun htmlToPlain(html: String): String =
    decodeHtml(
        html
            .replace(Regex("""<(br|/br|BR)\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " "),
    )
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .trim()
