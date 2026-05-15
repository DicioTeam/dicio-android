package org.stypox.dicio.screenshot

import android.Manifest
import android.view.WindowInsets
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.core.DataStore
import androidx.test.rule.GrantPermissionRule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.stypox.dicio.MainActivity
import org.stypox.dicio.cldr.CldrLanguages.LocaleAndTranslation
import org.stypox.dicio.di.SttInputDeviceWrapper
import org.stypox.dicio.di.SttInputDeviceWrapperModule
import org.stypox.dicio.di.WakeDeviceWrapper
import org.stypox.dicio.di.WakeDeviceWrapperModule
import org.stypox.dicio.eval.SkillEvaluator
import org.stypox.dicio.eval.SkillEvaluatorModule
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.settings.datastore.Theme
import org.stypox.dicio.settings.datastore.UserSettings
import org.stypox.dicio.settings.datastore.copy
import org.stypox.dicio.skills.calculator.CalculatorInfo
import org.stypox.dicio.skills.calculator.CalculatorOutput
import org.stypox.dicio.skills.current_time.CurrentTimeInfo
import org.stypox.dicio.skills.current_time.CurrentTimeOutput
import org.stypox.dicio.skills.joke.JokeInfo
import org.stypox.dicio.skills.joke.JokeOutput
import org.stypox.dicio.skills.lyrics.LyricsInfo
import org.stypox.dicio.skills.lyrics.LyricsOutput
import org.stypox.dicio.skills.media.MediaInfo
import org.stypox.dicio.skills.media.MediaOutput
import org.stypox.dicio.skills.search.SearchInfo
import org.stypox.dicio.skills.search.SearchOutput
import org.stypox.dicio.skills.telephone.ConfirmCallOutput
import org.stypox.dicio.skills.telephone.ConfirmedCallOutput
import org.stypox.dicio.skills.telephone.TelephoneInfo
import org.stypox.dicio.skills.timer.TimerInfo
import org.stypox.dicio.skills.timer.TimerOutput
import org.stypox.dicio.skills.translation.TranslationInfo
import org.stypox.dicio.skills.translation.TranslationOutput
import org.stypox.dicio.skills.weather.ResolvedLengthUnit
import org.stypox.dicio.skills.weather.ResolvedTemperatureUnit
import org.stypox.dicio.skills.weather.WeatherInfo
import org.stypox.dicio.skills.weather.WeatherOutput
import org.stypox.dicio.ui.home.Interaction
import org.stypox.dicio.ui.home.InteractionLog
import org.stypox.dicio.ui.home.QuestionAnswer
import javax.inject.Inject
import javax.inject.Singleton

@UninstallModules(
    SttInputDeviceWrapperModule::class,
    WakeDeviceWrapperModule::class,
    SkillEvaluatorModule::class,
)
@HiltAndroidTest
class ScreenshotTakerTest {
    @Module
    @InstallIn(SingletonComponent::class)
    class FakeSttInputDeviceWrapperModule {
        @Provides
        @Singleton
        fun provideInputDeviceWrapper(): SttInputDeviceWrapper {
            return FakeSttInputDeviceWrapper()
        }
    }
    @Module
    @InstallIn(SingletonComponent::class)
    class FakeWakeDeviceWrapperModule {
        @Provides
        @Singleton
        fun provideWakeDeviceWrapper(): WakeDeviceWrapper {
            return FakeWakeDeviceWrapper()
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    class FakeSkillEvaluatorModule {
        @Provides
        @Singleton
        fun provideSkillEvaluator(): SkillEvaluator {
            return FakeSkillEvaluator()
        }
    }


    @Inject
    lateinit var sttInputDeviceWrapper: SttInputDeviceWrapper

    private val fakeSttInputDeviceWrapper: FakeSttInputDeviceWrapper get() =
        sttInputDeviceWrapper as FakeSttInputDeviceWrapper

    @Inject
    lateinit var skillEvaluator: SkillEvaluator

    private val fakeSkillEvaluator: FakeSkillEvaluator get() =
        skillEvaluator as FakeSkillEvaluator
    
    @Inject
    lateinit var dataStore: DataStore<UserSettings>


    // must run before anything else
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // needed so that the SttButton does not show the "microphone permission needed" message
    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    // needed to make hiding status/navigation bars instantaneous
    @get:Rule(order = 2)
    val disableAnimationsRule = DisableAnimationsRule()

    @get:Rule(order = 3)
    val composeRule = createAndroidComposeRule<MainActivity>()


    private val coilEventListener = CoilEventListener()


    @Before
    fun init() {
        hiltRule.inject()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun takeScreenshots() = runTest {
        composeRule.activity.runOnUiThread {
            composeRule.activity.window.decorView.windowInsetsController!!
                .hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        coilEventListener.setup(composeRule.activity)

        // ensure the drawer/back button is out of focus by clicking in a random place,
        // otherwise there is darkened indication around it
        composeRule.onRoot().performTouchInput { click(center) }

        // make sure the app bar shows "Dicio" and not "Dicio-main" or other branch names
        composeRule.onNodeWithText("Dicio").assertExists()
        composeRule.onNodeWithText("Dicio-main").assertDoesNotExist()


        // screenshot 0: home screen with "Here is what I can do" and STT listening
        dataStore.updateData { it.copy { theme = Theme.THEME_DARK } }
        fakeSttInputDeviceWrapper.uiState.emit(SttState.Listening)
        composeRule.takeScreenshot("en-US", "0")

        // screenshot 1: home screen with interactions with weather, timer and lyrics skills
        dataStore.updateData { it.copy { theme = Theme.THEME_LIGHT } }
        fakeSttInputDeviceWrapper.uiState.emit(SttState.Loaded)
        coilEventListener.resetStartedImages()
        fakeSkillEvaluator.state.value = screenshot1InteractionLog
        composeRule.onNodeWithTag("interaction_list")
            .performScrollToIndex(3) // scroll to the first interaction
        runCatching { composeRule.waitUntil { coilEventListener.isIdle(startedAtLeast = 1) } }
        composeRule.takeScreenshot("en-US", "1")

        // screenshot 2: home screen with interactions with calculator, telephone and search skills
        dataStore.updateData { it.copy { theme = Theme.THEME_BLACK } }
        fakeSttInputDeviceWrapper.uiState.emit(SttState.Loaded)
        coilEventListener.resetStartedImages()
        fakeSkillEvaluator.state.value = screenshot2InteractionLog
        composeRule.onNodeWithTag("interaction_list")
            .performScrollToIndex(3) // scroll to the first interaction
        runCatching { composeRule.waitUntil { coilEventListener.isIdle(startedAtLeast = 2) } }
        composeRule.takeScreenshot("en-US", "2")

        // screenshot 3: home screen with interactions with translation, joke and media skills
        dataStore.updateData { it.copy { theme = Theme.THEME_LIGHT } }
        fakeSttInputDeviceWrapper.uiState.emit(SttState.Loaded)
        fakeSkillEvaluator.state.value = screenshot3InteractionLog
        composeRule.onNodeWithTag("interaction_list")
            .performScrollToIndex(3) // scroll to the second interaction
        composeRule.takeScreenshot("en-US", "3")

        // screenshot 5: settings screen
        dataStore.updateData { it.copy { theme = Theme.THEME_LIGHT } }
        composeRule.onNodeWithTag("drawer_handle")
            .performClick() // open the drawer
        composeRule.onNodeWithTag("settings_drawer_item")
            .performClick() // open the settings screen
        composeRule.takeScreenshot("en-US", "5")

        // screenshot 4: skill settings screen
        dataStore.updateData { it.copy { theme = Theme.THEME_DARK } }
        composeRule.onNodeWithTag("skill_settings_item")
            .performClick() // open the skill settings screen
        composeRule.onAllNodesWithTag("expand_skill_settings_handle").apply {
            get(0).performClick()
        } // expand all skill settings
        composeRule.takeScreenshot("en-US", "4")
    }

    companion object {
        private val screenshot1InteractionLog = InteractionLog(listOf(
            Interaction(WeatherInfo, listOf(
                QuestionAnswer(
                    question = "what's the weather in milan",
                    answer = WeatherOutput.Success(
                        city = "Milan",
                        description = "Few clouds",
                        iconUrl = "https://openweathermap.org/img/wn/02d@2x.png",
                        temp = 8.8,
                        tempMin = 7.2,
                        tempMax = 10.2,
                        tempString = "nine",
                        windSpeed = 1.8,
                        temperatureUnit = ResolvedTemperatureUnit.CELSIUS,
                        lengthUnit = ResolvedLengthUnit.METRIC,
                    )
                )
            )),
            Interaction(TimerInfo, listOf(
                QuestionAnswer(
                    question = "set a timer for two minutes thirty seconds",
                    answer = TimerOutput.Set(
                        milliseconds = 150000L,
                        lastTickMillis = mutableLongStateOf(139000L),
                        name = null,
                    )
                )
            )),
            Interaction(LyricsInfo, listOf(
                QuestionAnswer(
                    question = "lyrics bohemian rhapsody",
                    answer = LyricsOutput.Success(
                        title = "Bohemian Rhapsody",
                        artist = "Queen",
                        lyrics = "[Intro]\n" +
                                "Is this the real life? Is this just fantasy?\n" +
                                "Caught in a landslide, no escape from reality\n" +
                                "Open your eyes, look up to the skies and see\n" +
                                "I'm just a poor boy, I need no sympathy\n" +
                                "Because I'm easy come, easy go, little high, little low\n" +
                                "Any way the wind blows doesn't really matter to me, to me\n" +
                                "\n" +
                                "[Verse 1]\n" +
                                "Mama, just killed a man\n" +
                                "Put a gun against his head, pulled my trigger, now he's dead\n" +
                                "Mama, life had just begun\n" +
                                "But now I've gone and thrown it all away\n" +
                                "Mama, ooh, didn't mean to make you cry\n" +
                                "If I'm not back again this time tomorrow\n" +
                                "Carry on, carry on as if nothing really matters"
                    )
                )
            )),
        ), null)

        private val screenshot2InteractionLog = InteractionLog(listOf(
            Interaction(CalculatorInfo, listOf(
                QuestionAnswer(
                    question = "what is twelve plus three fifths minus two to the power of three",
                    answer = CalculatorOutput(
                        result = "4.6",
                        spokenResult = "four and three fifths",
                        inputInterpretation = "12 + 0.6 - 2 ^ 3"
                    )
                )
            )),
            Interaction(TelephoneInfo, listOf(
                QuestionAnswer(
                    question = "call michael",
                    answer = ConfirmCallOutput(
                        "Michael Smith",
                        "0123 456789"
                    )
                ),
                QuestionAnswer(
                    question = "go for it",
                    answer = ConfirmedCallOutput(
                        "0123 456789"
                    )
                ),
            )),
            Interaction(SearchInfo, listOf(
                QuestionAnswer(
                    question = "search newpipe",
                    answer = SearchOutput.Results(listOf(
                        SearchOutput.Data(
                            title = "NewPipe - a free YouTube client",
                            description = "NewPipe is an Android app that lets you watch videos " +
                                    "and live streams from YouTube and other platforms without " +
                                    "ads or permissions. It is fast, lightweight, privacy " +
                                    "friendly and supports offline usage, background player, " +
                                    "subscriptions and more features.",
                            url = "https://newpipe.net",
                            thumbnailUrl = "https://external-content.duckduckgo.com/ip3/newpipe.net.ico"
                        ),
                        SearchOutput.Data(
                            title = "GitHub - TeamNewPipe/NewPipe: A libre lightweight streaming front-end for Android.",
                            description = "The NewPipe project aims to provide a private, " +
                                    "anonymous experience for using web-based media services. " +
                                    "Therefore, the app does not collect any data without your " +
                                    "consent. NewPipe's privacy policy explains in detail what " +
                                    "data is sent and stored when you send a crash report, or " +
                                    "leave a comment in our blog. You can find the document here.",
                            url = "https://github.com/TeamNewPipe/NewPipe",
                            thumbnailUrl = "https://external-content.duckduckgo.com/ip3/github.com.ico"
                        ),
                    ))
                ),
            )),
        ), null)

        private val screenshot3InteractionLog = InteractionLog(listOf(
            Interaction(MediaInfo, listOf(
                QuestionAnswer(
                    question = "pause the song",
                    answer = MediaOutput(Sentences.Media.Pause)
                )
            )),
            Interaction(TranslationInfo, listOf(
                QuestionAnswer(
                    question = "translate hola from spanish to italian",
                    answer = TranslationOutput.Success(
                        translation = "Ciao",
                        sourceLanguage = LocaleAndTranslation("es", "Spanish", ""),
                        targetLanguage = LocaleAndTranslation("it", "Italian", ""),
                    )
                )
            )),
            Interaction(JokeInfo, listOf(
                QuestionAnswer(
                    question = "tell me a joke",
                    answer = JokeOutput.Success(
                        setup = "What is the tallest building in the world?",
                        delivery = "The library, it's got the most stories!",
                    )
                )
            )),
            Interaction(CurrentTimeInfo, listOf(
                QuestionAnswer(
                    question = "what time is it",
                    answer = CurrentTimeOutput("13:37"),
                )
            )),
        ), null)
    }
}
