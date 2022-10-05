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

/** Parser error message.
  *
  * An error message contains:
  *   - the index of the token at which the error was encountered.
  *   - the position (line and column) at which the error was encountered.
  *   - the token that cause the failure, as a string.
  *   - a list of the values that were expected.
  */
final case class Message(offset: Int, pos: Position, input: String, expected: List[String]) {
  def expecting(label: String): Message      = copy(expected = List(label))
  def mergeExpected(other: Message): Message = copy(expected = expected ++ other.expected)
}

object Message {
  def empty: Message = Message(0, Position.zero, "", List.empty)

  def apply[Token: SourceMap](state: State[Token], expected: List[String]): Message =
    if(state.isEOF) Message(state.offset, state.pos, "EOF", expected)
    else {
      val token = state.input(state.offset)
      Message(state.offset, SourceMap[Token].startsAt(token, state.pos), token.toString, expected)
    }
}
