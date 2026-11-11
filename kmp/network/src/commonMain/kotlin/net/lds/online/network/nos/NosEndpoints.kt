package net.lds.online.network.nos

object NosEndpoints {
    // Trailing slash required: Ktor defaultRequest URL merge (RFC 3986).
    const val BASE_URL = "https://nos.lds.online/"
    const val LOGIN = "/login"
    const val AJAX_MAP = "/ajax/ajaxmap.php"
    /** Web SNMP page actions (`mode=label_off|new_port_description|…`). */
    const val AJAX_SNMP = "/ajax/ajaxsnmp.php"
    /** Web left menu `#s_sw` + site pages — HTML, not markgen. */
    const val SW = "/sw"

    fun swSite(id: Int): String = "$SW/$id"

    /** Web mrtg page: `/sw/pic/{ip}` or `/sw/pic/{ip}/{n}`. */
    fun swPic(path: String): String = "$SW/pic/${path.trimStart('/')}"

    /** Web SNMP UI shell: `/sw/snmp/{ip}` (ports loaded via [AJAX_SNMP]). */
    fun swSnmp(ip: String): String = "$SW/snmp/${ip.trim()}"
}
