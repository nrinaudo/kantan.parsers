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

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Shrink
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink.shrink
import org.typelevel.jawn.ast._

package object json {
  // - Arbitrary -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def isFinite(n: Double): Boolean =
    !java.lang.Double.isNaN(n) && !java.lang.Double.isInfinite(n)

  val jnull    = Gen.const(JNull)
  val jboolean = Gen.oneOf(JTrue :: JFalse :: Nil)
  val jlong    = arbitrary[Long].map(LongNum(_))
  val jdouble  = arbitrary[Double].filter(isFinite).map(DoubleNum(_))
  val jstring  = arbitrary[String].filter(_.nonEmpty).map(JString(_))

  // Totally unscientific atom frequencies.
  val jatom: Gen[JAtom] =
    Gen.frequency((1, jnull), (8, jboolean), (8, jlong), (8, jdouble), (16, jstring))

  // Use lvl to limit the depth of our jvalues.
  // Otherwise we will end up with SOE real fast.

  val MaxLevel: Int = 3

  def size(level: Int) = (MaxLevel + 1 - level) * 10

  def jarray(lvl: Int): Gen[JArray] =
    Gen.resize(size(lvl), Gen.containerOf[Array, JValue](jvalue(lvl + 1)).map(JArray(_)))

  def jitem(lvl: Int): Gen[(String, JValue)] =
    for {
      s <- arbitrary[String]
      j <- jvalue(lvl)
    } yield (s, j)

  def jobject(lvl: Int): Gen[JObject] =
    Gen.resize(size(lvl), Gen.containerOf[Vector, (String, JValue)](jitem(lvl + 1)).map(JObject.fromSeq))

  def jvalue(lvl: Int = 0): Gen[JValue] =
    if(lvl >= MaxLevel) jatom
    else Gen.frequency((16, jatom), (1, jarray(lvl)), (2, jobject(lvl)))

  implicit lazy val arbitraryJValue: Arbitrary[JValue] =
    Arbitrary(jvalue())

  implicit lazy val arbitraryJObject: Arbitrary[JObject] = Arbitrary(jobject(0))

  // - Shrink ----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  implicit val shrinkJObject: Shrink[JObject] = Shrink { case JObject(fields) =>
    shrink(fields.toList).map(_.filter(_._1.nonEmpty)).map(JObject.fromSeq)
  }

  implicit val shrinkJString: Shrink[JString] = Shrink(jstring => shrink(jstring.s).map(JString.apply))

  implicit val shrinkJArray: Shrink[JArray] = Shrink { case JArray(values) =>
    shrink(values).map(JArray.apply)
  }

  implicit val shrinkDeferLong: Shrink[DeferLong] = Shrink { value =>
    shrink(value.n).map(n => DeferLong(n.toString))
  }

  implicit val shrinkDeferNum: Shrink[DeferNum] = Shrink { value =>
    shrink(value.n).map(n => DeferNum(n.toString))
  }

  implicit val shrinkDoubleNum: Shrink[DoubleNum] = Shrink { value =>
    shrink(value.n).map(n => DoubleNum(n))
  }

  implicit val shrinkLongNum: Shrink[LongNum] = Shrink { value =>
    shrink(value.n).map(n => LongNum(n))
  }

  implicit val shrinkJValue: Shrink[JValue] = Shrink {
    case JNull             => Stream.empty
    case JTrue             => Stream(JFalse)
    case JFalse            => Stream.empty
    case dl: DeferLong     => shrink(dl)
    case dn: DeferNum      => shrink(dn)
    case double: DoubleNum => shrink(double)
    case long: LongNum     => shrink(long)
    case str: JString      => shrink(str)
    case arr: JArray       => shrink(arr)
    case obj: JObject      => shrink(obj)
  }
}
