package com.alley.digitalmemory.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        // Agar list null ya empty hai, to empty string return karo
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toList(data: String?): List<String> {
        // Agar data null ya empty hai, to Sachi muchi ki Empty List return karo
        if (data.isNullOrEmpty()) return emptyList()

        return data.split("|||").map { it.trim() }
    }
}