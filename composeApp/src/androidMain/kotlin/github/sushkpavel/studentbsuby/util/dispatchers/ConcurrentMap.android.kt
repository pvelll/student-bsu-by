package github.sushkpavel.studentbsuby.util.dispatchers

import java.util.concurrent.ConcurrentHashMap

internal actual fun <K, V> concurrentMutableMap(): MutableMap<K, V> = ConcurrentHashMap()
