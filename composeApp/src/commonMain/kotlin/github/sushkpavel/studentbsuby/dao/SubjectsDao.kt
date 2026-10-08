package github.sushkpavel.studentbsuby.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import github.sushkpavel.studentbsuby.data.models.Subject

@Dao
interface SubjectsDao  {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject)

    @Query("SELECT *  FROM Subject WHERE owner = :username ORDER BY semester")
    suspend fun getAll(username : String) : List<Subject>

    @Query("DELETE FROM Subject WHERE owner = :username")
    suspend fun clear(username : String)
}
