

package com.amaze.filemanager.application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

import androidx.multidex.MultiDexApplication;

/**
 * @author Emmanuel on 22/11/2017, at 17:18.
 */
public class GlideApplication extends MultiDexApplication {
  @Override
  public void onCreate() {
    super.onCreate();
    Glide.get(this).setMemoryCategory(MemoryCategory.HIGH);
  }
}
