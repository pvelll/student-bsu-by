package github.sushkpavel.studentbsuby.util.dispatchers

internal expect fun <K, V> concurrentMutableMap(): MutableMap<K, V>
