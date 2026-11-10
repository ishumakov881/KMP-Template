package net.lds.online.network.nos

/**
 * Pull structured fields from web markgen `popupContent` HTML — discard HTML after parse.
 */
internal data class ParsedPopupFields(
    val ip: String? = null,
    val pingType: String? = null,
    val siteId: Int? = null,
    val siteLabel: String? = null,
    val place: String? = null,
    val deviceModel: String? = null,
    val serial: String? = null,
    val mac: String? = null,
    val maintenanceId: String? = null,
    val maintenanceActive: Boolean = false,
    val maintenanceText: String? = null,
)

internal fun parsePopupFields(html: String?): ParsedPopupFields {
    if (html.isNullOrBlank()) return ParsedPopupFields()
    val ip = DATA_IP.find(html)?.groupValues?.get(1)
        ?: SNMP_HREF.find(html)?.groupValues?.get(1)
        ?: IP_BOLD.find(html)?.groupValues?.get(1)?.trim()
    val pingType = DATA_T.find(html)?.groupValues?.get(1)
    val siteId = SITE_HREF.find(html)?.groupValues?.get(1)?.toIntOrNull()
    val siteLabel = SITE_LABEL.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    val place = PLACE.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    val deviceModel = MODEL.find(html)?.groupValues?.get(1)
        ?.replace(Regex("<[^>]+>"), "")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val serial = SERIAL.find(html)?.groupValues?.get(1)?.trim()
    val mac = MAC.find(html)?.groupValues?.get(1)?.trim()
    val maintenanceId = NB_ID.find(html)?.groupValues?.get(1)
    val maintenanceText = MAINT_TEXT.find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    val maintenanceActive = html.contains("maintenance_true") || !maintenanceText.isNullOrBlank()
    return ParsedPopupFields(
        ip = ip,
        pingType = pingType,
        siteId = siteId,
        siteLabel = siteLabel,
        place = place,
        deviceModel = deviceModel,
        serial = serial,
        mac = mac,
        maintenanceId = maintenanceId,
        maintenanceActive = maintenanceActive,
        maintenanceText = maintenanceText,
    )
}

private val DATA_IP = Regex("""data-ip=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val DATA_T = Regex("""data-t=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val SNMP_HREF = Regex("""/sw/snmp/([0-9.]+)""", RegexOption.IGNORE_CASE)
private val IP_BOLD = Regex("""IP:\s*<b>\s*([^<]+?)\s*</b>""", RegexOption.IGNORE_CASE)
private val SITE_HREF = Regex("""/sw/(\d+)#""", RegexOption.IGNORE_CASE)
private val SITE_LABEL = Regex("""href=["']/sw/\d+#[^"']*["'][^>]*>\s*([^<]+?)\s*</a>""", RegexOption.IGNORE_CASE)
private val PLACE = Regex("""Место установки:\s*([^<]+)""", RegexOption.IGNORE_CASE)
private val MODEL = Regex("""(?:Свич|Модель):\s*(?:<b>)?([^<\n]+)""", RegexOption.IGNORE_CASE)
private val SERIAL = Regex("""Серийник:\s*<b>\s*([^<]+?)\s*</b>""", RegexOption.IGNORE_CASE)
private val MAC = Regex("""MAC:\s*<b>\s*([^<]+?)\s*</b>""", RegexOption.IGNORE_CASE)
private val NB_ID = Regex("""data-nb_id=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val MAINT_TEXT = Regex("""class=["']maintenance_text["'][^>]*>\s*([^<]*)""", RegexOption.IGNORE_CASE)
