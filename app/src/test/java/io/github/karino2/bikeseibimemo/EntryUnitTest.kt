package io.github.karino2.bikeseibimemo

import org.junit.Test

import org.junit.Assert.*
import java.util.Date

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class EntryUnitTest {
    @Test
    fun fromLine_GAS() {
        val entry = Entry.fromLine(123, "2023,5,9,1,9.37,1430,79438,")
        assertEquals("Tue May 09 00:00:00 JST 2023", entry.date.toString())
    }

    @Test
    fun toLine_GAS() {
        val entry = Entry(123, Category.GAS, Date("2023/5/9"), 9.37, 1430, 99438, "メモです")
        assertEquals("2023,5,9,1,9.37,1430,99438,メモです", entry.toLine())
    }

    private val csvContent = """
            year,month,date,category,liter,price,runningDistance,memo
            2021,11,11,1,6.97,1115,76571,
            2021,11,25,1,6.8,1068,76723,
            2022,2,5,1,8.84,1423,76901,
            2022,4,26,1,9.31,1518,77076,
            2022,4,26,2,,13189,77123,ブレーキパッド、ピストン、ブレーキフルイドも
            2022,5,27,1,9.16,1429,77274,
        """.trimIndent()

    @Test
    fun testEntryList() {
        val elist = EntryList(csvContent)
        assertEquals(6, elist.raw.size)
        assertEquals(77274, elist.raw.last().runningDistance)
        assertEquals(1, elist.raw[0].lineNum)
        assertEquals(77274, elist.lastRunningDistance)
    }

    @Test
    fun testEntryListAdd() {
        val elist = EntryList(csvContent)
        val elist2 = elist.add(Entry(null, Category.GAS, Date(), 12.3, 1234, 99999, ""))
        assertEquals(elist2.raw.size, elist.raw.size+1)
        assertEquals(elist2.raw.last().lineNum, elist.raw.last().lineNum!!+1)
    }

    @Test
    fun testLast() {
        val elist = EntryList(csvContent)
        val lgas = elist.lastCategoryBy(Category.GAS)

        assertEquals(77274, lgas?.runningDistance)

        val loil = elist.lastCategoryBy(Category.OIL)
        assertEquals(77123, loil?.runningDistance)

        val ltire = elist.lastCategoryBy(Category.TIRE)
        assertNull(ltire)

    }

    @Test
    fun testSecondLastGAS() {
        val elist = EntryList(csvContent)
        val slgas = elist.secondLastGas()
        assertEquals(77076, slgas?.runningDistance)
    }


    private val twoGASCsv = """
            year,month,date,category,liter,price,runningDistance,memo
            2022,4,26,1,9.31,1518,77076,
            2022,4,26,2,,13189,77123,ブレーキパッド、ピストン、ブレーキフルイドも
            2022,5,27,1,9.16,1429,77274,
        """.trimIndent()

    private val oneGASCsv = """
            year,month,date,category,liter,price,runningDistance,memo
            2022,4,26,2,,13189,77123,ブレーキパッド、ピストン、ブレーキフルイドも
            2022,5,27,1,9.16,1429,77274,
        """.trimIndent()


    @Test
    fun testSecondLastGASNull() {
        val elist = EntryList(oneGASCsv)
        val slgas = elist.secondLastGas()
        assertNull(slgas)
    }
}