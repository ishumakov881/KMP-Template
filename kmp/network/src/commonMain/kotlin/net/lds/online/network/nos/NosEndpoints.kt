package net.lds.online.network.nos

object NosEndpoints {
    const val BASE_URL = "https://nos.lds.online"
    const val LOGIN = "/login"
    const val AJAX_MAP = "/ajax/ajaxmap.php"
    /** Web left menu `#s_sw` + site pages — HTML, not markgen. */
    const val SW = "/sw"

    fun swSite(id: Int): String = "$SW/$id"
}
