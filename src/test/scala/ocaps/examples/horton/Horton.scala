package ocaps.examples.horton

import java.util.concurrent.atomic.AtomicReference

import ocaps.Brand._
import ocaps._

import scala.collection.mutable
import reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

// #horton
class Who(sealer: Sealer) {
  def apply[T](provide: Provide[T]): Gift[T] = sealer(provide)

  def apply[T](fill: Fill[T]): FillBox[T] = sealer(fill)

  def hint: Hint = sealer.hint

  override def toString: String = s"Who(sealer = $sealer)"
}

class Be(unsealer: Unsealer) {
  def apply[T](gift: Gift[T]): Provide[T] = {
    require(gift.hint == unsealer.hint, s"Gift hint ${gift.hint} does not match unsealer hint ${unsealer.hint}")
    unsealer(gift).get
  }

  def apply[T](fillbox: FillBox[T]): Fill[T] = {
    require(fillbox.hint == unsealer.hint, s"Fillbox hint ${fillbox.hint} does not match unsealer hint ${unsealer.hint}")
    unsealer(fillbox).get
  }

  def hint: Hint = unsealer.hint

  override def toString: String = s"Be(unsealer = $unsealer)"
}

trait Stub[T] {
  def intro(whoBob: Who): Gift[T]

  def deliver[ArgType](verb: String, desc: (Gift[ArgType], Who))
                      (implicit tTag: ru.TypeTag[T], cTag: ClassTag[T], proxyMaker: Principal.ProxyMaker[ArgType]): Any
}

trait ProxyAmps {
  def put[T](key: T, value: (Stub[T], Who)): Unit

  def apply[T](key: T): (Stub[T], Who)
}

class Principal(label: String, printer: String => Unit) {
  private val proxyAmps = new ProxyAmpsImpl()

  private val (whoMe, beMe) = {
    val brand = Brand.create(label)
    (new Who(brand.sealer), new Be(brand.unsealer))
  }

  val who: Who = whoMe

  def encodeFor[T](targ: T, whoBlame: Who): Gift[T] = {
    val stub = makeStub(whoBlame, targ)
    wrap(stub, whoBlame)
  }

  def decodeFrom[T](gift: Gift[T], whoBlame: Who)(implicit proxyMaker: Principal.ProxyMaker[T]): T = {
    val stub = unwrap(gift, whoBlame)
    implicit val context: proxyMaker.Context = proxyMaker.Context(this, whoBlame, reportln)
    val proxyB = proxyMaker.makeProxy(stub)
    proxyAmps.put(proxyB, (stub, whoBlame))
    proxyB
  }

  override def toString: String = s"Principal($label)"

  private def makeStub[T](who: Who, t: T): StubImpl[T] = {
    new StubImpl(who, t)
  }

  private def reportln(msg: String): Unit = {
    printer(s"$label said:\n> $msg")
  }

  private def wrap[T](stub: Stub[T], whoBlame: Who): Gift[T] = {
    val provide: Provide[T] = { fillBox: FillBox[T] =>
      val fill: Fill[T] = beMe(fillBox)
      fill(stub)
    }
    whoBlame(provide)
  }

  private def unwrap[T](gs3: Gift[T], whoCarol: Who): Stub[T] = {
    val provide: Provide[T] = beMe(gs3)
    val result = new AtomicReference[Stub[T]]()
    val fill: Fill[T] = s3 => result.set(s3)
    val fillBox: FillBox[T] = whoCarol(fill)
    provide(fillBox)
    result.get()
  }

  class StubImpl[T](whoBlame: Who, delegate: T) extends Stub[T] with Dynamic {

    def intro(whoBob: Who): Gift[T] = {
      log(s"meet ${whoBob.hint}")
      val stub = new StubImpl[T](whoBob, delegate)
      wrap(stub, whoBob)
    }

    def deliver[ArgType](verb: String, desc: (Gift[ArgType], Who))
                        (implicit tTag: ru.TypeTag[T], cTag: ClassTag[T], proxyMaker: Principal.ProxyMaker[ArgType]): Any = {
      log(s"$verb/1")

      val (gift3, whoBlame) = desc
      implicit val context: proxyMaker.Context = proxyMaker.Context(Principal.this, whoBlame, reportln)
      val stub3: Stub[ArgType] = unwrap(gift3, whoBlame)
      val proxy: scala.Proxy[ArgType] = proxyMaker.makeProxy(stub3)
      proxyAmps.put(proxy, (stub3, whoBlame))
      applyDynamic(verb)(proxy)(tTag, cTag)
    }

    //  http://www.erights.org/elib/capability/horton/amplify.html
    // https://gist.github.com/bartschuller/4687387
    def applyDynamic(method: String)(args: Any*)(implicit tTag: ru.TypeTag[T], classTag: ClassTag[T]): Any = {
      val m = ru.runtimeMirror(delegate.getClass.getClassLoader)
      val sym = ru.weakTypeTag[T].tpe.decl(ru.TermName(method)).asMethod
      val im = m.reflect(delegate)
      val methodMirror = im.reflectMethod(sym)
      methodMirror.apply(args: _*)
    }

    override def toString: String = {
      s"StubImpl(whoBlame = $whoBlame, delegate = $delegate, principal = $label)"
    }

    private def log(msg: String): Unit = {
      reportln(s"${whoBlame.hint} asks me to:\n> > " + msg)
    }
  }

  class ProxyAmpsImpl() extends ProxyAmps {
    private val weakMap = mutable.WeakHashMap[AnyRef, (Stub[_], Who)]()

    def put[T](key: T, value: (Stub[T], Who)): Unit = {
      weakMap(key.asInstanceOf[AnyRef]) = value
    }

    def apply[T](key: T): (Stub[T], Who) = {
      weakMap(key.asInstanceOf[AnyRef]).asInstanceOf[(Stub[T], Who)]
    }
  }

}

object Principal {

  trait ProxyMaker[T] {

    case class Context(principal: Principal, whoBlame: Who, reportln: String => Unit)

    /**
     * Implicitly converts from argument to a (Gift, Who) pair.
     */
    implicit final def createDescription[A](argument: A)(implicit context: Context): (Gift[A], Who) = {
      val (stubToIntro, whoFrom) = context.principal.proxyAmps(argument)
      val gift = stubToIntro.intro(context.whoBlame)
      (gift, whoFrom)
    }

    def makeProxy(stub: Stub[T])(implicit context: Context): scala.Proxy[T]
  }

}
// #horton