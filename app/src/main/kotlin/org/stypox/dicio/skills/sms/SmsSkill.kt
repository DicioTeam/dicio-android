package org.stypox.dicio.skills.sms

import android.telephony.SmsManager
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences.Sms
import org.stypox.dicio.skills.telephone.Contact

class SmsSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Sms>) :
    StandardRecognizerSkill<Sms>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Sms): SkillOutput {
        val contentResolver = ctx.android.contentResolver
        val (userContactName, messageText) = when (inputData) {
            is Sms.Send -> Pair(
                inputData.who?.trim { it <= ' ' } ?: "",
                inputData.what?.trim()
            )
        }
        
        val contacts = Contact.getFilteredSortedContacts(contentResolver, userContactName)
        val validContacts = ArrayList<Pair<String, List<String>>>()
        
        var i = 0
        while (validContacts.size < 5 && i < contacts.size) {
            val contact = contacts[i]
            val numbers = contact.getNumbers(contentResolver)
            if (numbers.isEmpty()) {
                ++i
                continue
            }
            if (validContacts.isEmpty()
                && contact.distance < 3
                && numbers.size == 1 // it has just one number
                && (contacts.size <= i + 1 // the next contact has a distance higher by 3+
                        || contacts[i + 1].distance - 2 > contact.distance)
            ) {
                // very close match with just one number and without distance ties
                return if (messageText == null) {
                    // ask for the message
                    AskMessageOutput(contact.name, numbers[0])
                } else {
                    // we have everything, confirm sending
                    ConfirmSmsOutput(contact.name, numbers[0], messageText)
                }
            }
            validContacts.add(Pair(contact.name, numbers))
            ++i
        }

        if (validContacts.size == 1 // there is exactly one valid contact and ...
            // ... either it has exactly one number, or we would be forced (because no number parser
            // is available) to use ContactChooserName, which only uses the first phone number
            // anyway
            && (validContacts[0].second.size == 1 || ctx.parserFormatter == null)
        ) {
            // not a good enough match, but since we have only this, use it
            val contact = validContacts[0]
            return if (messageText == null) {
                AskMessageOutput(contact.first, contact.second[0])
            } else {
                ConfirmSmsOutput(contact.first, contact.second[0], messageText)
            }
        }

        // this point will not be reached if a very close match was found
        return SmsOutput(validContacts, messageText)
    }

    companion object {
        fun sendSms(number: String, message: String) {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
        }
    }
}
