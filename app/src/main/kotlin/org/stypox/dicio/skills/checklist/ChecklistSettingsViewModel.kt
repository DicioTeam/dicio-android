package org.stypox.dicio.skills.checklist

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.protobuf.util.JsonFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.stypox.dicio.App
import org.stypox.dicio.di.LocaleManager
import org.stypox.dicio.error.ErrorInfo
import org.stypox.dicio.error.ErrorUtils
import org.stypox.dicio.error.UserAction
import org.stypox.dicio.skills.checklist.ChecklistInfo.checklistDataStore
import org.stypox.dicio.util.toStateFlowDistinctBlockingFirst
import javax.inject.Inject


@HiltViewModel
class ChecklistSettingsViewModel @Inject constructor(
    application: Application,
    val localeManager: LocaleManager,
) : AndroidViewModel(application) {

    val dataStore = application.baseContext.checklistDataStore

    // run blocking because the checklist screen cannot start if checklists have not been loaded yet
    val checklists = dataStore.data.toStateFlowDistinctBlockingFirst(viewModelScope)

    fun addChecklist(checklist: Checklist) {
        viewModelScope.launch {
            dataStore.updateData {
                it.copy { checklists.add(checklist) }
            }
        }
    }

    fun replaceChecklist(index: Int, checklist: Checklist?) {
        viewModelScope.launch {
            dataStore.updateData {
                if (checklist != null) {
                    it.copy { checklists[index] = checklist }
                } else {
                    it.toBuilder().clearChecklists().addAllChecklists(it.checklistsList.filterIndexed { itemIndexed, _ -> index != itemIndexed }).build()
                }
            }
        }
    }

    fun exportChecklists(uri: Uri?) {
        if (uri != null) {
            viewModelScope.launch {
                try {
                    val export = dataStore.data.first()
                    val encoded = JsonFormat.printer().print(export)
                    getApplication<App>().baseContext.contentResolver.openOutputStream(uri).use {
                        it?.write(encoded.toByteArray())
                    }
                } catch (throwable: Throwable) {
                    ErrorUtils.openActivity(getApplication<App>().baseContext, ErrorInfo(throwable, UserAction.EXPORTING_CHECKLIST))
                }
            }
        }
    }

    fun importChecklists(uri: Uri?) {
        if (uri != null) {
            viewModelScope.launch {
                try {
                    val encoded =
                        getApplication<App>().baseContext.contentResolver.openInputStream(uri).use {
                            it?.readBytes()
                        }
                    val builder = SkillSettingsChecklist.newBuilder()
                    JsonFormat.parser().merge(encoded?.decodeToString(), builder)
                    val loaded = builder.build()
                    dataStore.updateData {
                        it.copy {
                            executionLastChecklistIndex = loaded.executionLastChecklistIndex + checklists.size
                            checklists.addAll(loaded.checklistsList)
                        }
                    }
                } catch (throwable: Throwable) {
                    ErrorUtils.openActivity(getApplication<App>().baseContext, ErrorInfo(throwable, UserAction.IMPORTING_CHECKLIST))
                }
            }
        }
    }
}
