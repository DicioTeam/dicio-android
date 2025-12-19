package org.stypox.dicio.skills.nextcloud_notes

import kotlinx.coroutines.flow.first
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.NextcloudNotes
import org.stypox.dicio.skills.nextcloud_notes.NextcloudNotesInfo.nextcloudNotesDataStore
import org.stypox.dicio.util.StringUtils
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NextcloudNotesSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<NextcloudNotes>
) : StandardRecognizerSkill<NextcloudNotes>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: NextcloudNotes): SkillOutput {
        val prefs = ctx.android.nextcloudNotesDataStore.data.first()

        if (prefs.serverAddress.isEmpty() || prefs.username.isEmpty() || prefs.password.isEmpty()) {
            return NextcloudNotesOutput.Failed(
                reason = NextcloudNotesOutput.FailureReason.SETTINGS_MISSING
            )
        }

        return when (inputData) {
            is NextcloudNotes.AddNote -> {
                if (prefs.targetNote.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.TARGET_NOTE_MISSING
                    )
                }
                val content = inputData.content ?: ""
                if (content.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONTENT_EMPTY
                    )
                }
                
                try {
                    addNoteViaWebDAV(
                        serverAddress = prefs.serverAddress,
                        username = prefs.username,
                        password = prefs.password,
                        noteName = prefs.targetNote,
                        content = content,
                        isShoppingList = false
                    )
                    return NextcloudNotesOutput.Success(
                        noteName = prefs.targetNote,
                        content = content,
                        isShoppingList = false
                    )
                } catch (e: Exception) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONNECTION_ERROR,
                        errorMessage = e.message
                    )
                }
            }
            
            is NextcloudNotes.AddToShoppingList -> {
                if (prefs.targetShoppingList.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.TARGET_SHOPPING_LIST_MISSING
                    )
                }
                val content = inputData.item ?: ""
                if (content.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONTENT_EMPTY
                    )
                }
                
                try {
                    addNoteViaWebDAV(
                        serverAddress = prefs.serverAddress,
                        username = prefs.username,
                        password = prefs.password,
                        noteName = prefs.targetShoppingList,
                        content = content,
                        isShoppingList = true
                    )
                    return NextcloudNotesOutput.Success(
                        noteName = prefs.targetShoppingList,
                        content = content,
                        isShoppingList = true
                    )
                } catch (e: Exception) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONNECTION_ERROR,
                        errorMessage = e.message
                    )
                }
            }
            
            is NextcloudNotes.QueryShoppingList -> {
                if (prefs.targetShoppingList.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.TARGET_SHOPPING_LIST_MISSING
                    )
                }
                
                try {
                    return queryShoppingList(
                        serverAddress = prefs.serverAddress,
                        username = prefs.username,
                        password = prefs.password,
                        noteName = prefs.targetShoppingList
                    )
                } catch (e: Exception) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONNECTION_ERROR,
                        errorMessage = e.message
                    )
                }
            }
            
            is NextcloudNotes.CheckShoppingList -> {
                if (prefs.targetShoppingList.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.TARGET_SHOPPING_LIST_MISSING
                    )
                }
                val item = inputData.item ?: ""
                if (item.isEmpty()) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONTENT_EMPTY
                    )
                }
                
                try {
                    return checkShoppingListItem(
                        serverAddress = prefs.serverAddress,
                        username = prefs.username,
                        password = prefs.password,
                        noteName = prefs.targetShoppingList,
                        itemToCheck = item
                    )
                } catch (e: Exception) {
                    return NextcloudNotesOutput.Failed(
                        reason = NextcloudNotesOutput.FailureReason.CONNECTION_ERROR,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    private fun buildNoteUrl(serverAddress: String, username: String, noteName: String): String {
        val normalizedServer = serverAddress.trimEnd('/')
        val encodedNoteName = URLEncoder.encode(noteName, "UTF-8")
        return "$normalizedServer/remote.php/dav/files/$username/Notes/$encodedNoteName"
    }

    private suspend fun addNoteViaWebDAV(
        serverAddress: String,
        username: String,
        password: String,
        noteName: String,
        content: String,
        isShoppingList: Boolean
    ) {
        val client = OkHttpClient()
        val noteUrl = buildNoteUrl(serverAddress, username, noteName)
        
        // Get existing content first
        val getRequest = Request.Builder()
            .url(noteUrl)
            .header("Authorization", Credentials.basic(username, password))
            .get()
            .build()
        
        val existingContent = try {
            val response = client.newCall(getRequest).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
        
        // Append new content with timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newContent = if (isShoppingList) {
            // For shopping lists, add as a checkbox item
            if (existingContent.isEmpty()) {
                "- [ ] $content"
            } else {
                "$existingContent\n- [ ] $content"
            }
        } else {
            // For notes, add with timestamp
            if (existingContent.isEmpty()) {
                "[$timestamp] $content"
            } else {
                "$existingContent\n\n[$timestamp] $content"
            }
        }
        
        val putRequest = Request.Builder()
            .url(noteUrl)
            .header("Authorization", Credentials.basic(username, password))
            .put(newContent.toRequestBody("text/plain".toMediaType()))
            .build()
        
        val response = client.newCall(putRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to add note: ${response.code} ${response.message}")
        }
    }
    
    private suspend fun queryShoppingList(
        serverAddress: String,
        username: String,
        password: String,
        noteName: String
    ): NextcloudNotesOutput {
        val content = fetchNoteContent(serverAddress, username, password, noteName)
        val items = parseShoppingListItems(content)
        
        return NextcloudNotesOutput.QueryShoppingListSuccess(
            noteName = noteName,
            items = items
        )
    }

    private suspend fun checkShoppingListItem(
        serverAddress: String,
        username: String,
        password: String,
        noteName: String,
        itemToCheck: String
    ): NextcloudNotesOutput {
        val content = fetchNoteContent(serverAddress, username, password, noteName)
        val items = parseShoppingListItems(content)
        
        val matchedItem = items.firstOrNull { item ->
            StringUtils.customStringDistance(item, itemToCheck.trim()) <= 3
        }
        
        return NextcloudNotesOutput.ItemFound(
            item = matchedItem ?: itemToCheck.trim(),
            found = matchedItem != null
        )
    }

    private suspend fun fetchNoteContent(
        serverAddress: String,
        username: String,
        password: String,
        noteName: String
    ): String {
        val client = OkHttpClient()
        val noteUrl = buildNoteUrl(serverAddress, username, noteName)
        
        val getRequest = Request.Builder()
            .url(noteUrl)
            .header("Authorization", Credentials.basic(username, password))
            .get()
            .build()
        
        val response = client.newCall(getRequest).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch note: ${response.code} ${response.message}")
        }
        
        return response.body?.string() ?: ""
    }
    
    private fun parseShoppingListItems(content: String): List<String> {
        if (content.isBlank()) return emptyList()
        
        val checkboxRegex = Regex("^\\s*-\\s*\\[.?\\]\\s*(.+)$")
        
        return content.lines()
            .mapNotNull { line ->
                checkboxRegex.find(line)?.let { match ->
                    stripMarkdownForSpeech(match.groupValues[1].trim())
                        .takeIf { it.isNotBlank() }
                }
            }
    }
    
    private fun stripMarkdownForSpeech(text: String): String {
        return text
            // Remove markdown headers
            .replace(Regex("^#{1,6}\\s+"), "")
            // Remove bold/italic markers
            .replace(Regex("[*_]{1,2}"), "")
            // Remove links but keep text: [text](url) -> text
            .replace(Regex("\\[([^]]+)\\]\\([^)]+\\)"), "$1")
            // Remove inline code markers
            .replace(Regex("`([^`]+)`"), "$1")
            // Clean up extra whitespace
            .trim()
    }
}
