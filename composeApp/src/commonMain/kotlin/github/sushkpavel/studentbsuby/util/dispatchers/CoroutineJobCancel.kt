package github.sushkpavel.studentbsuby.util.dispatchers

fun interface CoroutineJobCancel {
    fun cancel(key: Any?)
}
