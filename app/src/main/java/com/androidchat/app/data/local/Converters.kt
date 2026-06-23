package com.androidchat.app.data.local

import androidx.room.TypeConverter
import com.androidchat.app.data.local.entities.MessageStatus
import com.androidchat.app.data.local.entities.MessageType

class Converters {
    @TypeConverter fun fromMessageType(value: MessageType) = value.name
    @TypeConverter fun toMessageType(value: String) = MessageType.valueOf(value)
    @TypeConverter fun fromMessageStatus(value: MessageStatus) = value.name
    @TypeConverter fun toMessageStatus(value: String) = MessageStatus.valueOf(value)
}
