package github.sushkpavel.studentbsuby.util.communication

import androidx.lifecycle.SavedStateHandle

interface StateHolder<T> {

    val current : T

    suspend fun saveIn(
        savedStateHandle: SavedStateHandle,
        key : String,
    )
}

