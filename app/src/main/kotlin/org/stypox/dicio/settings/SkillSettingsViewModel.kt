package org.stypox.dicio.settings

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.di.SkillContextInternal
import org.stypox.dicio.settings.datastore.UserSettings
import org.stypox.dicio.eval.SkillHandler
import org.stypox.dicio.util.toStateFlowDistinctBlockingFirst
import javax.inject.Inject


@HiltViewModel
class SkillSettingsViewModel @Inject constructor(
    application: Application,
    private val dataStore: DataStore<UserSettings>,
    val skillContext: SkillContextInternal,
    private val skillHandler: SkillHandler,
) : AndroidViewModel(application) {

    val skills: List<SkillInfo> get() = skillHandler.allSkillInfoList

    // run blocking because the settings screen cannot start if settings have not been loaded yet
    val enabledSkills = dataStore.data
        .map { it.enabledSkillsMap }
        .toStateFlowDistinctBlockingFirst(viewModelScope)

    val numberLibraryNotAvailable = skillContext.parserFormatter == null

    fun setSkillEnabled(id: String, state: Boolean) {
        viewModelScope.launch {
            dataStore.updateData {
                it.toBuilder()
                    .putEnabledSkills(id, state)
                    .build()
            }
        }
    }
}
