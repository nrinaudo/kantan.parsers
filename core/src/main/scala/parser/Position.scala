package kantan.parsers

/** Represents a position in a source file.
  *
  * This is supposed to work in conjunction with [[SourceMap]], to allow a parser to automatically keep track of where
  * in a source file a token was encountered.
  */
case class Position(line: Int, column: Int):
  def nextLine: Position   = Position(line + 1, 0)
  def nextColumn: Position = Position(line, column + 1)

object Position:
  val zero: Position = Position(0, 0)
