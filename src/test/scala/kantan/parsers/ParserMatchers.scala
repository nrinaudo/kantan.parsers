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
  private def formatMessage(msg: Message) =
    s"Expected one of ${msg.expected.mkString("'", "', '", "'")} but found ${msg.input}"

  def succeedWith[A](expected: A): Matcher[Result[_, A]] = new Matcher[Result[_, A]] {
    override def apply(result: Result[_, A]) = result match {
      case Result.Ok(_, parsed, _, _) =>
        MatchResult(
          parsed.value == expected,
          s"parse result '${parsed.value}' did not match '$expected'",
          s"parse result '${parsed.value}' matched '$expected'"
        )
      case Result.Error(_, msg) => MatchResult(false, formatMessage(msg), "parsing succeeded")
    }
  }

  def failWith(labels: String*): Matcher[Result[_, _]] = new Matcher[Result[_, _]] {

    def productions(labels: Iterable[String]) = labels.mkString(", ")

    override def apply(result: Result[_, _]) =
      result match {
        case Result.Ok(_, _, _, _) => MatchResult(false, "parsing did not fail", "parsing failed")
        case Result.Error(_, Message(_, _, _, items)) =>
          MatchResult(
            items.toSet == labels.toSet,
            s"expected productions '${productions(labels)}' did not match '${productions(items)}'",
            s"expected productions '${productions(labels)}' matched '${productions(items)}'"
          )
      }
  }
}
