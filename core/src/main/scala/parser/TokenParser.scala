package kantan.parsers

/** Optimised version of [[Parser]] when working at the token level.
  *
  * Knowing that we're working directly with the input allows us, for example, to treat repetition operators as ranging
  * over an array rather than the default step-by-step approach.
  *
  * For example:
  * {{{
  * val parser = digit.rep
  * parser.run("567")
  * }}}
  *
  * [[TokenParser]] will treat this as extracting the sub-sequence from index 0 to 3 in `567`, rather than parsing `5`,
  * `6` and `7` individually, and accumulate them in a list.
  */
private class TokenParser[Token: SourceMap](parse: State[Token] => Result[Token, Token], pred: Token => Boolean)
    extends Parser[Token, Token]:

  // - Parser methods --------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def run(state: State[Token]): Result[Token, Token] = parse(state)

  override def label(label: String): TokenParser[Token] = new TokenParser(run andThen (_.label(label)), pred)
  override def backtrack                                = new TokenParser(run andThen (_.empty), pred)

  // - Repetition ------------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------
  def rep(allowEmpty: Boolean): Parser[Token, Seq[Token]] = state =>
    val start = state.offset
    val stop =
      val index = state.input.indexWhere(token => !pred(token), start)
      if index < 0 then state.input.length
      else index

    if start < stop then
      val value    = state.input.slice(start, stop)
      val newState = state.consumeRep(value)

      // There's an easy to miss trap here: the start position of the value we've just parsed is not necessarily
      // the current position in the input. If, for example, we're working with non-contiguous tokens, the current
      // position represents where the previous token ends, which is not quite the same thing as where the new token
      // begins.
      val parsed = Parsed(value, state.startsAt(state.input(start)), newState.pos)

      Result.Ok(true, parsed, newState, Message.empty)
    else if allowEmpty then Result.Ok(false, Parsed(List.empty, state.pos, state.pos), state, Message.empty)
    else Result.Error(false, Message(state, List.empty))

  override def rep = rep(false)

  override def rep0 = rep(true)

object TokenParser:
  // - Basic parser ----------------------------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------------------------------------------

  def apply[Token: SourceMap](pred: Token => Boolean): TokenParser[Token] = new TokenParser(
    state =>
      if state.isEOF then Result.Error(false, Message(state, List.empty))
      else
        val value = state.input(state.offset)

        if pred(value) then
          val newState = state.consume(value)
          val parsed   = Parsed(value, state.startsAt(value), newState.pos)

          Result.Ok(true, parsed, newState, Message.empty)
        else Result.Error(false, Message(state, List.empty))
    ,
    pred
  )
