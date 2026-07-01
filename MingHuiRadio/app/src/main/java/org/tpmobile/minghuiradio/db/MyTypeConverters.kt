package org.tpmobile.minghuiradio.db

import androidx.room.TypeConverter

class MyTypeConverters {

  /////////////////////////
  @TypeConverter
  fun stringToStringList(data: String?): List<String>? {
    return data?.split(",")?.filter {
      it.isNotBlank() && it != "null"
    }
  }

  @TypeConverter
  fun stringListToString(strings: List<String>?): String? {
    return strings?.joinToString(",")
  }

  /////////////////////////
  @TypeConverter
  fun stringToIntList(data: String?): List<Int>? {
    return data?.split(",")?.mapNotNull { it.toIntOrNull() }
  }

  @TypeConverter
  fun intListToString(ints: List<Int>?): String? {
    return ints?.joinToString(",")
  }

}