package com.etoitau.giftessera.domain

import kotlinx.serialization.Serializable

/**
 * An object for holding information relevant to a database save file
 * @param id - database primary key
 * @param name - file name
 * @param blob - ByteArray representing list of Bitmaps per FilmstripToByte helper classes
 */

@Serializable
class DatabaseFile(val id: Int?, val name: String, val blob: ByteArray)
