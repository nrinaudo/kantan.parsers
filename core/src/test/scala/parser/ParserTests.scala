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

import kantan.parsers.Parser.string
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.compatible.Assertion

class ParserTests extends AnyFunSuite with Matchers {
  // - Helpers ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def assertSuccess[Token, A](result: Result[Token, A], expected: A): Assertion = result match
    case Result.Ok(_, parsed, _, _) => assert(parsed.value == expected)
    case _                          => fail(s"Expected a success but parsing failed.")

  def assertFailure[Token, A](result: Result[Token, A], expected: String*): Assertion = result match
    case Result.Ok(_, _, _, _)                    => fail(s"Expected a failure but parsing succeeded.")
    case Result.Error(_, Message(_, _, _, items)) => assert(expected.toSet == items.toSet)

  // - `|` tests -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs | rhs selects the successful case when one fails") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    assertSuccess(parser.run("foo"), "foo")
    assertSuccess(parser.run("bar"), "bar")
  }

  test("lhs | rhs fails with the expected message when both fail") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    assertFailure(parser.run("baz"), "foo", "bar")
  }

  // - Composition tests -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs ~ rhs succeeds when both succeed") {
    val parser = string("foo") ~ string("bar")

    assertSuccess(parser.run("foobar"), ("foo", "bar"))
  }

  test("lhs ~ rhs fails with the expected message when one fails") {
    val parser = string("foo") ~ string("bar")

    assertFailure(parser.run("fooboo"), "bar")
    assertFailure(parser.run("faabar"), "foo")
  }
}
