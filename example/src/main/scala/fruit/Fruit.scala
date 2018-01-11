package fruit

import scala.util._
import com.tersesystems.capabilities._

sealed trait FruitState

object FruitState {
  case object Uneaten extends FruitState
  case object Eaten extends FruitState
}

sealed trait Fruit {
  protected[fruit] var state: FruitState = FruitState.Uneaten
}

final class Apple extends Fruit {
  override def toString: String = s"Apple($state)"
}

final class Pear extends Fruit {
  override def toString: String = s"Pear($state)"
}

object Eatable extends Authority[Fruit] {

  // Because we have a higher kinded type, we can pass through the specific type of fruit
  override type Capability[DSO <: Fruit] = EatableCapability[DSO]

  override type Args = Unit

  override protected def newCapability[DSO <: Fruit](fruit: DSO, args: Args*): Try[Capability[DSO]] = {
    Success(new Capability(fruit))
  }

  class EatableCapability[DSO <: Fruit] private[Eatable] (f: DSO) {
    // We want to return the most specific type, i.e. Apple rather than Fruit
    def eat(): DSO = {
      import FruitState._

      f.state match {
        case Uneaten =>
          f.state = Eaten
          f
        case Eaten =>
          throw new IllegalStateException("Already eaten!")
      }
    }
  }
}
