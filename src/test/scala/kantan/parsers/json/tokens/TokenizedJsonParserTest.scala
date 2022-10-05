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

import kantan.parsers.{Parsed, Result}
import kantan.parsers.json._

class TokenizedJsonParserTests extends JsonParserSuite {
  /*
    final case class Ok[Token, A](consumed: Boolean, value: Parsed[A], state: State[Token], message: Message)
      extends Result[Token, A]
  final case class Error[Token](consumed: Boolean, message: Message) extends Result[Token, Nothing]
   */
  override def parse(input: String) =
    JsonToken.parser.run(input) match {
      case failure: Result.Error[_]                 => failure
      case Result.Ok(_, Parsed(result, _, _), _, _) => jExp.run(result)
    }

}
