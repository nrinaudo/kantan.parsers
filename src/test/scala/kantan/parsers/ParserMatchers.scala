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

package kantan.parsers

import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.matchers.should.Matchers

trait ParserMatchers {
  private def formatMessage(msg: Message[_]) =
    s"Expected one of ${msg.expected.mkString("'", "', '", "'")} but found ${msg.input}"

  def succeedWith[A](expected: A): Matcher[Result[_, A]] = new Matcher[Result[_, A]] {
    override def apply(result: Result[_, A]) = result match {
      case Result.Ok(_, parsed, _, _) =>
        MatchResult(
          parsed.value == expected,
          s"parse result '${parsed.value}' did not match '$expected'",
          s"parse result '${parsed.value}' was found"
        )
      case Result.Error(_, msg) => MatchResult(false, formatMessage(msg), "parsing succeeded")
    }
  }

  private def matchFailure[Token](result: Result[Token, _])(f: Result.Error[Token] => MatchResult) = result match {
    case Result.Ok(_, _, _, _)      => MatchResult(false, "parsing did not fail", "parsing failed")
    case error: Result.Error[Token] => f(error)
  }

  def failWith(labels: String*): Matcher[Result[_, _]] = new Matcher[Result[_, _]] {

    def productions(labels: Iterable[String]) = labels.mkString(", ")

    override def apply(result: Result[_, _]) = matchFailure(result) { case Result.Error(_, Message(_, _, _, items)) =>
      MatchResult(
        items.toSet == labels.toSet,
        s"expected productions '${productions(labels)}' did not match '${productions(items)}'",
        s"expected productions '${productions(labels)}' was found"
      )
    }
  }

  def failAt(line: Int, column: Int): Matcher[Result[_, _]] = new Matcher[Result[_, _]] {
    override def apply(result: Result[_, _]) = matchFailure(result) { case Result.Error(_, Message(_, pos, _, _)) =>
      MatchResult(
        pos.line == line && pos.column == column,
        s"expected a failure at $line:$column but found it at ${pos.line}:${pos.column}",
        s"expected a failure at $line:$column and found it"
      )
    }
  }

  def failOnInput[Token](input: Message.Input[Token]): Matcher[Result[Token, _]] = new Matcher[Result[Token, _]] {
    def show(token: Message.Input[Token]) = token match {
      case Message.Input.Eof          => "EOF"
      case Message.Input.Token(value) => value.toString
      case Message.Input.None         => "N/A"
    }

    override def apply(result: Result[Token, _]) = matchFailure(result) {
      case Result.Error(_, Message(_, _, observed, _)) =>
        MatchResult(
          input == observed,
          s"expected a failure on token ${show(input)} but found it on ${show(observed)}",
          s"expected a failure on token ${show(input)} and found it"
        )
    }
  }

  def failOn(input: Char): Matcher[Result[Char, _]] = failOnInput(Message.Input.Token(input))
  val failOnEof: Matcher[Result[Char, _]]           = failOnInput(Message.Input.Eof)
  val failOnNothing: Matcher[Result[Char, _]]       = failOnInput(Message.Input.None)
}
