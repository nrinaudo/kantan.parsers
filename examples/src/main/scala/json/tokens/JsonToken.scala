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

package kantan.parsers.json.tokens

/** All possible tokens in a JSON file, as well as a parser for them.
  *
  * This is meant to be used as the input to tokens/jExp.
  *
  * The only subtlety here is JsonToken.Unknown, which represents any token that cannot be mapped to a known one. This
  * is useful in that it allows us to retrieve as many valid tokens as possible rather than fail at the first illegal
  * one. This is typically used when providing syntax highlighting data to editors (through LSP, say), where we still
  * want to have *something* for invalid data.
  *
  * Note how none of the parsers declared here have a label. This is intentional: labels are used to generate better
  * error messages, and this should, in theory, never fail.
  */

import kantan.parsers.{Parsed, Parser as P, Position, Result, SourceMap}
import P.*

// Note how we're not parsing to A, but Parsed[A]: every token is going to be tagged with its position information.
type Parser[A] = P[Char, Parsed[A]]

enum JsonToken:
  // - Tokens ----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  case Num(value: Double)
  case Str(value: String)
  case Bool(value: Boolean)
  case Unknown(value: String)
  case Null
  case ArrayStart
  case ArrayEnd
  case ObjStart
  case ObjEnd
  case FieldSep
  case ListSep

  override def toString: String = this match
    case Num(value)     => value.toString
    case Str(value)     => s"\"value\""
    case Bool(value)    => value.toString
    case Null           => "null"
    case ArrayStart     => "["
    case ArrayEnd       => "]"
    case ObjStart       => "{"
    case ObjEnd         => "}"
    case ListSep        => ","
    case FieldSep       => ":"
    case Unknown(value) => value

object JsonToken:
  // - Mapping tokens to source position -------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  given SourceMap[Parsed[JsonToken]] with
    extension (token: Parsed[JsonToken])
      def endsAt(current: Position)   = token.end
      def startsAt(current: Position) = token.start

  // - Token parsers ---------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val num: Parser[JsonToken] =
    val number = digit.rep.map(_.mkString)
    val sign   = (string("-") | string("+")).?.map(_.getOrElse(""))
    val exponent =
      (char(c => c == 'e' || c == 'E') *> (sign ~ number).map((s, n) => s"e$s$n")).?.map(_.getOrElse(""))

    val fractional = (char('.') *> number.map(n => s".$n")).?.map(_.getOrElse(""))
    val integral   = number
    val value = for
      s <- sign
      i <- integral
      f <- fractional
      e <- exponent
    yield s"$s$i$f$e".toDouble

    value.map(JsonToken.Num.apply).withPosition

  val str: Parser[JsonToken] =
    val isHexChar    = (c: Char) => c.isDigit || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
    val isStringChar = (c: Char) => c != '"' && c != '\\'

    val stringChar   = char(c => c != '"' && c != '\\').map(_.toString)
    val escapedChars = Set('b', 'n', 'f', 'r', 't', '/', '\\')
    val escapedChar  = char(escapedChars.contains).map(_.toString)
    val hexChar      = char(isHexChar)
    val hexChars = for
      c1 <- hexChar
      c2 <- hexChar
      c3 <- hexChar
      c4 <- hexChar
    yield s"$c1$c2$c3$c4"

    val escape = char('\\') *> (escapedChar | hexChars).map(s => s"\\$s")

    val parser = char('"') *> (stringChar | escape).rep0.map(_.mkString) <* char('"')
    parser.map(JsonToken.Str.apply).withPosition

  val bool: Parser[JsonToken] =
    val parser = string("true").as(true) | string("false").as(false)

    parser.map(JsonToken.Bool.apply).withPosition

  val nullToken: Parser[JsonToken] = string("null").as(JsonToken.Null).withPosition

  val unknown: Parser[JsonToken] =
    char(_ => true).filterNot(c => c.isWhitespace).rep.map(cs => JsonToken.Unknown(cs.mkString)).withPosition

  val listSep: Parser[JsonToken]    = char(',').as(JsonToken.ListSep).withPosition
  val fieldSep: Parser[JsonToken]   = char(':').as(JsonToken.FieldSep).withPosition
  val arrayStart: Parser[JsonToken] = char('[').as(JsonToken.ArrayStart).withPosition
  val arrayEnd: Parser[JsonToken]   = char(']').as(JsonToken.ArrayEnd).withPosition
  val objStart: Parser[JsonToken]   = char('{').as(JsonToken.ObjStart).withPosition
  val objEnd: Parser[JsonToken]     = char('}').as(JsonToken.ObjEnd).withPosition

  // - Global parser ---------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  val parser: P[Char, Seq[Parsed[JsonToken]]] =
    val token = num | str | bool | nullToken | arrayStart | arrayEnd | objStart | objEnd | listSep | fieldSep | unknown

    token.surroundedBy(whitespace.rep0).rep <* end
