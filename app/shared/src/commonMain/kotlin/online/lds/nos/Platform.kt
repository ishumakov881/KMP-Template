package online.lds.nos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform