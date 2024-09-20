

package com.amaze.filemanager.database.daos;

import static com.amaze.filemanager.database.ExplorerDatabase.COLUMN_TAB_NO;
import static com.amaze.filemanager.database.ExplorerDatabase.TABLE_TAB;

import java.util.List;

import com.amaze.filemanager.database.models.explorer.Tab;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * {@link Dao} interface definition for {@link Tab}. Concrete class is generated by Room during
 * build.
 *
 * @see Dao
 * @see Tab
 * @see com.amaze.filemanager.database.ExplorerDatabase
 */
@Dao
public interface TabDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insertTab(Tab tab);

  @Transaction
  @Query("DELETE FROM " + TABLE_TAB)
  Completable clear();

  @Query("SELECT * FROM " + TABLE_TAB + " WHERE " + COLUMN_TAB_NO + " = :tabNo")
  Single<Tab> find(int tabNo);

  @Update(onConflict = OnConflictStrategy.REPLACE)
  void update(Tab tab);

  @Query("SELECT * FROM " + TABLE_TAB)
  Single<List<Tab>> list();
}
