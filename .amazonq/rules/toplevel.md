Dicio is a free and open source voice assistant running on Android. It supports many different skills and input/output methods, and it provides both speech and graphical feedback to a question. It interprets user input and (when possible) generates user output entirely on-device, providing privacy by design. It has multilanguage support, and is currently available in these languages: Czech, Dutch, English, French, German, Greek, Italian, Polish, Russian, Slovenian, Spanish, Swedish and Ukrainian. Open to contributions :-D

Skills

Currently Dicio answers questions about:

    search: looks up information on DuckDuckGo (and in the future more engines) - Search for Dicio
    weather: collects weather information from OpenWeatherMap - What's the weather like?
    lyrics: shows Genius lyrics for songs - What's the song that goes we will we will rock you?
    open: opens an app on your device - Open NewPipe
    calculator: evaluates basic calculations - What is four thousand and two times three minus a million divided by three hundred?
    telephone: view and call contacts - Call Tom
    timer: set, query and cancel timers - Set a timer for five minutes
    current time: query current time - What time is it?
    navigation: opens the navigation app at the requested position - Take me to New York, fifteenth avenue
    jokes: tells you a joke - Tell me a joke
    media: play, pause, previous, next song
    translation: translate from/to any language with Lingva - How do I say Football in German?
    wake word control: turn on/off the wakeword - Stop listening


Adding skills

A skill is a component that enables the assistant to understand some specific queries and act accordingly. While reading the instructions, keep in mind the javadocs of the methods being implemented and the code of the already implemented skills. In order to add a skill to Dicio you have to follow the steps below.

$skill_id$ and $SkillId$ indicate the computer readable name of the skill, in snake_case and PascalCase, e.g. weather or Weather
1. Reference sentences

The new skill most likely needs to interpret user input. The Dicio framework provides a standard way to define how to efficiently match user input and extract information from it, in the form of translatable reference sentences stored in YAML files. Note that for some specific cases the standard recognizer might not be wanted, in which case you can skip this section, and in section 3 extend Skill<> and implement Skill.score() manually, instead of extending StandardRecognizerSkill<>.

    Edit the app/src/main/sentences/skill_definitions.yml file and add a definition for the new skill:

      # The unique ID of the skill.
    - id: $skill_id$
      # `SPECIFICITY` can be `high`, `medium` or `low`.
      # It should be chosen wisely: for example, a section that matches queries
      # about phone calls is very specific, while one that matches every question
      # about famous people has a lower specificity.
      specificity: SPECIFICITY
      # A list of definitions for the types of sentences this skill can interpret.
      # Can contain multiple sentences, e.g. the timer skill has the
      # "set", "cancel" and "query" sentences.
      sentences:
          # An ID for the sentence, must be unique amongst this skill's sentences.
        - id: SENTENCE_1_ID
          # (optional) If this sentence has some capturing groups, their IDs and
          # types must be listed here.
          captures:
              # An ID for the capturing group, must be unique amongst this
              # sentence's capturing groups
            - id: CAPTURING_GROUP_1_ID
              # Currently only string capturing groups are supported, but in
              # the future "number", "duration" and "date" will also be possible.
              # For the moment use "string" and then manually parse the string to
              # number, duration or date using dicio-numbers.
              type: string

Create a file named $skill_id$.yml (e.g. weather.yml) under app/src/main/sentences/en/: it will contain the sentences the skill should recognize.

For each of the sentence definitions in skill_definitions.yml, write the id of each sentence type followed by : and a list of sentences:

SENTENCE_1_ID:
  - a<n?> sentence|phrase? alternative # ...
  - another sentence|phrase? alternative with .CAPTURING_GROUP_1_ID. # ...
  # ...
# SENTENCE_2_ID: ... in case you have multiple sentence types

    Write the reference sentences according to the dicio-sentences-language's syntax.

    Try to build the app: if it succeeds you did everything right, otherwise you will get errors pointing to syntax errors in the .yml files.

Here is an example of the weather skill definition in skill_definitions.yml:

- id: weather
  specificity: high
  sentences:
    - id: current
      captures:
        - id: where
          type: string

And these are the example contents of app/src/main/sentences/en/weather.yml:

current:
  - (what is|s)|whats the weather like? (in|on .where.)?
  - weather (in|on? .where.)?
  - how is it outside

2. Subpackage

Create a subpackage that will contain all of the classes you are about to add: org.stypox.dicio.skills.SKILLID (e.g. org.stypox.dicio.skills.weather).
3. The Skill class

Create a class named $SkillId$Skill (e.g. WeatherSkill): it will contain the code that interprets user input (i.e. the score() function) and that processes it to generate output (i.e. the generateOutput() function). The next few points assume that you want to use the standard recognizer with the skill definition and sentences you created in step 1. In that case score() is actually already implemented and you don't need to provide an implementation yourself.

    Have the $SkillId$Skill class implement StandardRecognizerSkill<$SkillId$>. You can import the $SkillId$ class with import org.stypox.dicio.sentences.Sentences.$SkillId$. The Sentences.$SkillId$ sealed class is generated based on skill_definitions.yml, and contains one subclass for each of the defined sentence types.
    The constructor of Skill takes SkillInfo (see step 5), and moreover the constructor of StandardRecognizerSkill takes StandardRecognizerData<$SkillId$> (the data generated from the sentences, see step 5). You should expose these two parameters in $SkillId$Skill's constructor, too.
    Implement the following function: override suspend fun generateOutput(ctx: SkillContext, inputData: $SkillId$): SkillOutput. inputData is, again, an instance of Sentences.$SkillId$ corresponding to the matched sentence type, and its fields contain type-safe information about the data captured in capturing groups (if any).
    Any code making network requests or heavy calculations should be put in generateOutput (which is a suspend function for this exact purpose). The returned SkillOutput should contain all of the data needed to actually show the output, i.e. it shouldn't do any more network requests or calculations (unless it's an interactive widget and the user presses some button, but that's not too relevant for the matter at hand).

This is a stub implementation of the WeatherSkill:

package org.stypox.dicio.skills.weather
import org.stypox.dicio.sentences.Sentences.Weather
// ...
class WeatherSkill(correspondingSkillInfo: SkillInfo, data: StandardRecognizerData<Weather>) :
    StandardRecognizerSkill<Weather>(correspondingSkillInfo, data) {
    override suspend fun generateOutput(ctx: SkillContext, inputData: Weather): SkillOutput {
      return // ...
    }
}

4. SkillOutput

Create a class named $SkillId$Output (e.g. WeatherOutput): it will contain the code that creates a Jetpack Compose UI and provides speech output.

    The class should be constructed by Skill.generateOutput() with all of the data needed to display/speak output, and is meant to be serializable (so in most cases it is a data class). In some cases it might make sense to have multiple types of output (e.g. the weather has Success and Failed output types): in that case you can create a sealed interface and have both output types extend it.
    getSpeechOutput() returns a localized string that will be spoken via the configured Text To Speech service.
    @Composable GraphicalOutput() builds the UI that will be shown in a box on the home screen. The UI can be interactive and can act as a widget: for example the timer skill shows the ongoing countdown.
    [Optional] getNextSkills() returns a list of skills that could continue the current conversation. If this list is non-empty, the next time the user asks something to the assistant, these skills will be considered before all other skills, and if any of these skills understands the user input well enough, the conversation continues. For example, if the user says "Call Mom", the assistant may answer with "Should I call mom?" and this method would return a skill that can understand a yes/no response.

This is a stub implementation of WeatherOutput:

data class WeatherOutput(
    val city: String,
    val description: String,
    // ...
) : WeatherOutput {
    override fun getSpeechOutput(ctx: SkillContext): String = ctx.getString(
        R.string.skill_weather_in_city_there_is_description, city, description
    )

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        // Jetpack Compose UI
    }
}

5. SkillInfo

Create an object named $SkillId$Info (e.g. WeatherInfo) overriding SkillInfo: it will contain all of the information needed to manage your skill.

    This is not a class, but an object, because it makes no sense to instantiate it multiple times.
    Call the SkillInfo constructor with the "$skill_id$" string.
    Provide sensible values for name(), sentenceExample() and icon().
    Override the isAvailable() method and return whether the skill can be used under the circumstances the user is in (e.g. check whether the recognizer sentences are translated into the user language with Sentences.$SkillId$[ctx.sentencesLanguage] != null (see step 1 and step 5.5) or check whether ctx.parserFormatter != null, if your skill uses number parsing and formatting).
    Override the build() method so that it returns an instance of $SkillId$Skill with $SkillId$Info as correspondingSkillInfo and, if the skill uses standard recognizer sentences (see step 1), with Sentences.$SkillId$[ctx.sentencesLanguage] as data.
    [Optional] If your skill wants to present some preferences to the user, it has to do so by overriding the renderSettings value (which by default returns null to indicate there are no preferences).

6. List skill inside SkillHandler

Under org.stypox.dicio.Skills.SkillHandler, update the allSkillInfoList by adding $SkillId$Info; this will make the new skill finally visible to Dicio.
Notes

    The ctx: SkillContext object, that appears here and there in the implementation, allows accessing the Android context, the number parser/formatter and other resources and services, similarly to Android's context.
    The names used for things (files, classes, packages, sections, etc.) are not mandatory, but they help avoiding confusion, so try to stick to them.
    When committing changes about a skill, prefix the commit message with "[$SkillId$]", e.g. "[Weather] Fix crash".
    Add your skill with a short description and an example in the README under Skills and in the fastlane's long description.
    If you have any question, don't hesitate to ask. 😃


