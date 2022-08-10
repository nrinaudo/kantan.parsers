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

/** Example of parsing JSON in two phases:
  *   - tokenization (with JsonToken)
  *   - parsing (this file).
  */

import org.typelevel.jawn.ast.*
import kantan.parsers.{Parsed, Parser as P, Position, Result, SourceMap}
import P.*

// - Helpers -----------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------

type TokenParser[A] = P[Parsed[JsonToken], A]

/** Helper for parsing from Seq[Parsed[JsonToken]] into a JsonToken.
  *
  * Note that by doing so, we are discarding position information. This is desirable here, where our end goal is to
  * produce a JValue, but in some other scenarios, you might want to keep it (if the parsing phase is followed by a type
  * checking one, for example, and you want to provide useful type error messages).
  */
val jsonToken: TokenParser[JsonToken] = token[Parsed[JsonToken]].map(_.value)

// Parsers for the various "syntax" tokens.
val fieldSep: TokenParser[Unit] = jsonToken.collect { case JsonToken.FieldSep => () }.label(JsonToken.FieldSep.toString)
val listSep: TokenParser[Unit]  = jsonToken.collect { case JsonToken.ListSep => () }.label(JsonToken.ListSep.toString)
val objStart: TokenParser[Unit] = jsonToken.collect { case JsonToken.ObjStart => () }.label(JsonToken.ObjStart.toString)
val objEnd: TokenParser[Unit]   = jsonToken.collect { case JsonToken.ObjEnd => () }.label(JsonToken.ObjEnd.toString)
val arrayStart: TokenParser[Unit] =
  jsonToken.collect { case JsonToken.ArrayStart => () }.label(JsonToken.ArrayStart.toString)
val arrayEnd: TokenParser[Unit] =
  jsonToken.collect { case JsonToken.ArrayEnd => () }.label(JsonToken.ArrayStart.toString)

// - Parsers for JSON values -------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
val jNull: TokenParser[JNull.type] = jsonToken.collect { case JsonToken.Null => JNull }.label("null")

val jNum: TokenParser[JNum] = jsonToken.collect { case JsonToken.Num(value) => JNum(value) }.label("number")

val jString: TokenParser[JString] = jsonToken.collect { case JsonToken.Str(value) => JString(value) }.label("string")

val jBool: TokenParser[JBool] = jsonToken.collect {
  case JsonToken.Bool(true)  => JTrue
  case JsonToken.Bool(false) => JFalse
}.label("boolean")

def jObject: TokenParser[JObject] =
  val str  = jsonToken.collect { case JsonToken.Str(value) => value }
  val pair = ((str <* fieldSep) ~ jValue).label("field")

  pair
    .repSep(listSep)
    .between(objStart, objEnd)
    .map(JObject.fromSeq)
    .label("object")

def jArray: TokenParser[JArray] =
  jValue.repSep(listSep).between(arrayStart, arrayEnd).map(JArray.fromSeq).label("array")

def jValue: TokenParser[JValue] = jNum | jString | jBool | jNull | jArray | jObject

val jExp: TokenParser[JValue] = jValue <* end

@main def main =
  val input = """
{ 
  "foo": [1, 2, 3],
  "bar": null,
  "baz": {
    "left": true,
    "right": false
  }
}
"""

  val result = for
    tokens <- JsonToken.parser.run(input).toEither
    json   <- jExp.run(tokens).toEither
  yield json

  result match
    case Left(msg) =>
      println(
        s"Found ${msg.input} at line ${msg.pos.line} column ${msg.pos.column}, expected one of ${msg.expected.mkString(", ")}"
      )
    case Right(result) =>
      println(result)
