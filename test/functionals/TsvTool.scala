package functionals

import java.io.{BufferedReader, StringReader}

import org.openqa.selenium.WebElement

import scala.annotation.tailrec
import com.codeborne.selenide.Selenide._

object TsvTool {
  def setTsvTo(tsv: String, webElement: WebElement): Unit = {
    val br = new BufferedReader(new StringReader(tsv))

    @tailrec def readLines(buf: StringBuilder = new StringBuilder): String = {
      if (buf.length != 0) buf.append("\\n")

      val l = br.readLine()
      if (l == null) buf.toString
      else readLines(buf.append(l.replace("\t", "\\t")))
    }

    val s = "\"" + readLines() + "\""
    val js = "arguments[0].value=" + s
    println("js = " + js)
    executeJavaScript(js, webElement)
  }
}
