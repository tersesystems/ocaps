package ocaps

// construct a sealing monad, where the same unsealer is used for all boxes.


//trait BoxyBox[+T] {
//  type C
//}
//
//trait CanAccess {
//  type C
//}
//
//sealed trait Packed[+T] {
//  val box: BoxyBox[T]
//  val access: CanAccess { type C = box.C }
//}
//
//class Foo {
//  def doFoo(box: BoxyBox[Any])(implicit acc: CanAccess { type C = box.C }): Unit = {
//    acc
//  }
//}
//
//new Foo()