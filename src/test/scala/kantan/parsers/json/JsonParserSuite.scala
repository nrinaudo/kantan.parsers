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

import kantan.parsers.Result
import kantan.parsers.json._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.jawn.ast.{JObject, JValue}
import kantan.parsers.ParserMatchers

trait JsonParserSuite extends AnyFunSuite with ScalaCheckPropertyChecks with Matchers with ParserMatchers {

  def parse(input: String): Result[_, JValue]

  def roundTrip(value: JValue) =
    parse(value.toString()) should succeedWith(value)

  test("JSON values should be decoded as expected") {
    forAll { (obj: JObject) =>
      roundTrip(obj)
    }
  }
}
