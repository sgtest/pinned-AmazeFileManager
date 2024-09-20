

package com.amaze.filemanager.adapters.glide.apkimage;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 16:06.
 */
public class ApkImageModelLoader implements ModelLoader<String, Drawable> {

  private Context context;

  public ApkImageModelLoader(Context context) {
    this.context = context;
  }

  @Nullable
  @Override
  public LoadData<Drawable> buildLoadData(String s, int width, int height, Options options) {
    return new LoadData<>(new ObjectKey(s), new ApkImageDataFetcher(context, s));
  }

  @Override
  public boolean handles(String s) {
    return s.substring(s.length() - 4, s.length()).toLowerCase().equals(".apk");
  }
}
