package kantan.parsers

/** Type class used to keep track of a a token's position in a source file.
  *
  * A source map knows how to compute the following, given the current position in the input:
  *   - where the token starts.
  *   - where the token ends.
  *
  * In the case of characters, for example, the mapping is fairly straightforward. A character:
  *   - starts at the current position.
  *   - ends at the beginning of the following line if the character is a line break.
  *   - ends at the next column otherwise.
  *
  * One might imagine more complex scenarios, however. Typically, when splitting tokenization and parsing, you'll end up
  * working with tokens that know their position in the original source code.
  */
trait SourceMap[Token]:
  extension (token: Token)
    def endsAt(current: Position): Position
    def startsAt(current: Position): Position

// - Default instances -------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
object SourceMap:
  given SourceMap[Char] with
    extension (token: Char)
      def endsAt(current: Position) =
        if token == '\n' then current.nextLine
        else current.nextColumn

      def startsAt(current: Position) = current
