

package com.amaze.filemanager.database.daos;

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_NAME;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_SMB;

import java.util.List;

import com.amaze.filemanager.database.models.utilities.SmbEntry;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * {@link Dao} interface definition for {@link SmbEntry}. Concrete class is generated by Room during
 * build.
 *
 * @see Dao
 * @see SmbEntry
 * @see com.amaze.filemanager.database.UtilitiesDatabase
 */
@Dao
public interface SmbEntryDao {

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  Completable insert(SmbEntry instance);

  @Update
  Completable update(SmbEntry instance);

  @Query("SELECT * FROM " + TABLE_SMB)
  Single<List<SmbEntry>> list();

  @Query(
      "SELECT * FROM "
          + TABLE_SMB
          + " WHERE "
          + COLUMN_NAME
          + " = :name AND "
          + COLUMN_PATH
          + " = :path")
  Single<SmbEntry> findByNameAndPath(String name, String path);

  @Query("DELETE FROM " + TABLE_SMB + " WHERE " + COLUMN_NAME + " = :name")
  Completable deleteByName(String name);

  @Query(
      "DELETE FROM "
          + TABLE_SMB
          + " WHERE "
          + COLUMN_NAME
          + " = :name AND "
          + COLUMN_PATH
          + " = :path")
  Completable deleteByNameAndPath(String name, String path);
}
