package io.github.karino2.bikeseibimemo

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.Serializable
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

enum class Category(val rawValue: Int, val displayName: String)
{
    GAS(1, "給油"),
    OIL(2, "オイル"),
    TIRE(3, "タイヤ"),
    FILTER(4, "フィルタ"),
    INSURANCE(7, "保険"), // 車両整備記録アプリと揃えるため7にしておく。
    OTHER(99, "その他");

    companion object {
        fun from(v: Int) = when(v) {
            1 -> Category.GAS
            2-> Category.OIL
            3-> Category.TIRE
            4-> Category.FILTER
            7-> Category.INSURANCE
            99 -> Category.OTHER
            else -> throw IllegalArgumentException("Unknown category value: ${v}.")
        }

    }
}


/*
    lineNumは先頭はヘッダで0なのでEntryは1から始まる。
    dateは日付まで。他は0。
    literは小数点以下二桁固定、カテゴリがGASじゃない時はnull
    runningDistanceは無ければ-1
 */
data class Entry(val lineNum: Int?, val category: Category, val date: Date, val liter: Double?, val price: Int?, val runningDistance: Int?, val memo: String) : Serializable {
    companion object {
        // emptyは-1とするtoInt
        private fun String.toIntWithEmpty() : Int? {
            if (this == "")
                return null
            return this.toInt()
        }

        // emptyは使わないので0.0を返す
        private fun String.toDoubleWithEmpty() : Double? {
            return if(this == "") null else this.toDouble()
        }

        // "2023,5,9,1,9.37,1430,79438,memo"
        fun fromLine(lnum: Int, line: String) : Entry {
            val cells = line.split(",")
            if(cells.size != 8) throw IllegalArgumentException("Unknown csv format. cell num is ${cells.size}.")
            return Entry(lnum, Category.from(cells[3].toInt()), Date(cells[0].toInt()-1900, cells[1].toInt()-1, cells[2].toInt()), cells[4].toDoubleWithEmpty(), cells[5].toInt(), cells[6].toIntWithEmpty(), cells[7].replace("\\n", "\n"))
        }
    }

    // "2023,5,9,1,9.37,1430,79438,memo"
    fun toLine() : String {
        val sb = StringBuilder()
        sb.append(date.year+1900)
        sb.append(',')

        sb.append(date.month+1)
        sb.append(',')

        sb.append(date.date)
        sb.append(',')

        sb.append(category.rawValue)
        sb.append(',')

        liter?.let { sb.append("%.2f".format(it)) }
        sb.append(',')

        price?.let { sb.append(it)}
        sb.append(',')

        runningDistance?.let { sb.append(it) }
        sb.append(',')

        sb.append(memo.replace("\n", "\\n"))

        return sb.toString()
    }

    val displayDate: String
        get() = DateFormat.getDateInstance().format(date)

    val displayDateOnly: String
        get() =  android.text.format.DateFormat.format("M/d", date).toString()

    val displayYearDate: String
        get() =  android.text.format.DateFormat.format("yyyy/M/d", date).toString()

    val displayLiter: String
        get() {
            return liter?.let { "%.2f".format(it) } ?: ""
        }

    val Int?.display : String
        get() = this?.toString() ?: ""

    val displayPrice : String
        get() = price.display

    val displayRunningDistance : String
        get() = runningDistance.display

}

class EntryList(val raw: List<Entry>) {
    constructor(text: String) : this( text.split('\n').drop(1).dropLastWhile {it.isEmpty()}. mapIndexed() {i, l-> Entry.fromLine(i+1, l)})

    companion object {
        const val LAST_URI_KEY = "last_uri_path"

        fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("ITSU_NANI", Context.MODE_PRIVATE)!!
        fun lastUri(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)?.let {
            Uri.parse(it)
        }

        fun writeLastUri(ctx:Context, uri : Uri) = sharedPreferences(ctx)
            .edit()
            .putString(LAST_URI_KEY, uri.toString())
            .commit()

        fun load(context: Context): EntryList {
            val text = context.contentResolver.openFileDescriptor(lastUri(context)!!, "r")!!.use { desc ->
                val fis = FileInputStream(desc.fileDescriptor)
                fis.bufferedReader().use { it.readText() }
            }
            return EntryList(text)
        }

        const val headerLine = "year,month,date,category,liter,price,runningDistance,memo\n"

        fun createEmptyCsv(context: Context, uri: Uri) {
            context.contentResolver.openOutputStream(uri, "w")!!.use {
                BufferedWriter(OutputStreamWriter(it)).use { bw ->
                    bw.write(headerLine)
                }
            }

        }

        fun save(context: Context, elist: EntryList) {
            context.contentResolver.openOutputStream(lastUri(context)!!, "wt").use {
                BufferedWriter(OutputStreamWriter(it)).use { bw ->
                    bw.write(headerLine)
                    bw.write(elist.toLines())
                }
            }
        }

        fun calcMileage(lastDist: Int, lastLiter: Double, secondLastDist: Int) : String {
            val diffDist = lastDist - secondLastDist
            val mileage = (diffDist.toDouble())/lastLiter
            return "%.2fkm/l".format(mileage)
        }

    }

    fun toLines() : String {
        return raw.map { it.toLine() }.joinToString("\n")
    }

    fun add(entry: Entry) : EntryList {
        assert(entry.lineNum == null)

        return EntryList( raw+ entry.copy(lineNum = raw.size+1))
    }

    fun update(entry: Entry) : EntryList {
        return EntryList( raw.map { if(it.lineNum != entry.lineNum) it else entry } )
    }

    val lastRunningDistance by lazy {
        fromLatest().dropWhile { it.runningDistance == null }.firstOrNull()?.runningDistance
    }

    fun lastCategoryBy(category: Category) = fromLatest().dropWhile{ it.category != category }.firstOrNull()

    fun secondLastGas() = gasFromLatest().drop(1).firstOrNull()

    fun fromLatest() = raw.reversed()

    // list of (cur, prev) of GAS entry (to calculate mileage).
    fun gasPair() : List<Pair<Entry, Entry>> {
        val gasList = gasFromLatest()
        val prevList = gasList.drop(1)
        val curList = gasList.dropLast(1)
        return curList.zip(prevList)
    }

    private fun gasFromLatest() = fromLatest().filter { it.category == Category.GAS }

    val lastMileage : Pair<String, String>
        get() {
            val nyd = Pair("", "未定")
            val lastGas = lastCategoryBy(Category.GAS) ?: return nyd
            val slastGas = secondLastGas() ?: return nyd
            val lastDist = lastGas.runningDistance ?: return nyd
            val slastDist = slastGas.runningDistance ?: return nyd
            val lastLiter = lastGas.liter ?: return nyd
            val date = lastGas.displayDate

            val mils = calcMileage(lastDist, lastLiter, slastDist)

            return Pair(date, mils)
        }

    fun elapsedCategory(category: Category) : Pair<String, String> {
        val nyd = Pair("", "未定")
        val last = lastRunningDistance ?: return nyd
        val lastCat = lastCategoryBy(category) ?: return nyd
        val lastCatDis = lastCat.runningDistance ?: return nyd

        val date = lastCat.displayDate
        val diff = last - lastCatDis
        return Pair(date, "${diff}km")
    }
}