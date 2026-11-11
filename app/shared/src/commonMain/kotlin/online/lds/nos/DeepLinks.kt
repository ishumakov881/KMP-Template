package online.lds.nos

/**
 * Deep-link / intent helpers for native screens (SNMP smoke without map taps).
 * Example: `https://nos.lds.online/sw/snmp/10.0.93.243`
 */
object NosDeepLinks {
    const val EXTRA_SNMP_IP = "snmp_ip"

    private val IPV4 = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

    /** Path like `/sw/snmp/10.0.93.243` or full URL path. */
    fun snmpIpFromPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val normalized = path.trim().substringBefore('?').trimEnd('/')
        val marker = "/sw/snmp/"
        val idx = normalized.indexOf(marker, ignoreCase = true)
        val ip = if (idx >= 0) {
            normalized.substring(idx + marker.length).substringBefore('/')
        } else {
            normalized.trimStart('/')
        }
        return ip.takeIf { IPV4.matches(it) }
    }

    fun snmpIpFromExtra(value: String?): String? =
        value?.trim()?.takeIf { IPV4.matches(it) }
}
