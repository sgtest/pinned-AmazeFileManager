

package com.amaze.filemanager.database.models.utilities;

import com.amaze.filemanager.database.UtilitiesDatabase;

import androidx.room.Entity;

/**
 * {@link Entity} representation of <code>smb</code> table in utilities.db.
 *
 * @see UtilitiesDatabase
 */
@Entity(tableName = UtilitiesDatabase.TABLE_SMB)
public class SmbEntry extends OperationDataWithName {

  public SmbEntry(String name, String path) {
    super(name, path);
  }
}
