package kantan.parsers

/** Result of a parsing operation.
  *
  * This is essentially a very specialised version of `Either` (and can, in fact, be turned into one through
  * [[toEither]]).
  *
  * A result keeps track of whether or not any data has been consumed when producing it. This is used to decide whether
  * or not to try alternative parsers in a [[Parser.|]] call.
  *
  * Results also store an error message *even if they're successful*. This might seem a little odd, but is necessary to
  * be able to provide good error messages for combinators such as [[Parser.filter]].
  */
enum Result[Token, +A]:
  case Ok(consumed: Boolean, value: Parsed[A], state: State[Token], message: Message)
  case Error[Token](consumed: Boolean, message: Message) extends Result[Token, Nothing]

  def toEither: Either[Message, A] = this match
    case Result.Ok(_, parsed, _, _) => Right(parsed.value)
    case Result.Error(_, msg)       => Left(msg)

  def setStart(pos: Position): Result[Token, A] = this match
    case ok: Ok[Token, A] => ok.copy(value = ok.value.copy(start = pos))
    case other            => other

  // - Common properties -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def consumed: Boolean
  def message: Message

  // - Label handling --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Sets this parser's label.
    *
    * Note that this only happens on non-consuming parsers: if the parser consumed data, then he's *not* one the
    * possible outcomes of a disjunction, but the right branch of the disjunction. It just happened to fail.
    *
    * Take for example:
    * {{{
    * val parser = string("foo") | string("bar")
    * parser.run("foa")
    * }}}
    *
    * In this context, `bar` is clearly not a possibility - we've started parsing `foo` succesfully, and we want a
    * message like "Expected o, found a". Applying the label to a consuming result would, instead, yield "Expected foo,
    * found a", which is less useful.
    */
  def label(label: String): Result[Token, A] =
    this match
      case Result.NonConsuming(result) => result.mapMessage(_.expecting(label))
      case other                       => other

  // - Mapping ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def mapMessage(f: Message => Message): Result[Token, A] = this match
    case ok: Ok[Token, A]    => ok.copy(message = f(message))
    case error: Error[Token] => error.copy(message = f(message))

  def map[B](f: A => B): Result[Token, B] = this match
    case ok: Ok[Token, A] => ok.copy(value = ok.value.map(f))
    case e: Error[Token]  => e

  // - Backtrack handling ----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Marks this result as consuming. */
  def consume: Result[Token, A] = this match
    case ok: Ok[Token, A]  => ok.copy(consumed = true)
    case err: Error[Token] => err.copy(consumed = true)

  /** Marks this result as non-consuming. */
  def empty: Result[Token, A] = this match
    case ok: Ok[Token, A]  => ok.copy(consumed = false)
    case err: Error[Token] => err.copy(consumed = false)

// - Helpers -----------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
object Result:
  object NonConsuming:
    def unapply[Token, A](result: Result[Token, A]): Option[Result[Token, A]] =
      if result.consumed then None
      else Some(result)
