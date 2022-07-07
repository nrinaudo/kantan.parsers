package kantan.parsers

/** Parsed value, equiped with its start and end position in the original source code. */
case class Parsed[+A](value: A, start: Position, end: Position):
  def map[B](f: A => B): Parsed[B]        = copy(value = f(value))
  def withStart(pos: Position): Parsed[A] = copy(start = pos)
