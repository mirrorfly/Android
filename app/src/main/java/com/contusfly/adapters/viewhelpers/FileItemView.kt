package com.contusfly.adapters.viewhelpers

import android.view.View
import com.contus.flycommons.MediaDownloadStatus
import com.contus.flycommons.MediaUploadStatus
import com.contusfly.adapters.holders.FileReceivedViewHolder
import com.contusfly.adapters.holders.FileSentViewHolder
import com.contusfly.chat.ImageFileUtils
import com.contusfly.gone
import com.contusfly.interfaces.MessageItemListener
import com.contusfly.show
import com.contusfly.utils.ChatMessageUtils
import com.contusfly.utils.Utils
import com.contusflysdk.api.models.ChatMessage


/**
 * File sent/received item view
 *
 * @author ContusTeam <developers></developers>@contus.in>
 * @version 2.0
 */
class FileItemView(private val messageItemListener: MessageItemListener) {
    /**
     * Chat common methods will be available here
     */

    /**
     * Sender image item view
     *
     * @param fileViewHolder File item view holder
     * @param time           Time of message sent
     * @param message           Message item
     */
    fun fileSenderItemView(fileViewHolder: FileSentViewHolder, time: String?, message: ChatMessage) {
        with(fileViewHolder) {
            fileSentTimeText.text = time
            val fileName = message.mediaChatMessage.mediaFileName
             fileNameText.text = fileName.trim()
            val fileSize = message.mediaChatMessage.mediaFileSize
            fileSizeText.text = ChatMessageUtils.getFileSizeText(fileSize.toString())
            messageItemListener.setStatus(message, fileStatusImage)
            ImageFileUtils.setFileImage(filePictureImage, fileName)
            fileFavoriteImage.visibility = if (message.isMessageStarred()) View.VISIBLE else View.GONE
            handleSenderFileItemProgressUpdate(message, this)
        }
    }

    /**
     * Sender file uploading progress based on file upload status
     *
     * @param message      Message item
     * @param fileViewHolder    File item view holder
     */
    fun handleSenderFileItemProgressUpdate(
        message: ChatMessage,
        fileViewHolder: FileSentViewHolder
    ) {
        val progressPercentage = Utils.returnZeroIfStringEmpty(Utils.returnEmptyStringIfNull(message.mediaChatMessage.mediaProgressStatus))
        val fileUploadStatus = Utils.returnEmptyStringIfNull(message.getMediaChatMessage().getMediaUploadStatus().toString())
        val fileDownloadStatus = Utils.returnEmptyStringIfNull(message.getMediaChatMessage().getMediaDownloadStatus().toString())
        val fileStatus = if (message.isItCarbonMessage()) fileDownloadStatus else fileUploadStatus

        with(fileViewHolder) {
            if ((fileStatus.toInt() == MediaUploadStatus.MEDIA_UPLOADING || fileStatus.toInt() == MediaDownloadStatus.MEDIA_DOWNLOADING)
                && progressPercentage > 0 && progressPercentage < 100) {
                fileUploadProgressBuffer.gone()
                fileUploadProgress.show()
                fileUploadProgress.max = 100
                fileUploadProgress.progress = progressPercentage
            } else if ((fileStatus.toInt() == MediaUploadStatus.MEDIA_UPLOADING || fileStatus.toInt() == MediaDownloadStatus.MEDIA_DOWNLOADING)
                && (progressPercentage < 1 || progressPercentage >= 100)) {
                fileUploadProgress.gone()
                fileUploadProgressBuffer.show()
            }
        }
    }

    /**
     * Receiver image item view
     *
     * @param fileViewHolder File item view holder
     * @param time           Time of message sent
     * @param message           Message item
     */
    fun fileReceiverItemView(fileViewHolder: FileReceivedViewHolder, time: String?, message: ChatMessage) {
        with(fileViewHolder) {
            fileReceivedTimeText.text = time
            val fileName = message.mediaChatMessage.mediaFileName
            fileNameText.text = fileName.trim()
            ImageFileUtils.setFileImage(filePictureImage, fileName)
            val fileSize = message.mediaChatMessage.mediaFileSize
            fileSizeText.text = ChatMessageUtils.getFileSizeText(fileSize.toString())
            handleReceiverFileItemProgressUpdate(message, this)
        }
    }

    /**
     * Receiver file downloading progress based on file upload status
     *
     * @param message      Message item
     * @param fileViewHolder    File item view holder
     */
    fun handleReceiverFileItemProgressUpdate(
        message: ChatMessage,
        fileViewHolder: FileReceivedViewHolder
    ) {
        val fileStatus = Utils.returnEmptyStringIfNull(message.mediaChatMessage.mediaDownloadStatus.toString())
        val progressPercentage = Utils.returnZeroIfStringEmpty(Utils.returnEmptyStringIfNull(message.mediaChatMessage.mediaProgressStatus))

        with(fileViewHolder){
        if (fileStatus.toInt() == MediaDownloadStatus.MEDIA_DOWNLOADING
            && progressPercentage > 0 && progressPercentage < 100) {
            fileDownloadProgressBuffer.gone()
            fileDownloadProgress.show()
            fileDownloadProgress.max = 100
            fileDownloadProgress.progress = progressPercentage
        } else if (fileStatus.toInt() == MediaDownloadStatus.MEDIA_DOWNLOADING
            && (progressPercentage < 1 || progressPercentage >= 100)) {
            fileDownloadProgress.gone()
            fileDownloadProgressBuffer.show()
        }
        }
    }

}