package at.techbee.jtx.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import at.techbee.jtx.*
import at.techbee.jtx.database.*
import at.techbee.jtx.database.properties.Attachment
import at.techbee.jtx.database.properties.Attendee
import at.techbee.jtx.database.properties.Category
import at.techbee.jtx.database.properties.Comment
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class IcalViewFragmentTest {


    private lateinit var database: ICalDatabaseDao
    private lateinit var context: Context
    private lateinit var application: Application

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private val sampleDate = 1632636027826L     //  Sun Sep 26 2021 06:00:27
    private val sampleCollection = ICalCollection(collectionId = 1L, displayName = "testcollection automated tests")
    private val sampleJournal = ICalObject.createJournal().apply {
        collectionId = 1L
        summary = "Journal4Test"
        description = "Description4JournalTest"
        dtstart = sampleDate
    }
    private val sampleJournal2 = ICalObject.createJournal().apply {
        collectionId = 1L
        summary = "Journal4TestExtended"
        description = "Description4JournalTestExtended"
        dtstart = sampleDate
    }
    private val sampleNote = ICalObject.createNote("Note4Test").apply {
        collectionId = 1L
        description = "Description4NoteTest"
        dtstart = sampleDate
    }
    private val sampleTodo = ICalObject.createTodo().apply {
        collectionId = 1L
        summary = "Todo4Test"
        description = "Description4TodoTest"
        dtstart = sampleDate
    }

    private val sampleAttendee = Attendee(icalObjectId = sampleJournal.id, caladdress = "contact@techbee.at")
    private val sampleAttachment = Attachment(icalObjectId = sampleJournal.id, uri = "https://techbee.at", filename = "test.pdf")
    private val sampleComment = Comment(icalObjectId = sampleJournal.id, text = "my comment")
    private val sampleCategory1 = Category(text = "cat1")
    private val sampleCategory2 = Category(text = "cat2")

    /*
    database.insertOrganizer(Organizer(caladdress = "organizer", icalObjectId = newEntry))
     */

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ICalDatabase.switchToInMemory(context)
        database = ICalDatabase.getInstance(context).iCalDatabaseDao     // should be in-memory db now
        application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application


        //insert sample entries
        testScope.launch(TestCoroutineDispatcher()) {
            database.insertCollectionSync(sampleCollection)
            sampleJournal.id = database.insertICalObjectSync(sampleJournal)
            sampleNote.id = database.insertICalObjectSync(sampleNote)
            sampleTodo.id = database.insertICalObjectSync(sampleTodo)

            sampleAttendee.icalObjectId = sampleJournal.id
            sampleAttachment.icalObjectId = sampleJournal.id
            sampleComment.icalObjectId = sampleJournal.id
            sampleCategory1.icalObjectId = sampleJournal.id
            sampleCategory2.icalObjectId = sampleJournal.id

            database.insertAttendeeSync(sampleAttendee)
            database.insertAttachmentSync(sampleAttachment)
            database.insertCommentSync(sampleComment)
            database.insertCategorySync(sampleCategory1)
            database.insertCategorySync(sampleCategory2)
        }
    }


    @After
    fun closeDb() {
        ICalDatabase.getInMemoryDB(context).close()
    }


    @Test
    fun journal_everything_is_displayed()  {

        val fragmentArgs = Bundle()
        fragmentArgs.putLong("item2show", sampleJournal.id)
        //val scenario =
        launchFragmentInContainer<IcalViewFragment>(fragmentArgs, R.style.AppTheme, Lifecycle.State.RESUMED)

        val collectionString = sampleCollection.displayName + " (" + sampleCollection.accountName + ")"
        onView(allOf(withId(R.id.view_collection), withText(collectionString))).check(matches(isDisplayed()))
        //onView(withText(sampleCollection.displayName)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.view_summary),withText(sampleJournal.summary))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.view_description),withText(sampleJournal.description))).check(matches(isDisplayed()))

        val expectedDay = convertLongToDayString(sampleJournal.dtstart)
        val expectedMonth = convertLongToMonthString(sampleJournal.dtstart)
        val expectedYear = convertLongToYearString(sampleJournal.dtstart)
        val expectedTime = convertLongToTimeString(sampleJournal.dtstart)

        onView(withId(R.id.view_dtstart_day)).check(matches(withText(expectedDay)))
        onView(withId(R.id.view_dtstart_month)).check(matches(withText(expectedMonth)))
        onView(withId(R.id.view_dtstart_year)).check(matches(withText(expectedYear)))
        onView(withId(R.id.view_dtstart_time)).check(matches(withText(expectedTime)))

        val expectedCreated = application.resources.getString(R.string.view_created_text, Date(sampleJournal.created))
        val expectedLastModified = application.resources.getString(R.string.view_last_modified_text, Date(sampleJournal.lastModified))

        onView(withId(R.id.view_created)).check(matches(withText(expectedCreated)))
        onView(withId(R.id.view_lastModified)).check(matches(withText(expectedLastModified)))

        onView(withId(R.id.view_add_audio_note)).check(matches(isDisplayed()))
        onView(withId(R.id.view_add_note)).check(matches(isDisplayed()))
        onView(withText(R.string.view_feedback_linked_notes)).check(matches(isDisplayed()))
        onView(withId(R.id.view_classification_chip)).check(matches(isDisplayed()))
        onView(withId(R.id.view_status_chip)).check(matches(isDisplayed()))

        onView(withText(sampleCategory1.text)).check(matches(isDisplayed()))
        onView(withText(sampleCategory2.text)).check(matches(isDisplayed()))

        onView(withText(R.string.view_attendees_header)).check(matches(isDisplayed()))
        onView(withText(sampleAttendee.caladdress)).check(matches(isDisplayed()))

        onView(withText(R.string.view_comments_header)).check(matches(isDisplayed()))
        onView(withText(sampleComment.text)).check(matches(isDisplayed()))

        onView(withText(R.string.view_attachments_header)).check(matches(isDisplayed()))
        onView(withText(sampleAttachment.filename)).check(matches(isDisplayed()))
    }

    @Test
    fun note_is_displayed()  {

        val fragmentArgs = Bundle()
        fragmentArgs.putLong("item2show", sampleNote.id)
        //val scenario =
        launchFragmentInContainer<IcalViewFragment>(fragmentArgs, R.style.AppTheme, Lifecycle.State.RESUMED)
        onView(withText(sampleNote.summary)).check(matches(isDisplayed()))
        onView(withText(sampleNote.description)).check(matches(isDisplayed()))

        onView(withId(R.id.view_status_chip)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.view_classification_chip)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun task_is_displayed()  {

        val fragmentArgs = Bundle()
        fragmentArgs.putLong("item2show", sampleTodo.id)
        //val scenario =
        launchFragmentInContainer<IcalViewFragment>(fragmentArgs, R.style.AppTheme, Lifecycle.State.RESUMED)
        onView(withText(sampleTodo.summary)).check(matches(isDisplayed()))
        onView(withText(sampleTodo.description)).check(matches(isDisplayed()))
        onView(withId(R.id.view_progress_checkbox)).check(matches(isDisplayed()))
        onView(withId(R.id.view_progress_label)).check(matches(isDisplayed()))
        onView(withId(R.id.view_progress_percent)).check(matches(isDisplayed()))
        onView(withId(R.id.view_progress_slider)).check(matches(isDisplayed()))
    }


}