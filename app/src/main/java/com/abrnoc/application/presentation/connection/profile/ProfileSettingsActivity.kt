/******************************************************************************
 *                                                                            *
 * Copyright (C) 2021 by nekohasekai <contact-sagernet@sekai.icu>             *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package com.abrnoc.application.presentation.connection.profile

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import com.abrnoc.application.R
import com.abrnoc.application.connection.database.ProfileManager
import com.abrnoc.application.connection.neko.SagerDatabase
import com.abrnoc.application.ftm.AbstractBean
import com.abrnoc.application.presentation.connection.DataStore
import com.abrnoc.application.presentation.connection.Key
import com.abrnoc.application.presentation.connection.Logs
import com.abrnoc.application.presentation.connection.OnPreferenceDataStoreChangeListener
import com.abrnoc.application.presentation.connection.SagerNet
import com.abrnoc.application.presentation.connection.applyDefaultValues
import com.abrnoc.application.presentation.connection.onMainDispatcher
import com.abrnoc.application.presentation.connection.runOnDefaultDispatcher
import com.github.shadowsocks.plugin.AlertDialogFragment
import com.github.shadowsocks.plugin.Empty
import com.takisoft.preferencex.PreferenceFragmentCompat
import io.nekohasekai.sagernet.*
import io.nekohasekai.sagernet.widget.ListListener
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates

@Suppress("UNCHECKED_CAST")
abstract class ProfileSettingsActivity<T : AbstractBean>(
    @LayoutRes resId: Int = R.layout.layout_config_settings,
) : ThemedActivity(resId),
    OnPreferenceDataStoreChangeListener {

    class UnsavedChangesDialogFragment : AlertDialogFragment<Empty, Empty>() {
        override fun AlertDialog.Builder.prepare(listener: DialogInterface.OnClickListener) {
            setTitle(R.string.unsaved_changes_prompt)
            setPositiveButton(R.string.yes) { _, _ ->
                runOnDefaultDispatcher {
                    (requireActivity() as ProfileSettingsActivity<*>).saveAndExit()
                }
            }
            setNegativeButton(R.string.no) { _, _ ->
                requireActivity().finish()
            }
            setNeutralButton(android.R.string.cancel, null)
        }
    }

    @Parcelize
    data class ProfileIdArg(val profileId: Long, val groupId: Long) : Parcelable
    class DeleteConfirmationDialogFragment : AlertDialogFragment<ProfileIdArg, Empty>() {
        override fun AlertDialog.Builder.prepare(listener: DialogInterface.OnClickListener) {
            setTitle(R.string.delete_confirm_prompt)
            setPositiveButton(R.string.yes) { _, _ ->
                runOnDefaultDispatcher {
                    ProfileManager.deleteProfile(arg.groupId, arg.profileId)
                }
                requireActivity().finish()
            }
            setNegativeButton(R.string.no, null)
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "id"
        const val EXTRA_IS_SUBSCRIPTION = "sub"
    }

    abstract fun createEntity(): T
    abstract fun T.init()
    abstract fun T.serialize()

    val proxyEntity by lazy { SagerDatabase.proxyDao.getById(DataStore.editingId) }
    protected var isSubscription by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.profile_config)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }

        if (savedInstanceState == null) {
            val editingId = intent.getLongExtra(EXTRA_PROFILE_ID, 0L)
            isSubscription = intent.getBooleanExtra(EXTRA_IS_SUBSCRIPTION, false)
            DataStore.editingId = editingId
            runOnDefaultDispatcher {
                if (editingId == 0L) {
                    DataStore.editingGroup = DataStore.selectedGroupForImport()
                    createEntity().applyDefaultValues().init()
                } else {
                    if (proxyEntity == null) {
                        onMainDispatcher {
                            finish()
                        }
                        return@runOnDefaultDispatcher
                    }
                    DataStore.editingGroup = proxyEntity!!.groupId
                    (proxyEntity!!.requireBean() as T).init()
                }

                onMainDispatcher {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.settings, MyPreferenceFragmentCompat())
                        .commit()
                }
            }


        }

    }

    open suspend fun saveAndExit() {

        val editingId = DataStore.editingId
        if (editingId == 0L) {
            val editingGroup = DataStore.editingGroup
            ProfileManager.createProfile(editingGroup, createEntity().apply { serialize() })
        } else {
            if (proxyEntity == null) {
                finish()
                return
            }
            if (proxyEntity!!.id == DataStore.selectedProxy) {
                SagerNet.stopService()
            }
            ProfileManager.updateProfile(proxyEntity!!.apply { (requireBean() as T).serialize() })
        }
        finish()

    }

    val child by lazy { supportFragmentManager.findFragmentById(R.id.settings) as MyPreferenceFragmentCompat }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.profile_config_menu, menu)
//        menu.findItem(R.id.action_move)?.apply {
//            if (DataStore.editingId != 0L // not new profile
//                && SagerDatabase.groupDao.getById(DataStore.editingGroup)?.type == GroupType.BASIC // not in subscription group
//                && SagerDatabase.groupDao.allGroups()
//                    .filter { it.type == GroupType.BASIC }.size > 1 // have other basic group
//            ) isVisible = true
//        }
//        menu.findItem(R.id.action_create_shortcut)?.apply {
//            if (Build.VERSION.SDK_INT >= 26 &&
//                DataStore.editingId != 0L // not new profile
//            ) isVisible = true
//        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = child.onOptionsItemSelected(item)

    override fun onBackPressed() {
        if (DataStore.dirty) UnsavedChangesDialogFragment().apply { key() }
            .show(supportFragmentManager, null) else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!super.onSupportNavigateUp()) finish()
        return true
    }

    override fun onDestroy() {
        DataStore.profileCacheStore.unregisterChangeListener(this)
        super.onDestroy()
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        if (key != Key.PROFILE_DIRTY) {
            DataStore.dirty = true
        }
    }

    abstract fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    )

    open fun PreferenceFragmentCompat.viewCreated(view: View, savedInstanceState: Bundle?) {
    }

    open fun PreferenceFragmentCompat.displayPreferenceDialog(preference: Preference): Boolean {
        return false
    }

    class MyPreferenceFragmentCompat : PreferenceFragmentCompat() {

        var activity: ProfileSettingsActivity<*>? = null

        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = DataStore.profileCacheStore
            try {
                activity = (requireActivity() as ProfileSettingsActivity<*>).apply {
                    createPreferences(savedInstanceState, rootKey)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    SagerNet.application,
                    "Error on createPreferences, please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                Logs.e(e)
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            ViewCompat.setOnApplyWindowInsetsListener(listView, ListListener)

            activity?.apply {
                viewCreated(view, savedInstanceState)
                DataStore.dirty = false
                DataStore.profileCacheStore.registerChangeListener(this)
            }
        }

//        override fun onOptionsItemSelected(item: MenuItem) =  {
//            false
//        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            activity?.apply {
                if (displayPreferenceDialog(preference)) return
            }
            super.onDisplayPreferenceDialog(preference)
        }

    }

    object PasswordSummaryProvider : Preference.SummaryProvider<EditTextPreference> {

        override fun provideSummary(preference: EditTextPreference): CharSequence {
            val text = preference.text
            return if (text.isNullOrBlank()) {
                preference.context.getString(androidx.preference.R.string.not_set)
            } else {
                "\u2022".repeat(text.length)
            }
        }

    }

}