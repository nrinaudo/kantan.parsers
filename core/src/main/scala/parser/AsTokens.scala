package kantan.parsers

/** Type class that describes the capacity to tokenize something.
  *
  * A tokenized value is an indexed sequence (ideally an array). This allows the parser to navigate input not by
  * consuming it bit by bit and constructing tons of intermediate representations, but as a pointer in something
  * array-ish.
  *
  * Strings, for example, can be turned into an array of characters at very little cost.
  */
trait AsTokens[Source, Token]:
  extension (source: Source) def asTokens: IndexedSeq[Token]

object AsTokens:
  /** Strings tokenize to arrays of characters. */
  given AsTokens[String, Char] with
    extension (source: String) def asTokens = IArray.from(source)

  /** Indexed sequences are already tokenized. */
  given [Token]: AsTokens[IndexedSeq[Token], Token] with
    extension (source: IndexedSeq[Token]) def asTokens = source

  given [Token]: AsTokens[Seq[Token], Token] with
    extension (source: Seq[Token]) def asTokens = source.toIndexedSeq

  // TODO: I'm not entirely clear yet why this is necessary and not taken care of by `AsTokens[Seq[Token]]`.
  given [Token]: AsTokens[List[Token], Token] with
    extension (source: List[Token]) def asTokens = source.toIndexedSeq
