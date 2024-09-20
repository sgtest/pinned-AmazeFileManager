package com.amaze.filemanager.database;

import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.models.explorer.Sort;
import com.amaze.filemanager.filesystem.files.sort.SortType;
import io.reactivex.schedulers.Schedulers;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ning on 5/28/2018.
 */
public class SortHandler {

    private final Logger LOG = LoggerFactory.getLogger(SortHandler.class);

    private final ExplorerDatabase database;

    private SortHandler(@NonNull ExplorerDatabase explorerDatabase) {
        database = explorerDatabase;
    }

    public static SortHandler getInstance() {
        return SortHandlerHolder.INSTANCE;
    }

    public static SortType getSortType(Context context, String path) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> onlyThisFloders =
                sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, new HashSet<>());
        final boolean onlyThis = onlyThisFloders.contains(path);
        final int globalSortby = Integer.parseInt(sharedPref.getString("sortby", "0"));
        SortType globalSortType = SortType.getDirectorySortType(globalSortby);
        if (!onlyThis) {
            return globalSortType;
        }
        Sort sort = SortHandler.getInstance().findEntry(path);
        if (sort == null) {
            return globalSortType;
        }
        return SortType.getDirectorySortType(sort.type);
    }

    public void addEntry(String path, SortType sortType) {
        Sort sort = new Sort(path, sortType.toDirectorySortInt());
        database.sortDao().insert(sort).subscribeOn(Schedulers.io()).subscribe();
    }

    public void clear(String path) {
        database.sortDao().clear(path).subscribeOn(Schedulers.io()).subscribe();
    }

    public void updateEntry(Sort oldSort, String newPath, SortType newSortType) {
        Sort newSort = new Sort(newPath, newSortType.toDirectorySortInt());
        database.sortDao().update(newSort).subscribeOn(Schedulers.io()).subscribe();
    }

    @Nullable
    public Sort findEntry(String path) {
        try {
            return database.sortDao().find(path).subscribeOn(Schedulers.io()).blockingGet();
        } catch (Exception e) {
            // catch error to handle Single#onError for blockingGet
            LOG.error(getClass().getSimpleName(), e);
            return null;
        }
    }

    private static class SortHandlerHolder {
        private static final SortHandler INSTANCE =
                new SortHandler(AppConfig.getInstance().getExplorerDatabase());
    }
}
