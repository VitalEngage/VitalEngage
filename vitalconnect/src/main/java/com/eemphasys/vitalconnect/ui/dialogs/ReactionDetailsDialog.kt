package com.eemphasys.vitalconnect.ui.dialogs

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eemphasys.vitalconnect.common.AppContextHelper
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.ParticipantColorManager
import com.eemphasys.vitalconnect.common.enums.Reaction
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.MessageListViewItem
import com.eemphasys.vitalconnect.databinding.DialogReactionDetailsBinding
import com.eemphasys.vitalconnect.databinding.RowReactionDetailsItemBinding
import com.eemphasys.vitalconnect.misc.log_trace.LogTraceConstants
import com.eemphasys.vitalconnect.viewModel.MessageListViewModel
import com.eemphasys_enterprise.commonmobilelib.EETLog
import com.eemphasys_enterprise.commonmobilelib.LogConstants

class ReactionDetailsDialog : BaseBottomSheetDialogFragment() {

    lateinit var binding: DialogReactionDetailsBinding

    val messageListViewModel by lazyActivityViewModel {
        val conversationSid = requireArguments().getString(ARGUMENT_CONVERSATION_SID)!!
        injector.createMessageListViewModel(applicationContext, conversationSid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogReactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val message = messageListViewModel.selectedMessage ?: run {
                dismiss()
                return
            }

            val reactionsView = binding.editReactions.root
            reactionsView.reactions = message.reactions
            messageListViewModel.selfUser.observe(this) { reactionsView.identity = it.identity }

            reactionsView.onChangeListener = {
                messageListViewModel.setReactions(reactionsView.reactions)
                dismiss()
            }

            binding.participantsList.adapter = ReactionDetailsAdapter(message, messageListViewModel)
        }catch (e:Exception){
            Log.d("ExceptioninReactionDetailsdialog",e.message.toString())
            dismiss()
        }
    }

    companion object {

        private const val ARGUMENT_CONVERSATION_SID = "ARGUMENT_CONVERSATION_SID"

        fun getInstance(conversationSid: String) = ReactionDetailsDialog().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_CONVERSATION_SID, conversationSid)
            }
        }
    }
}

private class ReactionDetailsAdapter(message: MessageListViewItem, messageListViewModel: MessageListViewModel) :
    RecyclerView.Adapter<ReactionDetailsAdapter.ViewHolder>() {

    private val reactions: List<ReactionViewItem>

    init {
        reactions = message.reactions
            .flatMap { (reaction, identityList) ->
                identityList.map { ReactionViewItem(messageListViewModel.getFriendlyName(it), reaction) }
            }
            .sortedWith(Comparator { item1, item2 ->
                item1.reaction.sortOrder.compareTo(item2.reaction.sortOrder)
                    .takeIf { it != 0 }
                    ?.let { return@Comparator it }

                return@Comparator item1.username.compareTo(item2.username)
            })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowReactionDetailsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        holder.binding.participantAvatar.text = Constants.getInitials(reactions[position].username )
        holder.binding.reactionUsername.text = reactions[position].username
        holder.binding.reactionEmoji.text = context.getString(reactions[position].reaction.emoji)

        changeButtonBackgroundColor(
            holder.binding.participantAvatar,
            ParticipantColorManager.getColorForParticipant(reactions[position].username),
            ParticipantColorManager.getDarkColorForParticipant(reactions[position].username)
        )
    }

    override fun getItemCount() = reactions.size

    class ViewHolder(val binding: RowReactionDetailsItemBinding) : RecyclerView.ViewHolder(binding.root)

    data class ReactionViewItem(val username: String, val reaction: Reaction)

    private fun changeButtonBackgroundColor(textView: TextView?, colorid: Int, coloridText: Int) {
        try {
            val background = textView!!.background
            if (background is ShapeDrawable) {
                background.paint.color = colorid
                textView.setTextColor(coloridText)
            } else if (background is GradientDrawable) {
                background.setColor(colorid)
                textView.setTextColor(coloridText)
            } else if (background is ColorDrawable) {
                background.color = colorid
                textView.setTextColor(coloridText)
            }
        } catch (e: Exception) {
            EETLog.error(
                AppContextHelper.appContext, LogConstants.logDetails(
                    e,
                    LogConstants.LOG_LEVEL.ERROR.toString(),
                    LogConstants.LOG_SEVERITY.HIGH.toString()
                ),
                Constants.EX, LogTraceConstants.getUtilityData(
                    AppContextHelper.appContext!!
                )!!
            )
        }
    }
}
