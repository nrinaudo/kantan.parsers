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

import kantan.parsers.*
import kantan.parsers.Parser.{char, digit, end, satisfy, string}

enum Token:
  case Specification
  case Enum
  case Struct
  case Signal
  case Indent
  case LineBreak
  case Str(value: String)
  case Colon
  case Identifier(value: String)
  case Whitespace

val letter = satisfy[Char] { c =>
  (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
}

val identifier = (letter ~ (letter | digit | char('_')).rep0).map { case (head, tail) =>
  s"$head${tail.mkString}"
}

val space = char(' ').as(Token.Whitespace).label("whitespace")

val specName =
  identifier.repSep(char('.')).map(segments => Token.Identifier(segments.mkString("."))).label("specification name")
val specification = string("specification").as(Token.Specification)
val `enum`        = string("enum").as(Token.Enum)
val struct        = string("struct").as(Token.Struct)
val signal        = string("signal").as(Token.Signal)
val lineBreak     = (char('\n')).as(Token.LineBreak).label("line break")
val colon         = char(':').as(Token.Colon).label("colon")
val ident         = identifier.map(Token.Str.apply).label("identifier")

val token = `enum` | specification | space | lineBreak | struct | signal | colon | specName.backtrack | ident

val parser = token.rep

val input = """specification com.foo.bar

struct Foo:
  non list of int foo"""

/*
val input = """specification com.nrinaudo.foo.bar

struct Foo:
  int num
"""*/

@main def main =
  val result = parser.run(input)
  result match
    case Result.Ok(_, parsed, _, _) => println(s"OK: ${parsed.value}")
    case Result.Error(_, msg) =>
      println(s"Error at ${msg.pos.line}:${msg.pos.column}")
      println(s"Expected one of ${msg.expected.mkString(", ")}")
      println(s"Found '${msg.input}'")
