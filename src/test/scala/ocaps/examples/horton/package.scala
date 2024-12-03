package ocaps.examples

package object horton {
  import ocaps.Brand._

  trait Fill[T] extends (Stub[T] => Unit)

  type FillBox[T] = Box[Fill[T]]

  trait Provide[T] extends (FillBox[T] => Unit)

  type Gift[T] = Box[Provide[T]]

  trait Wrap[T] extends ((Stub[T], Who) => Gift[T])

  trait Unwrap[T] extends ((Gift[T], Who) => Stub[T])

  type Proxy[T] = T
}
