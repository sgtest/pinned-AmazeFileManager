/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.activities;

import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static androidx.test.core.app.ActivityScenario.launch;
import static org.robolectric.Shadows.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowStorageManager;

import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.test.TestUtils;

import android.os.Build;
import android.os.storage.StorageManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class, ShadowStorageManager.class},
    maxSdk = P)
/*
 * Need to make LooperMode PAUSED and flush the main looper before activity can show up.
 * @see {@link LooperMode.Mode.PAUSED}
 * @see {@link <a href="https://stackoverflow.com/questions/55679636/robolectric-throws-fragmentmanager-is-already-executing-transactions">StackOverflow discussion</a>}
 */
@LooperMode(LooperMode.Mode.PAUSED)
public class MainActivityTest {

  @Before
  public void setUp() {
    if (Build.VERSION.SDK_INT >= N) TestUtils.initializeInternalStorage();
  }

  @After
  public void tearDown() {
    if (Build.VERSION.SDK_INT >= N)
      shadowOf(ApplicationProvider.getApplicationContext().getSystemService(StorageManager.class))
          .resetStorageVolumeList();
  }

  @Test
  public void testMainActivity() {
    ActivityScenario<MainActivity> scenario = launch(MainActivity.class);

    ShadowLooper.idleMainLooper();

    scenario.moveToState(Lifecycle.State.STARTED);

    scenario.onActivity(
        activity -> {
          scenario.close();
        });
  }
}
