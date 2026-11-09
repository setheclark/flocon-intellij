package io.github.setheclark.intellij.graphql

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GraphQlGoToDeclarationTest : BasePlatformTestCase() {

    private val notifications = mutableListOf<Notification>()
    private var chooserOffers: List<GraphQlDeclaration>? = null

    override fun setUp() {
        super.setUp()
        notifications.clear()
        chooserOffers = null
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    notifications += notification
                }
            },
        )
    }

    private fun goTo(
        ref: GraphQlOperationRef,
        onResolved: (String) -> Unit = {},
        choose: (List<GraphQlDeclaration>) -> GraphQlDeclaration = { it.first() },
    ) {
        GraphQlGoToDeclaration(project) { _, matches, onChosen ->
            chooserOffers = matches
            onChosen(choose(matches))
        }.go(ref, onResolved)
    }

    private fun awaitUntil(message: String, condition: () -> Boolean) =
        PlatformTestUtil.waitWithEventsDispatching(message, condition, 10)

    fun testSingleMatchOpensTheFileWithTheCaretAtTheOperation() {
        val text = "# header\n\nquery GetUser(\$id: ID!) {\n  user(id: \$id) { name }\n}\n"
        val file = myFixture.addFileToProject("api/user.graphql", text).virtualFile
        var resolved: String? = null

        goTo(GraphQlOperationRef("GetUser", "query"), onResolved = { resolved = it })
        awaitUntil("file opened") { FileEditorManager.getInstance(project).selectedFiles.contains(file) }

        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        assertEquals(text.indexOf("query GetUser"), editor.caretModel.offset)
        assertEquals("query", resolved)
        assertNull("a single match needs no chooser", chooserOffers)
    }

    fun testUnknownOperationTypeIsTakenFromTheMatchedKeyword() {
        myFixture.addFileToProject("m.gql", "mutation Save { save }")
        var resolved: String? = null

        goTo(GraphQlOperationRef("Save", null), onResolved = { resolved = it })
        awaitUntil("resolved") { resolved != null }

        assertEquals("mutation", resolved)
    }

    fun testSeveralMatchesAreOfferedWithTheCallsOperationTypeFirst() {
        myFixture.addFileToProject("a.graphql", "query Item { item }")
        myFixture.addFileToProject("b.graphqls", "mutation Item { item }")
        myFixture.addFileToProject("c.gql", "subscription Item { item }")

        goTo(GraphQlOperationRef("Item", "mutation"))
        awaitUntil("chooser shown") { chooserOffers != null }

        assertEquals(listOf("mutation", "query", "subscription"), chooserOffers!!.map { it.keyword })
    }

    fun testChosenMatchIsOpened() {
        myFixture.addFileToProject("a.graphql", "query Item { item }")
        val chosen = myFixture.addFileToProject("b.graphql", "mutation Item { item }").virtualFile

        goTo(GraphQlOperationRef("Item", "query")) { matches -> matches.single { it.file == chosen } }
        awaitUntil("file opened") { FileEditorManager.getInstance(project).selectedFiles.contains(chosen) }
    }

    fun testNoMatchNotifiesAndOffersFindInFilesPrefilledWithTheOperationName() {
        myFixture.addFileToProject("a.graphql", "# query Missing { x }\nquery Other { x }\ntype Query { Missing: String }")

        goTo(GraphQlOperationRef("Missing", "query"))
        awaitUntil("notified") { notifications.isNotEmpty() }

        val notification = notifications.single()
        assertTrue(notification.content, notification.content.contains("Missing"))
        assertEquals(listOf("Find in Files"), notification.actions.map { it.templateText })
        assertTrue(FileEditorManager.getInstance(project).selectedFiles.isEmpty())
    }

    fun testActionWaitsForIndexingBeforeLookingUp() {
        val file = myFixture.addFileToProject("a.graphql", "query Slow { x }").virtualFile

        val token = DumbModeTestUtils.startEternalDumbModeTask(project)
        try {
            goTo(GraphQlOperationRef("Slow", "query"))
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            Thread.sleep(300)
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
            assertTrue("must not act while indexing", FileEditorManager.getInstance(project).selectedFiles.isEmpty())
            assertTrue(notifications.isEmpty())
        } finally {
            DumbModeTestUtils.endEternalDumbModeTaskAndWaitForSmartMode(project, token)
        }

        awaitUntil("file opened") { FileEditorManager.getInstance(project).selectedFiles.contains(file) }
    }

    fun testAnonymousOperationIsUnavailableWithAReason() {
        assertNotNull(GraphQlOperationRef(null, "query").unavailableReason)
        assertNotNull(GraphQlOperationRef("  ", "query").unavailableReason)
        assertNull(GraphQlOperationRef("Named", null).unavailableReason)
    }

    fun testAnonymousOperationDoesNotSearchOrFail() {
        myFixture.addFileToProject("a.graphql", "query { x }")

        goTo(GraphQlOperationRef(null, "query"))

        assertEquals(1, notifications.size)
        assertNull(chooserOffers)
        assertTrue(FileEditorManager.getInstance(project).selectedFiles.isEmpty())
    }
}
