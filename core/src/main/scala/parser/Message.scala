package kantan.parsers

/** Parser error message.
  *
  * An error message contains:
  * - the index of the token at which the error was encountered.
  * - the position (line and column) at which the error was encountered.
  * - the token that cause the failure, as a string.
  * - a list of the values that were expected.
  */
case class Message(offset: Int, pos: Position, input: String, expected: List[String]):
  def expecting(label: String): Message      = copy(expected = List(label))
  def mergeExpected(other: Message): Message = copy(expected = expected ++ other.expected)

object Message:
  def empty: Message = Message(0, Position.zero, "", List.empty)

  def apply[Token: SourceMap](state: State[Token], expected: List[String]): Message =
    if state.isEOF then Message(state.offset, state.pos, "EOF", expected)
    else
      val token = state.input(state.offset)
      Message(state.offset, token.startsAt(state.pos), token.toString, expected)
