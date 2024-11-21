package com.eemphasys.vitalconnect.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.emoji.widget.EmojiTextView
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.enums.Reaction
import com.eemphasys.vitalconnect.common.enums.Reactions
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import kotlin.properties.Delegates

class EditReactionsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var identity by Delegates.observable("") { _, _, _ -> update() }

    var reactions: Reactions by Delegates.observable(emptyMap()) { _, _, _ -> update() }

    var onChangeListener = {}

    private var myReactions: Set<Reaction> = emptySet()

    override fun onFinishInflate() {
        super.onFinishInflate()
        children
            .mapNotNull { it as? EmojiTextView }
            .forEach { emojiView ->
                val reaction = Reaction.fromEmoji(context, emojiView.text.toString())
                emojiView.setOnClickListener { onReactionClicked(reaction) }
            }
    }

    private fun update() {
        myReactions = reactions
            .filter { identity in it.value }
            .map { it.key }
            .toSet()

        children
            .mapNotNull { it as? EmojiTextView }
            .forEach {
                val reaction = Reaction.fromEmoji(context, it.text.toString())
                it.isSelected = reaction in myReactions
            }
    }

    private fun onReactionClicked(reaction: Reaction) {
        if (reaction in myReactions) {
            removeReaction(reaction)
        } else {
            addReaction(reaction)
        }

        onChangeListener()

        ChatAppModel.FirebaseLogEventListener?.buttonLogEvent(context, "VC_MessageList_ReactionClick",
            "MessageList",
            "EditReactionsView"
        )

    }

    private fun addReaction(reaction: Reaction) {
        for (existingReaction in myReactions) {
            reactions= reactions.toMutableMap().apply{
                put(existingReaction, getOrPut(existingReaction, ::emptySet) - identity)
            }
        }
        reactions = reactions.toMutableMap().apply {
            put(reaction, getOrPut(reaction, ::emptySet) + identity)
        }
    }

    private fun removeReaction(reaction: Reaction) {
        reactions = reactions.toMutableMap().apply {
            put(reaction, getOrPut(reaction, ::emptySet) - identity)
        }
    }
}
