package github.sushkpavel.studentbsuby.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import github.sushkpavel.studentbsuby.data.models.Lesson

@Dao
interface LessonsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: Lesson)

    @Query("SELECT * FROM Lesson WHERE owner = :username ORDER BY day_of_week, number")
    suspend fun getAll(username : String) : List<Lesson>

    @Query("DELETE FROM Lesson WHERE owner = :username")
    suspend fun clear(username : String)
}
