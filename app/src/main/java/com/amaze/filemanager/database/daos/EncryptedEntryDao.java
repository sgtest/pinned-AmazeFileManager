

package com.amaze.filemanager.database.daos;

import static com.amaze.filemanager.database.ExplorerDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.ExplorerDatabase.TABLE_ENCRYPTED;

import java.util.List;

import com.amaze.filemanager.database.models.explorer.EncryptedEntry;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * {@link Dao} interface definition for {@link EncryptedEntry}. Concrete class is generated by Room
 * during build.
 *
 * @see Dao
 * @see EncryptedEntry
 * @see com.amaze.filemanager.database.ExplorerDatabase
 */
@Dao
public interface EncryptedEntryDao {

  @Insert
  Completable insert(EncryptedEntry entry);

  @Query("SELECT * FROM " + TABLE_ENCRYPTED + " WHERE " + COLUMN_PATH + " = :path")
  Single<EncryptedEntry> select(String path);

  @Update
  Completable update(EncryptedEntry entry);

  @Transaction
  @Query("DELETE FROM " + TABLE_ENCRYPTED + " WHERE " + COLUMN_PATH + " = :path")
  Completable delete(String path);

  @Query("SELECT * FROM " + TABLE_ENCRYPTED)
  Single<List<EncryptedEntry>> list();
}
