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

import kantan.parsers.Parser.{char, string}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.compatible.Assertion

class ParserTests extends AnyFunSuite with Matchers with ParserMatchers {

  // - `|` tests -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs | rhs selects the successful case when one fails without consuming") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    parser.run("foo") should succeedWith("foo")
    parser.run("bar") should succeedWith("bar")
  }

  test("lhs | rhs fails with the expected message when both fail") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.run("car") should failWith("foo", "bar")

    // lhs did not consume, rhs did, so we were expecting rhs.
    parser.run("baz") should failWith("bar")

    // lhs consumed, so we were expecting lhs.
    parser.run("faz") should failWith("foo")
  }

  test("lhs | rhs succeeds or fails at the same time as lhs if lhs consumes") {
    val lhs    = string("foo")
    val rhs    = string("far")
    val parser = lhs | rhs

    parser.run("foo") should succeedWith("foo")
    parser.run("far") should failWith("foo")
  }

  test("lhs.backtrack | rhs succeeds or fails at the same time as rhs if lhs fails") {
    val lhs    = string("foo").backtrack
    val rhs    = string("far")
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.run("bar") should failWith("foo", "far")

    // lhs did not consume, rhs did, so we were expecting rhs.
    parser.run("for") should failWith("far")

    parser.run("far") should succeedWith("far")
  }

  test("lhs | rhs.backtrack fails with the expected message when both fail") {
    val lhs    = string("foo")
    val rhs    = string("bar").backtrack
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.run("baz") should failWith("foo", "bar")

    // lhs consumed, so we're expecting lhs.
    parser.run("far") should failWith("foo")
  }

  // - Composition tests -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs ~ rhs succeeds when both succeed") {
    val parser = string("foo") ~ string("bar")

    parser.run("foobar") should succeedWith(("foo", "bar"))
  }

  test("lhs ~ rhs fails with the expected message when one fails") {
    val parser = string("foo") ~ string("bar")

    parser.run("fooboo") should failWith("bar")
    parser.run("faabar") should failWith("foo")
  }

  // - `?` tests -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.? succeeds when one ocurrence is found") {
    val parser = string("foo").?

    parser.run("foo") should succeedWith(Option("foo"))
  }

  test("parser.? succeeds when no ocurrence is found") {
    val parser = string("foo").?

    parser.run("bar") should succeedWith(Option.empty[String])
  }

  // - Repetition tests ------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.rep succeeds when it finds at least one occurence") {
    val parser = string("foo").rep

    parser.run("foo") should succeedWith(Seq("foo"))
    parser.run("foofoofoo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.rep0 succeeds when it finds at least one occurence") {
    val parser = string("foo").rep0

    parser.run("foo") should succeedWith(Seq("foo"))
    parser.run("foofoofoo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.repSep succeeds when it finds at least one occurence") {
    val parser = string("foo").repSep(char('.'))

    parser.run("foo") should succeedWith(Seq("foo"))
    parser.run("foo.foo.foo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.repSep0 succeeds when it finds at least one occurence") {
    val parser = string("foo").repSep0(char('.'))

    parser.run("foo") should succeedWith(Seq("foo"))
    parser.run("foo.foo.foo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.rep fails when it finds no occurence") {
    val parser = string("foo").rep

    parser.run("bar") should failWith("foo")
  }

  test("parser.rep0 succeeds when it finds no occurence") {
    val parser = string("foo").rep0

    parser.run("bar") should succeedWith(Seq.empty[String])
  }

  test("parser.repSep fails when it finds no occurence") {
    val parser = string("foo").repSep(char('.'))

    parser.run("bar") should failWith("foo")
  }

  test("parser.repSep0 succeeds when it finds no occurence") {
    val parser = string("foo").repSep0(char('.'))

    parser.run("bar") should succeedWith(Seq.empty[String])
  }

  // - "surrounded" tests ----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.surroundedBy succeeds when surrounded") {
    val parser = string("foo").surroundedBy(char('|'))

    parser.run("|foo|") should succeedWith("foo")
  }

  test("parser.between succeeds when surrounded") {
    val parser = string("foo").between(char('<'), char('>'))

    parser.run("<foo>") should succeedWith("foo")
  }

  test("parser.surroundedBy fails when not surrounded") {
    val parser = string("foo").surroundedBy(char('|'))

    parser.run("foo") should failWith("|")
    parser.run("|foo") should failWith("|")
    parser.run("foo|") should failWith("|")
  }

  test("parser.between fails when not surrounded") {
    val parser = string("foo").between(char('<'), char('>'))

    parser.run("foo") should failWith("<")
    parser.run("<foo") should failWith(">")
    parser.run("foo>") should failWith("<")
  }

  // - Position tests --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.withPosition yields the right position") {
    val parser = string("foo") *> string("bar").withPosition <* string("baz")

    parser.run("foobarbaz") should succeedWith(Parsed("bar", Position(0, 3), Position(0, 6)))
  }
}
