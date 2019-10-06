package helpers

import java.nio.file.{Files, Paths}

import org.specs2.mutable.Specification

import scala.collection.{immutable => imm}

class TsvSpec extends Specification {
  "Tsv" should {
    "Can read tsv01" in {
      val tsvStr: String = new String(Files.readAllBytes(Paths.get("testdata/tsv/case01.tsv")), "utf-8")
      val tsv = Tsv.parse(tsvStr)
      tsv("Agent Name") === "shanai"
      tsv.agentName === "shanai"

      tsv("Date (yyyy-mm-dd)") === "2019-09-22"
      tsv.date === "2019-09-22"

      tsv("Time (hh:mm:ss)") === "08:54:16"
      tsv.time === "08:54:16"

      tsv("Lifetime AP") === "311089437"
      tsv.lifetimeAp === 311089437L

      tsv("Current AP") === "29426909"
      tsv.currentAp === 29426909L

      tsv("Distance Walked") === "8479"
      tsv.distanceWalked === 8479

      tsv.agentLevel === 15
    }

    "Can read tsv02" in {
      val tsvStr: String = new String(Files.readAllBytes(Paths.get("testdata/tsv/case02.tsv")), "utf-8")
      val tsv = Tsv.parse(tsvStr)
      tsv("Agent Name") === "shanai"
      tsv.agentName === "shanai"

      tsv("Date (yyyy-mm-dd)") === "2019-10-06"
      tsv.date === "2019-10-06"

      tsv("Time (hh:mm:ss)") === "14:34:18"
      tsv.time === "14:34:18"

      tsv("Lifetime AP") === "312897982"
      tsv.lifetimeAp === 312897982L

      tsv("Current AP") === "31235454"
      tsv.currentAp === 31235454L

      tsv("Distance Walked") === "8506"
      tsv.distanceWalked === 8506

      tsv("Level") === "14"
      tsv.agentLevel === 14
    }

    "Ap to level" in {
      Tsv.apToLevel(0) === 1
      Tsv.apToLevel(100) === 1

      Tsv.apToLevel(2499) === 1
      Tsv.apToLevel(2500) === 2
      Tsv.apToLevel(2501) === 2

      Tsv.apToLevel(19999) === 2
      Tsv.apToLevel(20000) === 3
      Tsv.apToLevel(20001) === 3

      Tsv.apToLevel(69999) === 3
      Tsv.apToLevel(70000) === 4
      Tsv.apToLevel(70001) === 4

      Tsv.apToLevel(149999) === 4
      Tsv.apToLevel(150000) === 5
      Tsv.apToLevel(150001) === 5

      Tsv.apToLevel(299999) === 5
      Tsv.apToLevel(300000) === 6
      Tsv.apToLevel(300001) === 6

      Tsv.apToLevel(599999) === 6
      Tsv.apToLevel(600000) === 7
      Tsv.apToLevel(600001) === 7

      Tsv.apToLevel(1199999) === 7
      Tsv.apToLevel(1200000) === 8
      Tsv.apToLevel(1200001) === 8

      Tsv.apToLevel(2399999) === 8
      Tsv.apToLevel(2400000) === 9
      Tsv.apToLevel(2400001) === 9

      Tsv.apToLevel(3999999) === 9
      Tsv.apToLevel(4000000) === 10
      Tsv.apToLevel(4000001) === 10

      Tsv.apToLevel(5999999) === 10
      Tsv.apToLevel(6000000) === 11
      Tsv.apToLevel(6000001) === 11

      Tsv.apToLevel(8399999) === 11
      Tsv.apToLevel(8400000) === 12
      Tsv.apToLevel(8400001) === 12

      Tsv.apToLevel(11999999) === 12
      Tsv.apToLevel(12000000) === 13
      Tsv.apToLevel(12000001) === 13

      Tsv.apToLevel(16999999) === 13
      Tsv.apToLevel(17000000) === 14
      Tsv.apToLevel(17000001) === 14

      Tsv.apToLevel(23999999) === 14
      Tsv.apToLevel(24000000) === 15
      Tsv.apToLevel(24000001) === 15

      Tsv.apToLevel(39999999) === 15
      Tsv.apToLevel(40000000) === 16
      Tsv.apToLevel(40000001) === 16
    }
  }
}

