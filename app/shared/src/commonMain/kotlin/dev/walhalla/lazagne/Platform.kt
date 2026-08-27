package dev.walhalla.lazagne

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform