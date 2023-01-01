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

import kantan.parsers.Parser.{char, digit, letter, string}
import org.scalacheck.Shrink
import org.scalacheck.Gen.{alphaChar, alphaNumChar, numChar}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.compatible.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ParserTests extends AnyFunSuite with Matchers with ParserMatchers with ScalaCheckPropertyChecks {

  /** Disables shrinking of characters, as we quickly end up with nonsensical shrinking. */
  implicit val shrinkChar: Shrink[Char] = Shrink.shrinkAny

  // - `|` tests -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs | rhs selects the successful case when one fails without consuming") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    parser.parse("foo") should succeedWith("foo")
    parser.parse("bar") should succeedWith("bar")
  }

  test("lhs | rhs fails with the expected message when both fail") {
    val lhs    = string("foo")
    val rhs    = string("bar")
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.parse("car") should (failWith("foo", "bar") and failOn('c') and failAt(0, 0))

    // lhs did not consume, rhs did, so we were expecting rhs.
    parser.parse("baz") should (failWith("bar") and failOn('z') and failAt(0, 2))

    // lhs consumed, so we were expecting lhs.
    parser.parse("faz") should (failWith("foo") and failOn('a') and failAt(0, 1))
  }

  test("lhs | rhs succeeds or fails at the same time as lhs if lhs consumes") {
    val lhs    = string("foo")
    val rhs    = string("far")
    val parser = lhs | rhs

    parser.parse("foo") should succeedWith("foo")
    parser.parse("far") should (failWith("foo") and failOn('a') and failAt(0, 1))
  }

  test("lhs.backtrack | rhs succeeds or fails at the same time as rhs if lhs fails") {
    val lhs    = string("foo").backtrack
    val rhs    = string("far")
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.parse("bar") should (failWith("foo", "far") and failOn('b') and failAt(0, 0))

    // lhs did not consume, rhs did, so we were expecting rhs.
    parser.parse("for") should (failWith("far") and failOn('o') and failAt(0, 1))

    parser.parse("far") should succeedWith("far")
  }

  test("lhs | rhs.backtrack fails with the expected message when both fail") {
    val lhs    = string("foo")
    val rhs    = string("bar").backtrack
    val parser = lhs | rhs

    // neither lhs nor rhs consumed, it could be either.
    parser.parse("baz") should (failWith("foo", "bar") and failOn('z') and failAt(0, 2))

    // lhs consumed, so we're expecting lhs.
    parser.parse("far") should (failWith("foo") and failOn('a') and failAt(0, 1))
  }

  // - Composition tests -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("lhs ~ rhs succeeds when both succeed") {
    val parser = string("foo") ~ string("bar")

    parser.parse("foobar") should succeedWith(("foo", "bar"))
  }

  test("lhs ~ rhs fails with the expected message when one fails") {
    val parser = string("foo") ~ string("bar")

    parser.parse("fooboo") should (failWith("bar") and failOn('o') and failAt(0, 4))
    parser.parse("faabar") should (failWith("foo") and failOn('a') and failAt(0, 1))
  }

  // - `?` tests -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.? succeeds when one ocurrence is found") {
    val parser = string("foo").?

    parser.parse("foo") should succeedWith(Option("foo"))
  }

  test("parser.? succeeds when no ocurrence is found") {
    val parser = string("foo").?

    parser.parse("bar") should succeedWith(Option.empty[String])
  }

  // - Repetition tests ------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.rep succeeds when it finds at least one occurence") {
    val parser = string("foo").rep

    parser.parse("foo") should succeedWith(Seq("foo"))
    parser.parse("foofoofoo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.rep0 succeeds when it finds at least one occurence") {
    val parser = string("foo").rep0

    parser.parse("foo") should succeedWith(Seq("foo"))
    parser.parse("foofoofoo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.repSep succeeds when it finds at least one occurence") {
    val parser = string("foo").repSep(char('.'))

    parser.parse("foo") should succeedWith(Seq("foo"))
    parser.parse("foo.foo.foo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.repSep0 succeeds when it finds at least one occurence") {
    val parser = string("foo").repSep0(char('.'))

    parser.parse("foo") should succeedWith(Seq("foo"))
    parser.parse("foo.foo.foo") should succeedWith(Seq("foo", "foo", "foo"))
  }

  test("parser.rep fails when it finds no occurence") {
    val parser = string("foo").rep

    parser.parse("bar") should (failWith("foo") and failOn('b') and failAt(0, 0))
  }

  test("parser.rep0 succeeds when it finds no occurence") {
    val parser = string("foo").rep0

    parser.parse("bar") should succeedWith(Seq.empty[String])
  }

  test("parser.repSep fails when it finds no occurence") {
    val parser = string("foo").repSep(char('.'))

    parser.parse("bar") should (failWith("foo") and failOn('b') and failAt(0, 0))
  }

  test("parser.repSep0 succeeds when it finds no occurence") {
    val parser = string("foo").repSep0(char('.'))

    parser.parse("bar") should succeedWith(Seq.empty[String])
  }

  // - "surrounded" tests ----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.surroundedBy succeeds when surrounded") {
    val parser = string("foo").surroundedBy(char('|'))

    parser.parse("|foo|") should succeedWith("foo")
  }

  test("parser.between succeeds when surrounded") {
    val parser = string("foo").between(char('<'), char('>'))

    parser.parse("<foo>") should succeedWith("foo")
  }

  test("parser.surroundedBy fails when not surrounded") {
    val parser = string("foo").surroundedBy(char('|'))

    parser.parse("foo") should (failWith("|") and failOn('f') and failAt(0, 0))
    parser.parse("|foo") should (failWith("|") and failOnEof and failAt(0, 4))
    parser.parse("foo|") should (failWith("|") and failOn('f') and failAt(0, 0))
  }

  test("parser.between fails when not surrounded") {
    val parser = string("foo").between(char('<'), char('>'))

    parser.parse("foo") should (failWith("<") and failOn('f') and failAt(0, 0))
    parser.parse("<foo") should (failWith(">") and failOnEof and failAt(0, 4))
    parser.parse("foo>") should (failWith("<") and failOn('f') and failAt(0, 0))
  }

  // - Position tests --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("parser.withPosition yields the right position") {
    val parser = string("foo") *> string("bar").withPosition <* string("baz")

    parser.parse("foobarbaz") should succeedWith(Parsed("bar", Position(0, 3), Position(0, 6)))
  }

  // - Filter tests ----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("for-comprehensions allow decomposition") {
    val p1 = string("foo")
    val p2 = string("bar")
    val p3 = string("baz")

    val parser = for {
      (t1, t2) <- p1 ~ p2
      t3       <- p3
    } yield ()

    parser.parse("foobarbaz") should succeedWith(())
  }

  test("! behaves as expected") {
    val parser = (string("foo") <* !char('1')) ~ digit

    parser.parse("foo1") should (failWith("not 1") and failOnNothing and failAt(0, 0))
    parser.parse("foo2") should succeedWith(("foo", '2'))
  }

  // - Letter & digit tests --------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  test("Letters should be parsed as expected") {
    forAll(alphaChar) { c =>
      Parser.letter.parse(c.toString) should succeedWith(c)
      Parser.digit.parse(c.toString) should failWith("digit")
    }
  }

  test("Digits should be parsed as expected") {
    forAll(numChar) { c =>
      Parser.digit.parse(c.toString) should succeedWith(c)
      Parser.letter.parse(c.toString) should failWith("letter")
    }
  }

  test("Letters and digits should be parsed as expected") {
    forAll(alphaNumChar) { c =>
      (Parser.letter | Parser.digit).parse(c.toString) should succeedWith(c)
    }
  }
}
