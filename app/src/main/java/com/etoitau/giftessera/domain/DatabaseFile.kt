package com.etoitau.giftessera.domain

/**
 * An object for holding information relevant to a database save file
 * @param id - database primary key
 * @param name - file name
 * @param blob - ByteArray representing list of Bitmaps per FilmstripToByte helper classes
 */
class DatabaseFile(val id: Int?, val name: String, val blob: ByteArray)
