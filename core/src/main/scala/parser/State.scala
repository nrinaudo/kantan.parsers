package kantan.parsers

/** State of a parser.
  *
  * A parser works with:
  *   - an array of tokens to explore (typically the characters that compose a string).
  *   - an offset in that array that represents how far we've parsed already.
  *   - the position of the last parsed token.
  *
  * TODO: before writing documentation, we need to keep track of a token's START and END position. It makes things far
  * easier to explain. With chars, a token's start position is always the previous token's end position. With more
  * complex tokens, this might not hold - think of space-separated ints: "1 2". You cannot guess the start position of
  * '2' just from '1': this doesn't tell you how many spaces there are before the next token starts.
  */
case class State[Token: SourceMap](input: IndexedSeq[Token], offset: Int, pos: Position):

  def isEOF: Boolean = offset >= input.length

  def startsAt(parsed: Token): Position = parsed.startsAt(pos)

  def consume(parsed: Token): State[Token] = copy(offset = offset + 1, parsed.endsAt(pos))

  def consumeRep(parsed: Seq[Token]): State[Token] =
    val newPos = parsed.foldLeft(pos)((curr, token) => token.endsAt(curr))
    copy(offset = offset + parsed.length, newPos)

object State:
  def init[Token: SourceMap](tokens: IndexedSeq[Token]): State[Token] = State(tokens, 0, Position.zero)
