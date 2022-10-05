/*
 * Copyright 2022 Nicolas Rinaudo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kantan.parsers.json

import org.typelevel.jawn.ast._
import kantan.parsers.{Parser => P}
import P._

/** Parser for JSON data.
  *
  * This parser does everything in one pass. By contrast, tokens/json first tokenises the input, then tries to assemble
  * the tokens as JSON data.
  */
package object raw {
  type Parser[A] = P[Char, A]

  /** JSON numbers */
  val jNum: Parser[JNum] = {
    val number = digit.rep.map(_.mkString)
    val sign   = (string("-") | string("+")).?.map(_.getOrElse(""))
    val exponent =
      (char(c => c == 'e' || c == 'E') *> (sign ~ number).map { case (s, n) => s"e$s$n" }).?.map(_.getOrElse(""))

    val fractional = (char('.') *> number.map(n => s".$n")).?.map(_.getOrElse(""))
    val integral   = number

    val parser = for {
      s <- sign
      i <- integral
      f <- fractional
      e <- exponent
    } yield JNum(s"$s$i$f$e")

    parser.label("number")
  }

  /** String, used both for JSON string and object field names. */
  val rawString: Parser[String] = {
    val escapedChars =
      Map('b' -> "\b", 'n' -> "\n", 'f' -> "\f", 'r' -> "\r", 't' -> "\t", '/' -> "/", '\\' -> "\\", '"' -> "\"")

    val isHexChar    = (c: Char) => c.isDigit || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
    val isStringChar = (c: Char) => c != '"' && c != '\\'

    val stringChar = char(c => c != '"' && c != '\\').map(_.toString)

    val escapedChar = char(escapedChars.contains _).collect(escapedChars)
    val hexChar     = char(isHexChar)
    val hexChars = for {
      _  <- char('u')
      c1 <- hexChar
      c2 <- hexChar
      c3 <- hexChar
      c4 <- hexChar
    } yield Character.toChars(Integer.parseInt(s"$c1$c2$c3$c4", 16)).mkString

    val escape = char('\\') *> (escapedChar | hexChars)

    char('"') *> (stringChar | escape).rep0.map(_.mkString) <* char('"')
  }

  /** JSON string. */
  val jString: Parser[JString] = rawString.map(JString.apply).label("string")

  /** JSON boolean. */
  val jBool: Parser[JBool] =
    (string("true").as(JBool.True) | string("false").as(JBool.False)).label("boolean")

  /** JSON null. */
  val jNull: Parser[JNull.type] = string("null").as(JNull).label("null")

  /** Any JSON value. */
  def jValue: Parser[JValue] = (jNum | jString | jBool | jNull | jArray | jObject).surroundedBy(whitespace.rep0)

  /** List separator, used to separate object fields and elements in an array. */
  val listSep = char(',').surroundedBy(whitespace.rep0)

  /** JSON object. */
  def jObject: Parser[JObject] = {
    val pair: Parser[(String, JValue)] = (rawString <* char(':')) ~ jValue

    pair
      .repSep0(listSep)
      .between(char('{').surroundedBy(whitespace.rep0), char('}').surroundedBy(whitespace.rep0))
      .map(JObject.fromSeq)
      .label("object")
  }

  /** JSON array. */
  def jArray: Parser[JArray] =
    jValue.repSep0(listSep).between(char('['), char(']')).map(JArray.fromSeq).label("array")

  /** JSON expression. */
  val jExp: Parser[JObject] = jObject <* end
}
