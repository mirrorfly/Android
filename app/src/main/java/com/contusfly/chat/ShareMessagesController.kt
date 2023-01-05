package com.contusfly.chat

import com.contusfly.models.MessageObject
import com.contusflysdk.api.models.ChatMessage
import com.contusfly.chat.FileMimeType.Companion.APPLICATION
import com.contusfly.chat.FileMimeType.Companion.AUDIO
import com.contusfly.chat.FileMimeType.Companion.IMAGE
import com.contusfly.chat.FileMimeType.Companion.VIDEO
import com.contusfly.models.ContactShareModel
import com.contusfly.models.FileObject
import java.util.*
import javax.inject.Inject


/**
 * This Class handles the Message Sending part of the Quick Share Module
 *
 * @author ContusTeam <developers></developers>@contus.in>
 * @version 1.0
 */
class ShareMessagesController @Inject
constructor(private val messagingClient: MessagingClient){

    /**
     * Compose and send text messages to the given list of rosters
     *
     * @param shareText the text to share or send
     * @param userIdList user list to send the text message
     */
    fun sendTextMessage(shareText: String, userIdList: List<String>) {
        val messageObjectList = ArrayList<MessageObject>()
        for (userId in userIdList) {
            messageObjectList.add(messagingClient.composeTextMessage(userId, shareText))
        }
        sendMessage(messageObjectList)
    }

    /**
     * Compose and send Contact messages to the given list of rosters
     *
     * @param contacts list of ContactShareModel to share to the users
     * @param users    user id list to send the text message
     */
    fun sendContactMessage(contacts: List<ContactShareModel>, users: List<String>) {
        val messageObjectList = ArrayList<MessageObject>()
        for (userId in users) {
            for (contactMessage in contacts) {
                messageObjectList.add(messagingClient.composeContactMessage(userId, contactMessage))
            }
        }
        sendMessage(messageObjectList)
    }

    /**
     * Sends Message to the remaining users of a quick share
     *
     * @param message  the message that sends to each users in the list
     * @param usersJID list of JID to which the message is going to send
     */
    fun sendMediaMessagesToRemainingUsers(message: ChatMessage, usersJID: List<String?>) {
        /* No implementation needed*/
    }



    /**
     * Send Media Message to the first user in quick to whom only the uploads takes place
     *
     * @param fileObjects list of files the needs to be uploaded
     * @param userIdList  list of JID to which the message is going to send.
     */
    fun sendMediaMessagesForSingleUser(fileObjects: List<FileObject>, userIdList: List<String>) {
        val messageObjectList = ArrayList<MessageObject>()
        for (userId in userIdList) {
                for (fileObject in fileObjects) {
                    when (fileObject.fileMimeType) {
                        IMAGE -> messageObjectList.add(messagingClient.composeImageMessage(userId, fileObject.filePath, fileObject.caption))
                        VIDEO -> {
                            val videoMessage = messagingClient.composeVideoMessage(userId, fileObject.filePath, fileObject.caption)
                            addVideoMessage(videoMessage, messageObjectList)
                        }
                        AUDIO -> {
                            val audioMessage = messagingClient.composeAudioMessage(userId, false, fileObject.filePath)
                            addAudioMessage(audioMessage, messageObjectList)
                        }
                        APPLICATION -> {
                            val documentMessage = messagingClient.composeDocumentsMessage(userId, fileObject.filePath)
                            addDocumentMessage(documentMessage, messageObjectList)
                        }
                    }
                }
        }
        sendMessage(messageObjectList)
    }

    private fun addVideoMessage(videoMessage: Pair<Boolean, MessageObject?>, messageObjectList: ArrayList<MessageObject>){
        if (videoMessage.first)
            messageObjectList.add(videoMessage.second!!)
    }

    private fun addAudioMessage( audioMessage: Triple<Boolean, Boolean, MessageObject?>, messageObjectList: ArrayList<MessageObject>){
        if (audioMessage.first && audioMessage.second)
            messageObjectList.add(audioMessage.third!!)
    }

    private fun addDocumentMessage(documentMessage: Triple<Boolean, Boolean, MessageObject?>, messageObjectList: ArrayList<MessageObject>){
        if (documentMessage.first && documentMessage.second)
            messageObjectList.add(documentMessage.third!!)
    }

    /**
     * Send the message to the SDK
     *
     * @param messageObjectList list of messages to send
     */
    private fun sendMessage(messageObjectList: ArrayList<MessageObject>) {
        for (messageObject in messageObjectList) {
            messagingClient.sendMessage(messageObject, null)
        }
    }
}