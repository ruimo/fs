package helpers

import java.io.{BufferedReader, StringReader}
import java.util.Arrays

import scala.collection.{immutable => imm}
import com.ruimo.scoins.LoanPattern._
import com.ruimo.csv.{Parser => CsvParser}

import scala.annotation.tailrec

trait Tsv {
  def apply(key: String): String
  val raw: imm.Map[String, String]
  val agentName: String
  val date: String
  val time: String
  val lifetimeAp: Long
  val currentAp: Long
  val agentLevel: Int
  val distanceWalked: Int
}

object Tsv {
  private val ApForLevel = Array(
    2500L, 20000, 70000, 150000, 300000, 600000, 1200000, 2400000,
    4000000, 6000000, 8400000, 12000000, 17000000, 24000000, 40000000
  )

  private case class TsvImpl(raw: imm.Map[String, String]) extends Tsv {
    override def apply(key: String): String = raw(key)
    override val agentName: String = raw("Agent Name")
    override val date: String = raw("Date (yyyy-mm-dd)")
    override val time: String = raw("Time (hh:mm:ss)")
    override val lifetimeAp: Long = raw("Lifetime AP").toLong
    override val currentAp: Long = raw("Current AP").toLong
    override val agentLevel: Int = apToLevel(currentAp)
    override val distanceWalked: Int = raw("Distance Walked").toInt
  }

  def apToLevel(ap: Long): Int = {
    val idx = Arrays.binarySearch(ApForLevel, ap)
    if (idx < 0) -idx else idx + 2
  }

  def parse(s: String): Tsv = {
    val (line0, line1) = using(new BufferedReader(new StringReader(s))) { br =>
      (br.readLine(), br.readLine())
    }.get

    @tailrec def loop(h: Seq[String], b: Seq[String], result: imm.Map[String, String] = imm.Map()): imm.Map[String, String] =
      if (h.isEmpty) result
      else {
        if (b.isEmpty)
          loop(h.tail, Seq(""), result.updated(h.head, ""))
        else
          loop(h.tail, b.tail, result.updated(h.head, b.head))
      }

    TsvImpl(
      loop(
        CsvParser.parseOneLine(line0, '\t').get,
        CsvParser.parseOneLine(line1, '\t').get
      )
    )
  }
}
