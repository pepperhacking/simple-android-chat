package com.softbankrobotics.chatbot

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.builder.ChatBuilder
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder
import com.aldebaran.qi.sdk.builder.TopicBuilder

class MainActivity : Activity(), RobotLifecycleCallbacks {
    // The QiContext provided by the QiSDK
    private var qiContext: QiContext? = null

    // Topic and Chat
    private var qiChatbot: QiChatbot? = null
    private var chat: Chat? = null
    private var topic: Topic? = null
    private var chatFuture: Future<Void>? = null

    // Bookmarks
    private var askBookmark: Bookmark? = null
    private var helloBookmark: Bookmark? = null
    private var finishBookmark: Bookmark? = null

    // Bookmark Status
    private var helloBookmarkStatus: BookmarkStatus? = null
    private var finishBookmarkStatus: BookmarkStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Register the RobotLifecycleCallbacks for this Activity.
        QiSDK.register(this, this);
    }

    override fun onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    override fun onRobotFocusGained(qiContext: QiContext?) {
        this.qiContext = qiContext

        initInteraction()
        activateDialog()
    }

    override fun onRobotFocusLost() {
        this.qiContext = null
        chat?.removeAllOnStartedListeners()
    }

    override fun onRobotFocusRefused(reason: String?) {
    }

    private fun initInteraction() {
        // Create a Topic
        topic = TopicBuilder.with(qiContext)
                .withResource(R.raw.chat)
                .build()

        // Create a new QiChatbot
        qiChatbot = QiChatbotBuilder.with(qiContext)
                .withTopic(topic)
                .build()

        // Create a new Chat action.
        chat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .build()

        // Get the bookmarks from the topic
        val bookmarks = topic?.bookmarks

        // Get a bookmark
        askBookmark = bookmarks?.get("ASK")
        helloBookmark = bookmarks?.get("HELLO")
        finishBookmark = bookmarks?.get("FINISH")

        // Create Bookmark Status to interact with them
        finishBookmarkStatus = qiChatbot?.bookmarkStatus(finishBookmark)

        chat?.addOnStartedListener {
            sayScript(askBookmark)
        }

        finishBookmarkStatus?.addOnReachedListener {
            Log.i("TAG", "restart")
            restartInteraction()
        }
    }

    private fun sayScript(Bookmark: Bookmark?) {
        qiChatbot?.async()?.goToBookmark(Bookmark,
                AutonomousReactionImportance.HIGH,
                AutonomousReactionValidity.IMMEDIATE)
    }

    private fun activateDialog() {
        if (chatFuture == null) {
            chatFuture = chat?.async()?.run()
            chatFuture?.thenConsume {
                chatFuture = null // Reset to null
            }
        }
    }

    private fun restartInteraction() {
        chatFuture?.requestCancellation()
    }
}
