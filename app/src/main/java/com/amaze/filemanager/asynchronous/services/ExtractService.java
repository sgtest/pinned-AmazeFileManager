package com.amaze.filemanager.asynchronous.services;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.fileoperations.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.compress.PasswordRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.CorruptedInputException;

public class ExtractService extends AbstractProgressiveService {

    public static final String KEY_PATH_ZIP = "zip";
    public static final String KEY_ENTRIES_ZIP = "entries";
    public static final String TAG_BROADCAST_EXTRACT_CANCEL = "excancel";
    public static final String KEY_PATH_EXTRACT = "extractpath";
    private final Logger LOG = LoggerFactory.getLogger(ExtractService.class);
    private final IBinder mBinder = new ObtainableServiceBinder<>(this);
    // list of data packages,// to initiate chart in process viewer fragment
    private final ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
    private final ProgressHandler progressHandler = new ProgressHandler();
    /**
     * Class used for the client Binder. Because we know this service always runs in the same process
     * as its clients, we don't need to deal with IPC.
     */
    private final BroadcastReceiver receiver1 =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    progressHandler.setCancelled(true);
                }
            };
    private NotificationManagerCompat mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private ProgressListener progressListener;
    private RemoteViews customSmallContentViews, customBigContentViews;
    private @Nullable DoWork extractingAsyncTask;

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(receiver1, new IntentFilter(TAG_BROADCAST_EXTRACT_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        String file = intent.getStringExtra(KEY_PATH_ZIP);
        String extractPath = intent.getStringExtra(KEY_PATH_EXTRACT);
        String[] entries = intent.getStringArrayExtra(KEY_ENTRIES_ZIP);

        mNotifyManager = NotificationManagerCompat.from(getApplicationContext());
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int accentColor =
                ((AppConfig) getApplication())
                        .getUtilsProvider()
                        .getColorPreference()
                        .getCurrentUserColorPreferences(this, sharedPreferences)
                        .getAccent();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, getPendingIntentFlag(0));

        customSmallContentViews =
                new RemoteViews(getPackageName(), R.layout.notification_service_small);
        customBigContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_big);

        Intent stopIntent = new Intent(TAG_BROADCAST_EXTRACT_CANCEL);
        PendingIntent stopPendingIntent =
                PendingIntent.getBroadcast(
                        getApplicationContext(), 1234, stopIntent, getPendingIntentFlag(FLAG_UPDATE_CURRENT));
        NotificationCompat.Action action =
                new NotificationCompat.Action(
                        R.drawable.ic_zip_box_grey, getString(R.string.stop_ftp), stopPendingIntent);

        mBuilder =
                new NotificationCompat.Builder(
                        getApplicationContext(), NotificationConstants.CHANNEL_NORMAL_ID);
        mBuilder
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_zip_box_grey)
                .setContentIntent(pendingIntent)
                .setCustomContentView(customSmallContentViews)
                .setCustomBigContentView(customBigContentViews)
                .setCustomHeadsUpContentView(customSmallContentViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .addAction(action)
                .setAutoCancel(true)
                .setOngoing(true)
                .setColor(accentColor);

        NotificationConstants.setMetadata(
                getApplicationContext(), mBuilder, NotificationConstants.TYPE_NORMAL);
        startForeground(NotificationConstants.EXTRACT_ID, mBuilder.build());
        initNotificationViews();

        long totalSize = getTotalSize(file);

        progressHandler.setSourceSize(1);
        progressHandler.setTotalSize(totalSize);
        progressHandler.setProgressListener((speed) -> publishResults(speed, false, false));

        super.onStartCommand(intent, flags, startId);
        super.progressHalted();
        extractingAsyncTask = new DoWork(progressHandler, file, extractPath, entries);
        extractingAsyncTask.execute();

        return START_NOT_STICKY;
    }

    @Override
    protected NotificationManagerCompat getNotificationManager() {
        return mNotifyManager;
    }

    @Override
    protected NotificationCompat.Builder getNotificationBuilder() {
        return mBuilder;
    }

    @Override
    protected int getNotificationId() {
        return NotificationConstants.EXTRACT_ID;
    }

    @Override
    @StringRes
    protected int getTitle(boolean move) {
        return R.string.extracting;
    }

    @Override
    protected RemoteViews getNotificationCustomViewSmall() {
        return customSmallContentViews;
    }

    @Override
    protected RemoteViews getNotificationCustomViewBig() {
        return customBigContentViews;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    @Override
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    protected ArrayList<DatapointParcelable> getDataPackages() {
        return dataPackages;
    }

    @Override
    protected ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    @Override
    protected void clearDataPackages() {
        dataPackages.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (extractingAsyncTask != null) {
            extractingAsyncTask.cancel(true);
        }
        unregisterReceiver(receiver1);
    }

    /**
     * Method calculates zip file size to initiate progress Supporting local file extraction progress
     * for now
     */
    private long getTotalSize(String filePath) {
        return new File(filePath).length();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DoWork extends AsyncTask<Void, IOException, Boolean> {
        private final String compressedPath;
        private final ProgressHandler progressHandler;
        private String[] entriesToExtract;
        private String extractionPath;
        private ServiceWatcherUtil watcherUtil;
        private boolean paused = false;
        private boolean passwordProtected = false;

        private DoWork(ProgressHandler progressHandler, String cpath, String epath, String[] entries) {
            this.progressHandler = progressHandler;
            compressedPath = cpath;
            extractionPath = epath;
            entriesToExtract = entries;
        }

        @Override
        protected Boolean doInBackground(Void... p) {
            while (!isCancelled()) {
                if (paused) continue;

                final ExtractService extractService = ExtractService.this;

                File f = new File(compressedPath);
                String extractDirName = CompressedHelper.getFileName(f.getName());

                if (compressedPath.equals(extractionPath)) {
                    // custom extraction path not set, extract at default path
                    extractionPath = f.getParent() + "/" + extractDirName;
                } else {
                    if (extractionPath.endsWith("/")) {
                        extractionPath = extractionPath + extractDirName;
                    } else if (!passwordProtected) {
                        extractionPath = extractionPath + "/" + extractDirName;
                    }
                }

                if (entriesToExtract != null && entriesToExtract.length == 0)
                    entriesToExtract = null;

                final Extractor extractor =
                        CompressedHelper.getExtractorInstance(
                                extractService.getApplicationContext(),
                                f,
                                extractionPath,
                                new Extractor.OnUpdate() {
                                    private int sourceFilesProcessed = 0;

                                    @Override
                                    public void onStart(long totalBytes, String firstEntryName) {
                                        // setting total bytes calculated from zip entries
                                        progressHandler.setTotalSize(totalBytes);

                                        extractService.addFirstDatapoint(firstEntryName, 1, totalBytes, false);

                                        watcherUtil = new ServiceWatcherUtil(progressHandler);
                                        watcherUtil.watch(ExtractService.this);
                                    }

                                    @Override
                                    public void onUpdate(String entryPath) {
                                        progressHandler.setFileName(entryPath);
                                        if (entriesToExtract != null) {
                                            progressHandler.setSourceFilesProcessed(sourceFilesProcessed++);
                                        }
                                    }

                                    @Override
                                    public void onFinish() {
                                        if (entriesToExtract == null) {
                                            progressHandler.setSourceFilesProcessed(1);
                                        }
                                    }

                                    @Override
                                    public boolean isCancelled() {
                                        return progressHandler.getCancelled();
                                    }
                                },
                                ServiceWatcherUtil.UPDATE_POSITION);

                if (extractor == null) {
                    Toast.makeText(
                                    getApplicationContext(),
                                    R.string.error_cant_decompress_that_file,
                                    Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

                try {
                    if (entriesToExtract != null) {
                        extractor.extractFiles(entriesToExtract);
                    } else {
                        extractor.extractEverything();
                    }
                    return (extractor.getInvalidArchiveEntries().size() == 0);
                } catch (Extractor.EmptyArchiveNotice e) {
                    LOG.error("Archive " + compressedPath + " is an empty archive");
                    AppConfig.toast(
                            getApplicationContext(),
                            extractService.getString(R.string.error_empty_archive, compressedPath));
                    return true;
                } catch (Extractor.BadArchiveNotice e) {
                    LOG.error("Archive " + compressedPath + " is a corrupted archive.", e);
                    AppConfig.toast(
                            getApplicationContext(),
                            e.getCause() != null && TextUtils.isEmpty(e.getCause().getMessage())
                                    ? getString(R.string.error_bad_archive_without_info, compressedPath)
                                    : getString(
                                    R.string.error_bad_archive_with_info, compressedPath, e.getMessage()));
                    return true;
                } catch (CorruptedInputException e) {
                    LOG.debug("Corrupted LZMA input", e);
                    return false;
                } catch (IOException e) {
                    if (PasswordRequiredException.class.isAssignableFrom(e.getClass())) {
                        LOG.debug("Archive is password protected.", e);
                        if (ArchivePasswordCache.getInstance().containsKey(compressedPath)) {
                            ArchivePasswordCache.getInstance().remove(compressedPath);
                            AppConfig.toast(
                                    getApplicationContext(),
                                    extractService.getString(R.string.error_archive_password_incorrect));
                        }
                        passwordProtected = true;
                        paused = true;
                        publishProgress(e);
                    } else if (e.getCause() != null
                            && UnsupportedRarV5Exception.class.isAssignableFrom(e.getCause().getClass())) {
                        LOG.error("RAR " + compressedPath + " is unsupported V5 archive", e);
                        AppConfig.toast(
                                getApplicationContext(),
                                extractService.getString(R.string.error_unsupported_v5_rar, compressedPath));
                        return false;
                    } else {
                        LOG.error("Error while extracting file " + compressedPath, e);
                        AppConfig.toast(getApplicationContext(), extractService.getString(R.string.error));
                        paused = true;
                        publishProgress(e);
                    }
                } catch (Throwable unhandledException) {
                    LOG.error("Unhandled exception thrown", unhandledException);
                }
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(IOException... values) {
            super.onProgressUpdate(values);
            if (values.length < 1 || !passwordProtected) return;

            IOException result = values[0];
            ArchivePasswordCache.getInstance().remove(compressedPath);
            GeneralDialogCreation.showPasswordDialog(
                    AppConfig.getInstance().getMainActivityContext(),
                    (MainActivity) AppConfig.getInstance().getMainActivityContext(),
                    AppConfig.getInstance().getUtilsProvider().getAppTheme(),
                    R.string.archive_password_prompt,
                    R.string.authenticate_password,
                    (dialog, which) -> {
                        AppCompatEditText editText = dialog.getView().findViewById(R.id.singleedittext_input);
                        ArchivePasswordCache.getInstance().put(compressedPath, editText.getText().toString());
                        ExtractService.this.getDataPackages().clear();
                        this.paused = false;
                        dialog.dismiss();
                    },
                    ((dialog, which) -> {
                        dialog.dismiss();
                        toastOnParseError(result);
                        cancel(true); // This cancels the AsyncTask...
                        progressHandler.setCancelled(true);
                        stopSelf(); // and this stops the ExtractService altogether.
                        this.paused = false;
                    }));
        }

        @Override
        public void onPostExecute(Boolean hasInvalidEntries) {
            ArchivePasswordCache.getInstance().remove(compressedPath);
            final ExtractService extractService = ExtractService.this;

            // check whether watcherutil was initialized. It was not initialized when we got exception
            // in extracting the file
            if (watcherUtil != null) watcherUtil.stopWatch();
            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, extractionPath);
            extractService.sendBroadcast(intent);
            extractService.stopSelf();

            if (!hasInvalidEntries)
                AppConfig.toast(
                        getApplicationContext(), getString(R.string.multiple_invalid_archive_entries));
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            ArchivePasswordCache.getInstance().remove(compressedPath);
        }

        private void toastOnParseError(IOException result) {
            Toast.makeText(
                            getApplicationContext(),
                            AppConfig.getInstance()
                                    .getResources()
                                    .getString(
                                            R.string.cannot_extract_archive,
                                            compressedPath,
                                            result.getLocalizedMessage()),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }
}
