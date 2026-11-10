package online.lds.nos

import dev.walhalla.lazagne.sayHello

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}