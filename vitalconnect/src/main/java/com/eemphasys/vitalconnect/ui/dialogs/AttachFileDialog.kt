package com.eemphasys.vitalconnect.ui.dialogs

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.core.content.FileProvider
import com.eemphasys.vitalconnect.common.ChatAppModel
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.enums.ConversationsError
import com.eemphasys.vitalconnect.common.extensions.applicationContext
import com.eemphasys.vitalconnect.common.extensions.getString
import com.eemphasys.vitalconnect.common.extensions.lazyActivityViewModel
import com.eemphasys.vitalconnect.common.extensions.parcelable
import com.eemphasys.vitalconnect.common.injector
import com.eemphasys.vitalconnect.data.models.MediaMessagePreviewItem
import com.eemphasys.vitalconnect.databinding.DialogAttachFileBinding
import com.eemphasys_enterprise.commonmobilelib.EETLog
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class AttachFileDialog : BaseBottomSheetDialogFragment() {

    lateinit var binding: DialogAttachFileBinding

    var imageCaptureUri = Uri.EMPTY

    val messageListViewModel by lazyActivityViewModel {
        val conversationSid = requireArguments().getString(ARGUMENT_CONVERSATION_SID)!!
        injector.createMessageListViewModel(applicationContext, conversationSid)
    }

    private val takePicture = registerForActivityResult(TakePicture()) { success ->
        if (success){
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(imageCaptureUri)
            val sizeInBytes: Long = getAttachmentSize(inputStream)
            val sizeInMB: Double = sizeInBytes / (1024.0 * 1024.0) // Convert bytes to MB
            if(Constants.CURRENT_CONVERSATION_ISWEBCHAT == "false" && sizeInMB < 5){
            sendMediaMessage(imageCaptureUri)
            }
            else if(Constants.CURRENT_CONVERSATION_ISWEBCHAT == "true" && sizeInMB < 20){
                sendMediaMessage(imageCaptureUri)
            }
            else{
                messageListViewModel.onMessageError.value = ConversationsError.FILE_TOO_LARGE
            }}
        dismiss()
    }

    private val openDocument = registerForActivityResult(OpenDocument()) { uri: Uri? ->
        if(uri != null) {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri!!)
            val type = contentResolver.getType(uri)
            val sizeInBytes: Long = getAttachmentSize(inputStream)
            val sizeInKB: Double = sizeInBytes / 1024.0  // Convert bytes to KB
            val sizeInMB: Double = sizeInBytes / (1024.0 * 1024.0) // Convert bytes to MB
            when (type) {
                "application/pdf" -> {
                    if (Constants.CURRENT_CONVERSATION_ISWEBCHAT == "false" && sizeInKB < 600) {
                        uri?.let { sendMediaMessage(it) }
                    } else if (Constants.CURRENT_CONVERSATION_ISWEBCHAT == "true" && sizeInMB < 20) {
                        uri?.let { sendMediaMessage(it) }
                    } else {
                        messageListViewModel.onMessageError.value =
                            ConversationsError.FILE_TOO_LARGE
                    }
                }

                "image/jpeg", "image/png", "image/jpg" -> {
                    if(Constants.CURRENT_CONVERSATION_ISWEBCHAT == "false" && sizeInMB < 5){
                        uri?.let { sendMediaMessage(it) }
                    }
                    else if(Constants.CURRENT_CONVERSATION_ISWEBCHAT == "true" && sizeInMB < 20){
                        uri?.let { sendMediaMessage(it) }
                    }
                    else{
                        messageListViewModel.onMessageError.value = ConversationsError.FILE_TOO_LARGE
                    }
                }

                else -> {
                    messageListViewModel.onMessageError.value =
                        ConversationsError.INVALID_CONTENT_TYPE
                }
            }
        }
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { imageCaptureUri = it.parcelable(IMAGE_CAPTURE_URI) }
        EETLog.saveUserJourney(this::class.java.simpleName + " onCreate Called")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogAttachFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            binding.takePhoto.visibility = View.GONE
        }

        binding.takePhoto.setOnClickListener {
            startImageCapture()
        }

        binding.fileManager.setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IMAGE_CAPTURE_URI, imageCaptureUri)
    }

    private fun startImageCapture() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", picturesDir)
        imageCaptureUri =
            FileProvider.getUriForFile(requireContext(), ChatAppModel.appId + ".provider", photoFile)

        takePicture.launch(imageCaptureUri)
    }

    fun sendMediaMessage(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val type = contentResolver.getType(uri)
        val name = contentResolver.getString(uri, OpenableColumns.DISPLAY_NAME)



        if (inputStream != null) {
            Constants.URI = uri.toString()
            Constants.INPUTSTREAM = inputStream
            Constants.MEDIA_NAME = name
            Constants.MEDIA_TYPE = type
            messageListViewModel.mediaMessagePreview.value = MediaMessagePreviewItem(uri.toString(),inputStream,name,type)
//            messageListViewModel.sendMediaMessage(uri.toString(), inputStream, name, type)
        } else {
            messageListViewModel.onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    private fun getAttachmentSize(inputStream: InputStream?): Long {
        if (inputStream == null) return 0

        return try {
            // Calculate the size of the attachment
            inputStream.use { stream ->
                var totalBytes: Long = 0
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead.toLong()
                }
                totalBytes
            }
        } catch (e: Exception) {
            // Handle exceptions (e.g., IOException)
            e.printStackTrace()
            0
        }
    }


    companion object {

        private const val IMAGE_CAPTURE_URI = "IMAGE_CAPTURE_URI"

        private const val ARGUMENT_CONVERSATION_SID = "ARGUMENT_CONVERSATION_SID"

        fun getInstance(conversationSid: String) = AttachFileDialog().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_CONVERSATION_SID, conversationSid)
            }
        }
    }
}
