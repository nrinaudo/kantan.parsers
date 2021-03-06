package kantan.parsers

/** Parses a sequence of `Token` into an `A`.
  *
  * The companion object provides standard parsers from which to start building larger ones. In particular, it contains
  * the necessary tools to start writing a string parser, such as [[char]] and [[string]].
  *
  * In order to provide better error messages, developers are encouraged to use [[label]] to describe the kind of thing
  * a parser will produce - a digit, for example, or an array, or...
  *
  * An important thing to realise is that parsers are non-backtracking by default. See the [[|]] documentation for
  * detailed information on the consequences of this design choice.
  */
trait Parser[Token, +A]:
  // - Main methods ----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def run[Source](input: Source)(using AsTokens[Source, Token], SourceMap[Token]): Result[Token, A] = run(
    State.init(input.asTokens)
  )

  protected def run(state: State[Token]): Result[Token, A]

  // - Label handling --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Sets the label of this parser.
    *
    * A parser's label is a simple description of what they're expecting to parse. Typically, a parser who means to
    * parse a "true / false" value would have a `boolean` label.
    *
    * This allows us to provide meaningful error messages, where instead of saying "expected [, { or true", we can have
    * "expected array, object or boolean".
    */
  def label(label: String): Parser[Token, A] = state => run(state).label(label)

  // - Filtering -------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  /** Fails any parser that does not match the specified predicate.
    *
    * Note that such parsers are, of necessity, backtracking. Consider the following admitedly silly example:
    * {{{
    * val parser = digit.filter(_ != '9') | char('9')
    * parser.run("9")
    * }}}
    *
    * If [[filter]] didn't turn [[digit]] backtracking, then this parser would fail, even though `9` is perfectly valid
    * input for it. [[digit]] would succeed and consume the `9`, then [[filter]] would fail the parser with a consuming
    * result - `char('9')` would not even be attempted.
    */
  def filter(f: A => Boolean): Parser[Token, A] = state =>
    run(state) match
      case Result.Ok(_, parsed, _, msg) if !f(parsed.value) =>
        Result.Error(false, msg.copy(input = parsed.value.toString))
      case other => other

  def filterNot(f: A => Boolean): Parser[Token, A] = filter(f andThen (b => !b))

  /** A [[filter]] and a [[map]] rolled into one.
    *
    * For similar reasons to [[filter]], such parsers are backtracking. Consider the following example:
    * {{{
    * val parser = digit.collect {
    *   case c if c != '9' => c.toInt
    * } | char('9').as(9)
    *
    * parser.run("9")
    * }}}
    *
    * If [[collect]] didn't turn the parser backtracking, this would fail, even though `9` is valid input: [[digit]]
    * would succeed and consume the `9`, then [[collect]] would fail the parser with a consuming result - `char('9')`
    * would not even be attempted.
    */
  def collect[B](f: PartialFunction[A, B]): Parser[Token, B] = state =>
    run(state) match
      case Result.Ok(consumed, parsed, state, msg) =>
        f.lift(parsed.value) match
          case Some(b) => Result.Ok(consumed, parsed.copy(value = b), state, msg)
          case None    => Result.Error(false, msg.copy(input = parsed.value.toString, pos = parsed.start))
      case error: Result.Error[Token] => error

  // - Mapping ---------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def map[B](f: A => B): Parser[Token, B] = state => run(state).map(f)

  def as[B](b: B): Parser[Token, B] = map(_ => b)

  def withPosition: Parser[Token, Parsed[A]] = state =>
    run(state) match
      case Result.Ok(consumed, parsed, state, msg) => Result.Ok(consumed, parsed.map(_ => parsed), state, msg)
      case failure: Result.Error[Token]            => failure

  // - Combining parsers -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def flatMap[B](f: A => Parser[Token, B]): Parser[Token, B] =
    state =>
      run(state) match
        case Result.Ok(true, parsed, rest, _)  => f(parsed.value).run(rest).consume.setStart(parsed.start)
        case Result.Ok(false, parsed, rest, _) => f(parsed.value).run(rest).setStart(parsed.start)
        case error: Result.Error[Token]        => error

  /** Attempts either this parser or the specified one.
    *
    * Note that this is non-backtracking: the alternative parser will only be tried if this parser is non-consuming.
    *
    * Consider the following example:
    * {{{
    * val parser =  string("foo") | string("bar")
    * parser.run("foa")
    * }}}
    *
    * It's perfectly impossible for `bar` to be a valid match here, and we know that as soon as we've started
    * successfuly parsing `foo`. A non-backtracking parser will not attempt `bar`, which yields:
    *   - performance improvement (we're not trying parses that we know will fail).
    *   - better error messages (the error isn't that we were expecting `foo` or `bar`, but that we were parsing `foo`
    *     and found an `a` where we expected an `o`).
    *
    * It is sometimes necessary to override that behaviour. Take the following example:
    * {{{
    * val parser = string("foo") | string("foa")
    * parser.run("foa")
    * }}}
    *
    * We want this to succeed, but since `string("foo")` is non-backtracking, `string("foa")` will not be attempted. In
    * these scenarios, calling [[backtrack]] on `string("foo")` allows `parser` to attempt `string("foa")`
    *
    * Finally, consider the following:
    * {{{
    * val parser = string("foo").backtrack | string("bar")
    * parser.run("bar")
    * }}}
    *
    * This will succeed: non-consuming successes will still result in the alternative parser being attempted, to try and
    * find the first result that actually consumes data.
    */
  def |[AA >: A](p2: => Parser[Token, AA]): Parser[Token, AA] = state =>
    run(state) match
      case Result.NonConsuming(result) =>
        p2.run(state) match
          // Note that we only merge labels if both parsers are non-consuming. If we have consumed data, then the parser
          // is not one of the expected inputs, but *the* expected input that just happened to have failed.
          case Result.NonConsuming(result) => result.mapMessage(_.mergeExpected(result.message))
          case other                       => other
      case other => other

  def ~[B](p2: Parser[Token, B]): Parser[Token, (A, B)] = for
    a <- this
    b <- p2
  yield (a, b)

  def *>[B](p2: Parser[Token, B]): Parser[Token, B] = for
    _ <- this
    b <- p2
  yield b

  def <*[B](p2: Parser[Token, B]): Parser[Token, A] = for
    a <- this
    _ <- p2
  yield a

  // - Misc. -----------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def surroundedBy[B](p: Parser[Token, B]): Parser[Token, A] = between(p, p)

  def between[Left, Right](left: Parser[Token, Left], right: Parser[Token, Right]): Parser[Token, A] =
    left *> this <* right

  def backtrack: Parser[Token, A] = state => run(state).empty

  def ? : Parser[Token, Option[A]] = map(Option.apply) | Parser.pure(None)

  // - Repeating parsers -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def rep0: Parser[Token, Seq[A]] = this.rep | Parser.pure(List.empty)

  def rep: Parser[Token, Seq[A]] = for
    head <- this
    tail <- this.rep0
  yield head +: tail

  def repSep[Sep](sep: Parser[Token, Sep]): Parser[Token, Seq[A]] =
    val nonEmpty = (this ~ (sep *> this).rep0).map(_ +: _)
    nonEmpty | Parser.pure(Seq.empty)

// - Base parsers ------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------

object Parser:
  // - Common operations -----------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def pure[Token, A](value: A): Parser[Token, A] = state =>
    Result.Ok(false, Parsed(value, state.pos, state.pos), state, Message.empty)

  def ap[Token, A, B](ff: Parser[Token, A => B]): Parser[Token, A] => Parser[Token, B] = fa =>
    for
      a <- fa
      f <- ff
    yield f(a)

  // - Base parsers ----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def token[Token: SourceMap]: Parser[Token, Token] = TokenParser(_ => true)

  def satisfy[Token: SourceMap](f: Token => Boolean): Parser[Token, Token] =
    TokenParser(f)

  def end[Token: SourceMap]: Parser[Token, Unit] = state =>
    if state.isEOF then Result.Ok(false, Parsed((), state.pos, state.pos), state, Message.empty)
    else Result.Error(false, Message(state, List("EOF")))

  def oneOf[Token, A](head: Parser[Token, A], tail: Parser[Token, A]*): Parser[Token, A] = tail.foldLeft(head)(_ | _)

  def sequence[Token, A](parsers: List[Parser[Token, A]]): Parser[Token, List[A]] = parsers match
    case head :: tail =>
      for
        h <- head
        t <- sequence(tail)
      yield h :: t
    case Nil => pure(Nil)

  // - String parsers --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def string(s: String): Parser[Char, String] = sequence(s.toList.map(char)).map(_.mkString).label(s)

  def char(c: Char): Parser[Char, Char]            = satisfy[Char](_ == c).label(c.toString)
  def char(f: Char => Boolean): Parser[Char, Char] = satisfy(f)

  def letter: Parser[Char, Char]     = satisfy[Char](_.isLetter).label("letter")
  def digit: Parser[Char, Char]      = satisfy[Char](_.isDigit).label("digit")
  def whitespace: Parser[Char, Char] = satisfy[Char](_.isWhitespace).label("whitespace")

  def identifier: Parser[Char, String] = (letter | digit | char('_')).rep.map(_.mkString)
