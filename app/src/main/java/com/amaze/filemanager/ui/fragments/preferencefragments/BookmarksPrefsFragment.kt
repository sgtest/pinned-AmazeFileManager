package com.amaze.filemanager.ui.fragments.preferencefragments

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.databinding.DialogTwoedittextsBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.SimpleTextWatcher

class BookmarksPrefsFragment : BasePrefsFragment() {
    override val title = R.string.show_bookmarks_pref

    companion object {
        private val dataUtils = DataUtils.getInstance()!!
    }

    private val position: MutableMap<Preference, Int> = HashMap()
    private var bookmarksList: PreferenceCategory? = null

    private val itemOnEditListener = { it: PathSwitchPreference ->
        showEditDialog(it)
    }

    private val itemOnDeleteListener = { it: PathSwitchPreference ->
        showDeleteDialog(it)
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.bookmarks_prefs, rootKey)

        findPreference<Preference>("add_bookmarks")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                showCreateDialog()

                true
            }

        bookmarksList = findPreference("bookmarks_list")
        reload()
    }

    private fun reload() {
        for (p in position) {
            bookmarksList?.removePreference(p.key)
        }

        position.clear()
        for (i in dataUtils.books.indices) {
            val p = PathSwitchPreference(activity, itemOnEditListener, itemOnDeleteListener)
            p.title = dataUtils.books[i][0]
            p.summary = dataUtils.books[i][1]
            position[p] = i
            bookmarksList?.addPreference(p)
        }
    }

    private fun showCreateDialog() {
        val fabSkin = activity.accent
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val dialogBinding = DialogTwoedittextsBinding.inflate(LayoutInflater.from(requireContext()))

        val v = dialogBinding.root
        dialogBinding.textInput1.hint = getString(R.string.name)
        dialogBinding.textInput2.hint = getString(R.string.directory)
        val txtShortcutName = dialogBinding.text1
        val txtShortcutPath = dialogBinding.text2

        val dialog =
            MaterialDialog.Builder(requireActivity())
                .title(R.string.create_bookmark)
                .theme(activity.appTheme.materialDialogTheme)
                .positiveColor(fabSkin)
                .positiveText(R.string.create)
                .negativeColor(fabSkin)
                .negativeText(android.R.string.cancel)
                .customView(v, false)
                .build()
        dialog.getActionButton(DialogAction.POSITIVE).isEnabled = false
        disableButtonIfTitleEmpty(txtShortcutName, dialog)
        disableButtonIfNotPath(txtShortcutPath, dialog)
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                val result = isValidBookmark(
                    txtShortcutName.text.toString(),
                    txtShortcutPath.text.toString()
                )
                if (!result.first) {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(result.second),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    val p = PathSwitchPreference(activity, itemOnEditListener, itemOnDeleteListener)
                    p.title = txtShortcutName.text
                    p.summary = txtShortcutPath.text
                    position[p] = dataUtils.books.size
                    bookmarksList?.addPreference(p)
                    val values =
                        arrayOf(
                            txtShortcutName.text.toString(),
                            txtShortcutPath.text.toString(),
                        )
                    dataUtils.addBook(values)
                    utilsHandler.saveToDatabase(
                        OperationData(
                            UtilsHandler.Operation.BOOKMARKS,
                            txtShortcutName.text.toString(),
                            txtShortcutPath.text.toString(),
                        ),
                    )
                    dialog.dismiss()
                }
            }
        dialog.show()
    }

    private fun isValidBookmark(
        name: String,
        path: String,
    ): Pair<Boolean, Int> {
        return when {
            name.isEmpty() -> Pair(false, R.string.invalid_name)
            dataUtils.containsBooks(arrayOf(name, path)) != -1 -> Pair(
                false,
                R.string.bookmark_exists
            )

            !FileUtils.isPathAccessible(path, activity.prefs) -> Pair(
                false,
                R.string.ftp_path_change_error_invalid
            )

            else -> Pair(true, 0)
        }
    }

    private fun showEditDialog(p: PathSwitchPreference) {
        val fabSkin = activity.accent
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val dialogBinding = DialogTwoedittextsBinding.inflate(LayoutInflater.from(requireContext()))

        val v = dialogBinding.root
        dialogBinding.textInput1.hint = getString(R.string.name)
        dialogBinding.textInput2.hint = getString(R.string.directory)
        val editText1 = dialogBinding.text1
        val editText2 = dialogBinding.text2
        editText1.setText(p.title)
        editText2.setText(p.summary)

        val dialog =
            MaterialDialog.Builder(activity)
                .title(R.string.edit_bookmark)
                .theme(activity.appTheme.materialDialogTheme)
                .positiveColor(fabSkin)
                .positiveText(getString(R.string.edit).uppercase()) // TODO: 29/4/2017 don't use toUpperCase()
                .negativeColor(fabSkin)
                .negativeText(android.R.string.cancel)
                .customView(v, false)
                .build()
        dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
            FileUtils.isPathAccessible(editText2.text.toString(), activity.prefs)
        disableButtonIfTitleEmpty(editText1, dialog)
        disableButtonIfNotPath(editText2, dialog)
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                val oldName = p.title.toString()
                val oldPath = p.summary.toString()
                dataUtils.removeBook(position[p]!!)
                position.remove(p)
                bookmarksList?.removePreference(p)
                p.title = editText1.text
                p.summary = editText2.text
                position[p] = position.size
                bookmarksList?.addPreference(p)
                val values = arrayOf(editText1.text.toString(), editText2.text.toString())
                dataUtils.addBook(values)
                AppConfig.getInstance()
                    .runInBackground {
                        utilsHandler.renameBookmark(
                            oldName,
                            oldPath,
                            editText1.text.toString(),
                            editText2.text.toString(),
                        )
                    }
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun showDeleteDialog(p: PathSwitchPreference) {
        val fabSkin = activity.accent
        val utilsHandler = AppConfig.getInstance().utilsHandler

        val dialog =
            MaterialDialog.Builder(activity)
                .title(R.string.question_delete_bookmark)
                .theme(activity.appTheme.materialDialogTheme)
                .positiveColor(fabSkin)
                .positiveText(getString(R.string.delete).uppercase()) // TODO: 29/4/2017 don't use toUpperCase(), 20/9,2017 why not?
                .negativeColor(fabSkin)
                .negativeText(android.R.string.cancel)
                .build()
        dialog.getActionButton(DialogAction.POSITIVE)
            .setOnClickListener {
                dataUtils.removeBook(position[p]!!)
                utilsHandler.removeFromDatabase(
                    OperationData(
                        UtilsHandler.Operation.BOOKMARKS,
                        p.title.toString(),
                        p.summary.toString(),
                    ),
                )
                bookmarksList?.removePreference(p)
                position.remove(p)
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun disableButtonIfNotPath(
        path: AppCompatEditText,
        dialog: MaterialDialog,
    ) {
        path.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled =
                        FileUtils.isPathAccessible(s.toString(), activity.prefs)
                }
            },
        )
    }

    private fun disableButtonIfTitleEmpty(
        title: AppCompatEditText,
        dialog: MaterialDialog,
    ) {
        title.addTextChangedListener(
            object : SimpleTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    dialog.getActionButton(DialogAction.POSITIVE).isEnabled = title.length() > 0
                }
            },
        )
    }
}
