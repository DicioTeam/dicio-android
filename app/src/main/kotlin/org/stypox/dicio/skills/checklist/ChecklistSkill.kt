package org.stypox.dicio.skills.checklist

import com.google.protobuf.Timestamp
import org.dicio.numbers.unit.Number
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.Checklist
import org.stypox.dicio.skills.checklist.ChecklistInfo.checklistDataStore
import org.stypox.dicio.util.StringUtils
import org.stypox.dicio.util.getString
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ChecklistSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Checklist>) :
    StandardRecognizerSkill<Checklist>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Checklist): SkillOutput {
        var output: SkillOutput? = null
        ctx.android.checklistDataStore.updateData {
            it.copy {
                if (checklists.isEmpty()) {
                    output = ChecklistSkillOutput(
                        ctx.getString(R.string.skill_checklist_none_defined),
                        InteractionPlan.FinishInteraction,
                    )
                } else {
                    if (executionLastChecklistIndex >= checklists.size) {
                        executionLastChecklistIndex = 0
                    }
                    when (inputData) {
                        is Checklist.StartList -> {
                            if (!inputData.list.isNullOrBlank()) {
                                val (checklistIndex, distance) = findChecklistByName(
                                    it,
                                    inputData.list
                                )
                                if (distance < 0) {
                                    executionLastChecklistIndex = checklistIndex
                                } else {
                                    output = object : ChecklistYesNoSkillOutput(
                                        ctx.getString(
                                            R.string.skill_checklist_confirm_start,
                                            checklists[checklistIndex].checklistName
                                        )
                                    ) {
                                        override suspend fun onYes(ctx: SkillContext): SkillOutput {
                                            var innerOutput: SkillOutput? = null
                                            ctx.android.checklistDataStore.updateData { innerIt ->
                                                innerIt.copy {
                                                    executionLastChecklistIndex =
                                                        checklistIndex
                                                    val pair = advanceState(
                                                        ctx,
                                                        checklists[executionLastChecklistIndex],
                                                        true, ""
                                                    )
                                                    innerOutput = pair.first
                                                    checklists[executionLastChecklistIndex] =
                                                        pair.second
                                                }
                                            }
                                            return innerOutput!!
                                        }
                                    }
                                }
                            }
                            if (output == null) {
                                val pair = advanceState(
                                    ctx,
                                    checklists[executionLastChecklistIndex],
                                    true,
                                    ""
                                )
                                output = pair.first
                                checklists[executionLastChecklistIndex] = pair.second
                            }
                        }

                        is Checklist.CompleteItem -> {
                            if (!inputData.list.isNullOrBlank()) {
                                val (checklistIndex, distance) = findChecklistByName(
                                    it,
                                    inputData.list
                                )
                                if (distance < 0) {
                                    executionLastChecklistIndex = checklistIndex
                                } else {
                                    output = ChecklistSkillOutput(ctx.getString(R.string.skill_checklist_not_recognized), InteractionPlan.FinishInteraction)
                                }
                            }
                            if (output == null) {
                                val checklist = checklists[executionLastChecklistIndex]
                                var itemIndex: Int? = null
                                if (inputData.itemNumber != null) {
                                    val itemNumberInfo = ctx.parserFormatter?.extractNumber(inputData.itemNumber.trim())?.mixedWithText
                                    val itemNumber: Number? = if (itemNumberInfo != null && itemNumberInfo.size == 1 && itemNumberInfo[0] is Number) {
                                        itemNumberInfo[0] as Number
                                    } else {
                                        null
                                    }
                                    if (itemNumber != null && itemNumber.isInteger && itemNumber.integerValue() >= 1 && itemNumber.integerValue() <= checklist.checklistItemCount) {
                                        itemIndex = itemNumber.integerValue().toInt() - 1
                                    } else {
                                        output = ChecklistSkillOutput(ctx.getString(R.string.skill_checklist_item_unrecognized), InteractionPlan.FinishInteraction)
                                    }
                                }
                                if (itemIndex == null && inputData.item != null) {
                                    val (foundIndex, distance) = findItemByName(checklist, inputData.item)
                                    if (distance < 0) {
                                        itemIndex = foundIndex
                                    } else {
                                        output = ChecklistSkillOutput(ctx.getString(R.string.skill_checklist_item_unrecognized), InteractionPlan.FinishInteraction)
                                    }
                                }
                                if (output == null) {
                                    val pair =
                                        advanceState(
                                            ctx,
                                            markItem(checklist, itemIndex, ItemState.COMPLETED),
                                            false, ""
                                        )
                                    output = pair.first
                                    checklists[executionLastChecklistIndex] = pair.second
                                }
                            }
                        }

                        is Checklist.SkipItem -> {
                            val checklist = checklists[executionLastChecklistIndex]
                            val pair =
                                advanceState(ctx, markItem(checklist, null, ItemState.SKIPPED), false, "")
                            output = pair.first
                            checklists[executionLastChecklistIndex] = pair.second
                        }

                        is Checklist.Wait -> {
                            output = ChecklistSkillOutput(ctx.getString(R.string.skill_checklist_wait_acknowledge), InteractionPlan.FinishInteraction)
                        }

                        is Checklist.QueryItem -> {
                            if (!inputData.list.isNullOrBlank()) {
                                val (checklistIndex, distance) = findChecklistByName(
                                    it,
                                    inputData.list
                                )
                                if (distance < 0) {
                                    executionLastChecklistIndex = checklistIndex
                                } else {
                                    output = ChecklistSkillOutput(ctx.getString(R.string.skill_checklist_not_recognized), InteractionPlan.FinishInteraction)
                                }
                            }
                            if (output == null) {
                                val checklist = checklists[executionLastChecklistIndex]
                                val pair = advanceState(ctx, checklist, false, "")
                                output = pair.first
                                checklists[executionLastChecklistIndex] = pair.second
                            }
                        }

                        is Checklist.ResetList -> {
                            val checklist = checklists[executionLastChecklistIndex]
                            val pair = advanceState(
                                ctx,
                                checklist.copy { executionState = ChecklistState.NOT_STARTED },
                                true, ctx.getString(R.string.skill_checklist_confirm_restart)
                            )
                            output = pair.first
                            checklists[executionLastChecklistIndex] = pair.second
                        }
                    }
                }
            }
        }
        return output!!
    }

    companion object {
        private fun timestampToInstant(timestamp: Timestamp): Instant =
            Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())

        fun timestampToLocal(timestamp: Timestamp): LocalDateTime =
            LocalDateTime.ofInstant(timestampToInstant(timestamp), ZoneId.systemDefault())

        private fun timestampNow(): Timestamp {
            val now = Instant.now()
            return Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
        }

        fun findChecklistByName(checklists: SkillSettingsChecklist, name: String): Pair<Int, Int> =
            checklists.checklistsList.mapIndexedNotNull { index, checklist ->
                Pair(index, StringUtils.customStringDistance(name, checklist.checklistName))
            }
            .minByOrNull { pair -> pair.second }!!

        fun findItemByName(checklist: org.stypox.dicio.skills.checklist.Checklist, name: String): Pair<Int, Int> =
            checklist.checklistItemList.mapIndexedNotNull { index, item ->
                Pair(index, StringUtils.customStringDistance(name, item.itemName))
            }
                .minByOrNull { pair -> pair.second }!!

        private fun formatTime(ctx: SkillContext, timestamp: Timestamp): String {
            val local = timestampToLocal(timestamp)
            val nowLocal = timestampToLocal(timestampNow())

            val formatter: DateTimeFormatter
            if (local.year == nowLocal.year && local.dayOfYear == nowLocal.dayOfYear) {
                formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(ctx.locale)
                // This is today, just use the time.
            } else if (local.year == nowLocal.year && local.dayOfYear >= nowLocal.dayOfYear - 7 && local.dayOfYear < nowLocal.dayOfYear) {
                // Within the past seven days, so can use the day of the week
                formatter = DateTimeFormatter.ofPattern("hh:mm a 'on' EEEE")
            } else if (local.year == nowLocal.year || (local.year == nowLocal.year - 1 && local.monthValue > nowLocal.monthValue)) {
                // In the current year, or last year but in a month that has not happened this year
                formatter = DateTimeFormatter.ofPattern("hh:mm a 'on' MMMM dd")
            } else {
                // Otherwise, we can go long form
                formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(ctx.locale)
            }

            return formatter.format(local)
        }

        private fun startChecklistIfNecessary(
            ctx: SkillContext,
            checklist: org.stypox.dicio.skills.checklist.Checklist,
            now: Timestamp,
            intro: String,
        ): Pair<String?, org.stypox.dicio.skills.checklist.Checklist> =
            if (checklist.executionState == ChecklistState.IN_PROGRESS) {
                Pair(null, checklist)
            } else {
                val title = checklist.checklistName.ifEmpty { ctx.getString(R.string.skill_checklist_unspecified_item_name) }
                // FIXME: Move output text into advance so it applies if we jump into a checklist using another command besides start
                var outputText = intro.ifEmpty {
                    when (checklist.executionState) {
                        ChecklistState.IN_PROGRESS -> ctx.getString(
                            R.string.skill_checklist_continue_checklist,
                            title,
                            formatTime(ctx, checklist.executionStartedAt)
                        )
                        ChecklistState.COMPLETE -> ctx.getString(
                            R.string.skill_checklist_restart_checklist,
                            title,
                            formatTime(ctx, checklist.executionEndedAt)
                        )
                        else -> ctx.getString(R.string.skill_checklist_start_checklist, title)
                    }
                }

                Pair(outputText, checklist.copy {
                    executionStartedAt = now
                    executionState = ChecklistState.IN_PROGRESS
                    executionLastIndex = 0
                    checklistItem.forEachIndexed { index, item ->
                        checklistItem[index] = item.copy {
                            executionState = ItemState.NOT_ASKED
                            executionLastChanged = now
                        }
                    }
                })
            }

        private fun fastForwardChecklist(
            ctx: SkillContext,
            checklist: org.stypox.dicio.skills.checklist.Checklist,
            now: Timestamp,
        ): Pair<String?, org.stypox.dicio.skills.checklist.Checklist> {
            // Fast forward past any items already completed.
            var currentIndex = checklist.executionLastIndex
            while (currentIndex < checklist.checklistItemCount && checklist.checklistItemList[currentIndex].executionState == ItemState.COMPLETED) {
                currentIndex += 1
            }
            // If we hit the end, circle back to any incomplete items.
            var message: String? = null
            var isComplete = false
            if (currentIndex >= checklist.checklistItemCount) {
                isComplete = true
                checklist.checklistItemList.forEachIndexed { index, item ->
                    if (isComplete && item.executionState != ItemState.COMPLETED) {
                        currentIndex = index
                        isComplete = false
                    }
                }
                if (!isComplete) {
                    message = ctx.getString(R.string.skill_checklist_revisit_skipped_items)
                }
            }

            return Pair(message, checklist.copy {
                executionLastIndex = currentIndex
                if (isComplete) {
                    executionEndedAt = now
                    executionState = ChecklistState.COMPLETE
                }
            })
        }

        private fun advanceState(
            ctx: SkillContext,
            checklistValue: org.stypox.dicio.skills.checklist.Checklist,
            isChecklistStart: Boolean,
            intro: String,
        ): Pair<ChecklistSkillOutput, org.stypox.dicio.skills.checklist.Checklist> {
            val now = timestampNow()
            val startPair = startChecklistIfNecessary(ctx, checklistValue, now, intro)
            val startMessage = startPair.first
            val ffpair = fastForwardChecklist(ctx, startPair.second, now)
            val ffmessage = ffpair.first
            var checklist = ffpair.second
            if (checklist.executionState == ChecklistState.COMPLETE) {
                val elapsedTime = Duration.between(timestampToInstant(checklist.executionStartedAt), timestampToInstant(now))
                return Pair(ChecklistSkillOutput(if (startMessage != null) {
                    "$startMessage "
                } else { "" } + ctx.getString(
                    R.string.skill_checklist_complete,
                    renderDuration(ctx, elapsedTime)
                ), InteractionPlan.FinishInteraction), checklist)
            }
            val item = checklist.checklistItemList[checklist.executionLastIndex]
            checklist = checklist.copy {
                checklistItem[checklist.executionLastIndex] = item.copy {
                    executionState = ItemState.ASKED
                    executionLastChanged = now
                }
            }
            return Pair(
                ChecklistSkillOutput(if (startMessage != null) {
                    "$startMessage "
                } else { "" } + if (ffmessage != null) {
                    "$ffmessage "
                } else { "" } + if (isChecklistStart) {
                    if (item.itemName.isBlank()) {
                        ctx.getString(
                            R.string.skill_checklist_start_no_description,
                            checklist.executionLastIndex + 1,
                            checklist.checklistItemCount
                        )
                    } else {
                        ctx.getString(
                            R.string.skill_checklist_start_with_description,
                            checklist.executionLastIndex + 1,
                            checklist.checklistItemCount,
                            item.itemName
                        )
                    }
                } else {
                    item.itemName.ifBlank {
                        ctx.getString(
                            R.string.skill_checklist_next_no_description,
                            checklist.executionLastIndex + 1,
                            checklist.checklistItemCount
                        ) }
                }, InteractionPlan.Continue(true)),
                checklist
            )
        }

        private fun renderDuration(ctx: SkillContext, elapsedTime: Duration): String =
            if (elapsedTime.toDays() > 0) {
                ctx.getString(
                    R.string.skill_checklist_days_hours,
                    elapsedTime.toDays(),
                    elapsedTime.toHours() % 24
                )
            } else if (elapsedTime.toHours() > 0) {
                ctx.getString(
                    R.string.skill_checklist_hours_minutes,
                    elapsedTime.toHours(),
                    elapsedTime.toMinutes() % 60
                )
            } else if (elapsedTime.toMinutes() > 0) {
                ctx.getString(
                    R.string.skill_checklist_minutes_seconds,
                    elapsedTime.toMinutes(),
                    elapsedTime.toSeconds() % 60
                )
            } else {
                ctx.getString(R.string.skill_checklist_seconds, elapsedTime.toSeconds())
            }

        fun markItem(checklist: org.stypox.dicio.skills.checklist.Checklist, itemIndex: Int?, newState: ItemState): org.stypox.dicio.skills.checklist.Checklist =
            checklist.copy {
                val selectedIndex: Int = itemIndex ?: executionLastIndex
                if (selectedIndex < checklistItem.size && checklistItem[selectedIndex].executionState != newState) {
                    checklistItem[selectedIndex] = checklistItem[selectedIndex].copy {
                        executionState = newState
                        executionLastChanged = timestampNow()
                    }
                    executionLastIndex = selectedIndex + 1
                }
            }
    }
}
