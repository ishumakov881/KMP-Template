package net.lds.online.network.nos

/** Entry from web left menu `#s_sw` (`/sw/{id}` → name). */
data class SwSite(
    val id: Int,
    val name: String,
)

/** One device row from web `/sw/{id}` table. */
data class SwDevice(
    val number: String,
    val ip: String,
    val address: String,
    val siteHint: String,
    val vlan: String,
    val switchType: String,
    val place: String,
    val down: Boolean,
    /** Absolute URL of web mrtg thumb (`/inc/other/rrd/{ip}/nolegend.png`). */
    val mrtgUrl: String? = null,
    /** Absolute URL of web full graph page (`/sw/pic/{ip}`). */
    val picUrl: String? = null,
) {
    val subtitle: String
        get() = listOfNotNull(
            address.takeIf { it.isNotBlank() },
            switchType.takeIf { it.isNotBlank() },
            place.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

data class SwSitePage(
    val id: Int,
    val title: String,
    val devices: List<SwDevice>,
)

/** One graph on web `/sw/pic/{ip}` or `/sw/pic/{ip}/{n}`. */
data class SwPicGraph(
    val imageUrl: String,
    /** Relative path under `/sw/pic/` for drill-down (`10.0.x.x/1`), if linked. */
    val nextPath: String? = null,
)

data class SwPicPage(
    val title: String,
    val graphs: List<SwPicGraph>,
)
