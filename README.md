# kantan.parsers

## Why another library?

Most Scala parser libraries seem to be written with a heavy emphasis on performance and, as far as I can tell, always
make the same trade off: the input type is hard-coded to `String`.

kantan.parsers takes a different approach: it abstracts over the input type, and I really didn't focus too much on
performances except for a few obvious optimisations.

It might seem like an odd choice - after all, most parsers reasonable people can think of will be used to parse textual
content. But kantan.parsers has been written to scratch a very specific itch: end user experience, where the end user
is the person writing the content that will be parsed.

The "issue" with normal parser libraries is they produce parsers that will fail on the first error: as soon as an
unexepected character (or, in more advanced scenarios, a type error) is encountered, the entire process shuts down. 
If what you're trying to work with is a rich DSL used by domain experts, they'd likely appreciate the same kind of perks
developers have grown used to:
- syntax highlighting.
- as many known errors as possible, rather than just the first one.
- ...

Abstracting over the input type allows you to have multiple parsing phases. kantan.parsers was written with the
following in mind:
- tokenisation: turn a string into a list of tokens, tagged with their position in the file. This can be made total
  with a convenient "unknown" token type, and means that you'll always have basic syntax highlighting, even with invalid
  input.
- parsing: turn a list of tokens into an untyped AST. This should fail on syntactically incorrect input (`if while true`),
  but accept semantically invalid data (`true + 1`).
- type checking: turn an untyped AST into a typed one. This should fail whenever a type error is detected, but can look
  for *all* of them before aborting.

Of course, this multi-phase approach is more expensive than the regular parsec philosophy of doing everything all at
once. If the language you're writting a parser for is not edited interactively, you should probably look at other
parser libraries.

## State of kantan.parsers

This is, at this stage, a proof of concept. People seemed rather taken aback by the notion of abstracting over the input
type when I brought it up, and this is the best way of showing that it could be done *and* it could be useful.

It is, in all honesty, not very well tested at all - that is, I've manually ran it on large input files and made sure
it produced the same result as existing parsers, but there is currently not a single unit test. I'll get around to
writing some if there is more interest in this library than my simple _see, it *can* be done!_ motivation.

## Concepts

### Input type

The `AsTokens` type class is used to abstract over the input type: kantan.parsers can work with any time that can be
turned into an indexed sequence of tokens. For example, a `String` can easily be turned into an array of characters.

Provided your input type has an instance of `AsTokens`, you can pass values of it directly to `Parser.run`.

### Backtracking

Backtracking is something that sounds a lot more complex than it actually is. It's "just" a way of controlling the
behaviour of the `or` operator.

Consider the following example:

```scala
val parser =  string("foo") | string("bar")
parser.run("foa")
```

There's a decision to be made here: do we consider that if the first parser (`"foo"`) encountered at least one valid
character (`'f'`), then it's impossible for the second (`"bar"`) to be valid?

If so, you have a non-backtracking parser, in which characters, once consumed, stay consumed, and if a branch of
an `or` parser reads data, then the other one cannot be attempted.

If not, you have a non-backtracking parser, and any failure in the left hand side of `or` will result in the right hand
side being attempted.

kantan.parsers is a non-backtracking parser, because this usually makes for faster parsers and better error messages.
But, sometimes, a strictly non-backtracking parser yields undesirable behaviours:

```scala
val parser = string("foo") | string("foa")
parser.run("foa")
```

With a strictly non-backtracking parser, this will always fail, even though `"foa"` is perfectly valid input. Ideally,
you'd rewrite the parser so that there is no ambiguity - so that the left hand side and right hand side do not start
the same way - but that's not always possible. In this scenario, you can make the left hand side backtracking (by using
the `backtrack` method), and everything will behave as you'd expect.

### Labels

Parser error messages sometimes tend to not be very useful. kantan.parsers uses the notion of _labels_ to help with
this.

The general idea is that it's trivial to write error messages of the shape _Unexpected X_, but it's also not very
useful, even if tagged with the position in the input where the error occurred.

A better error message would be of the shape _Encountered X, was expecting one of: Y, Z_. `Y` and `Z` are labels.

For example:

```scala
val parser = foo.label("foo") | bar.label("bar")
parser.run("lorem ipsum")
```

Should this fail, kantan.parsers will be able to use the label information to generate an error message that looks like
_Encountered l, was expecting one of: foo, bar_.

### Position in the source code

To help error messages further, a parser keeps track of its position in the source file and is capable of exposing it
at any time through the `withPosition` method.

The slightly tricky bit is doing so on parsers that no longer work at the character level, but on tokens extracted
from a previous parsing pass.

kantan.parsers gives you the tools to do so through `SourceMap`, which maps between a token to its position in a source
file (given the end position of the previous token).

This is straightforward for characters (and supported out of the box):
- a character starts at the end position of the previous character.
- if a line break, a character ends at the the first column of the next line from the previous token.
- if not a line break, a character ends at the same line, next column from the previous token.

It's perfectly possible (and, indeed, expected) to provide instances for language-specific tokens, provided tokens
keep track of the positions in which they were found at the character level.

This is relatively straightforward, as shown in `JsonToken` in the examples.