/*
 * Copyright 2018 Will Sargent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ocaps

object macros {

  import scala.reflect.macros._

  /**
    * Composition merges together two capabilities into one that has the power of both.
    *
    * Use this as follows:
    *
    * {{{
    * val doerChanger: Doer with Changer = compose[Doer with Changer](doer, changer)
    * }}}
    *
    * and it has the effect of
    *
    * {{{
    * object Foo {
    *
    *  trait Doer {
    *    def doTheThing(): Unit
    *  }
    *
    *  trait Changer {
    *    def changeName(name: String): Foo
    * }
    *
    *  def amplify(doer: => Foo.Doer, changer: => Foo.Changer) = {
    *     new Doer with Changer {
    *       override def doTheThing(): Unit = doer.doTheThing()
    *       override def changeName(name: String): Foo = changer.changeName(name)
    *     }
    *   }
    * }
    * }}}
    */
  def compose[R](xs: Any*): R = macro impl.compose[R]

  def attenuate[R](capability: Any): R = macro impl.attenuate[R]

  def modulate[R](
    capability: Any,
    before: String => Unit,
    after: (String, Any) => Unit
  ): R = macro impl.modulate[R]

  def revocable[R](capability: Any): Revocable[R] = macro impl.revocable[R]


  /**
   * Instantiate a trait (or zero-parameter abstract class) by forwarding to the methods of another object.
   *
   * @param target the object to forward to. Must have methods matching the name and type of [[T]]'s abstract methods.
   * @tparam T the type of the trait or abstract class to implement
   * @return an instance of [[T]] with all abstract methods implemented by forwarding to `target`.
   */
  def forward[T](target: Any): T = macro impl.forward[T]

  private object impl {
    def compose[R: c.WeakTypeTag](
                                   c: blackbox.Context
                                 )(xs: c.Expr[Any]*): c.universe.Tree = {
      import c.universe._

      def members(tpe: c.universe.Type, input: c.Expr[Any]) = {
        val implementedMembers: Seq[c.universe.Tree] = tpe.members.collect {
          case member if member.isMethod && member.isAbstract =>
            val term = member.asTerm
            val termName = term.name.toTermName
            if (term.isVal) {
              q"override val $termName = $input.$termName"
            } else {
              val paramLists = member.asMethod.paramLists
              val paramDefs = paramLists.map {
                _.map { sym =>
                  q"val ${sym.name.toTermName}: ${sym.typeSignature}"
                }
              }
              val paramNames = paramLists.map {
                _.map {
                  _.name.toTermName
                }
              }
              q"override def $termName(...$paramDefs) = $input.$termName(...$paramNames)"
            }
        }.toSeq

        implementedMembers
      }

      val tpeR = implicitly[WeakTypeTag[R]].tpe
      val implementedMembers = xs.flatMap(el => members(el.actualType, el))
      val types = xs.map(_.actualType)

      val impl = q"new ..$types { ..$implementedMembers }"
      q"$impl: $tpeR"
    }

    def attenuate[R: c.WeakTypeTag](
                                     c: blackbox.Context
                                   )(capability: c.Expr[Any]): c.universe.Tree = {
      import c.universe._

      def members(tpe: c.universe.Type, input: c.Expr[Any]) = {
        val implementedMembers: Seq[c.universe.Tree] = tpe.members.collect {
          case member if member.isMethod && member.isAbstract =>
            val term = member.asTerm
            val termName = term.name.toTermName
            if (term.isVal) {
              q"override val $termName = $input.$termName"
            } else {
              val paramLists = member.asMethod.paramLists
              val paramDefs = paramLists.map {
                _.map { sym =>
                  q"val ${sym.name.toTermName}: ${sym.typeSignature}"
                }
              }
              val paramNames = paramLists.map {
                _.map {
                  _.name.toTermName
                }
              }
              q"override def $termName(...$paramDefs) = $input.$termName(...$paramNames)"
            }
        }.toSeq

        implementedMembers
      }

      val tpeR = implicitly[WeakTypeTag[R]].tpe
      val implementedMembers = members(tpeR, capability)
      val impl = q"new $tpeR { ..$implementedMembers }"
      q"$impl: $tpeR"
    }

    def modulate[R: c.WeakTypeTag](c: blackbox.Context)(
      capability: c.Expr[R],
      before: c.Expr[String => Unit],
      after: c.Expr[(String, Any) => Unit]
    ): c.universe.Tree = {
      import c.universe._

      // Have to untypecheck the external tree as it has a different owner
      // @see [[http://stackoverflow.com/questions/20936509/scala-macros-what-is-the-difference-between-typed-aka-typechecked-an-untyped]]
      // https://stackoverflow.com/questions/41968925/macro-untypecheck-required
      // apparently pattern matching code doesn't work very well here?
      val beforeDef = q"private val __before = ${c.untypecheck(before.tree)}"
      val afterDef = q"private val __after = ${c.untypecheck(after.tree)}"

      def members(tpe: c.universe.Type, input: c.Expr[Any]) = {
        val implementedMembers: Seq[c.universe.Tree] = tpe.members.collect {
          case member if member.isMethod && member.isAbstract =>
            val term = member.asTerm
            val termName = term.name.toTermName
            if (term.isVal) {
              q"override val $termName = $input.$termName"
            } else {
              val paramLists = member.asMethod.paramLists
              val paramDefs = paramLists.map {
                _.map { sym =>
                  q"val ${sym.name.toTermName}: ${sym.typeSignature}"
                }
              }
              val paramNames = paramLists.map {
                _.map {
                  _.name.toTermName
                }
              }
              val methodNameLiteral = q"${termName.decodedName.toString.trim}"
              val resultIdent: Ident = Ident(TermName("result"))
              val beforeIdent: Ident = Ident(TermName("__before"))
              val afterIdent: Ident = Ident(TermName("__after"))
              val beforeCall = q"$beforeIdent(...$methodNameLiteral)"
              val afterCall = q"$afterIdent($methodNameLiteral, $resultIdent)"
              q"override def $termName(...$paramDefs) = { $beforeCall; val $resultIdent = $input.$termName(...$paramNames); $afterCall; $resultIdent }"
            }
        }.toSeq

        implementedMembers
      }

      val tpeR = implicitly[WeakTypeTag[R]].tpe
      val implementedMembers
      : Seq[c.universe.Tree] = members(tpeR, capability) ++ Seq(
        beforeDef,
        afterDef
      )

      val impl = q"new $tpeR { ..$implementedMembers }"
      q"$impl: $tpeR"
    }

    def revocable[R: c.WeakTypeTag](
                                     c: blackbox.Context
                                   )(capability: c.Expr[Any]): c.universe.Tree = {
      import c.universe._

      val tpe = implicitly[c.WeakTypeTag[R]].tpe

      // Try to assert a pure trait...
      tpe.decls.foreach { decl =>
        assert(
          decl.isAbstract,
          s"Type must be abstract, but $decl has implementation!"
        )
      }

      val implementedMembers = tpe.members.collect {
        case member if member.isMethod && member.isAbstract =>
          val m = member.asMethod
          // Type parameters in the methods of the trait are hard, because a T is not a T in our world.
          // trait Reader {
          //   def bufferedReader[T](charset: Charset)(block: BufferedReader => T): Try[T]
          // }
          val termTypeParams =
            m.typeParams.map(t => q"type ${t.name.toTypeName}")
          //val mapped = termTypeParams.map(_.toString).map(name => {
          //  TypeDef(Modifiers(Flag.DEFERRED),TypeName(name), List(),TypeBoundsTree(EmptyTree, EmptyTree))
          //})

          val term = m.asTerm
          val termName = term.name.toTermName
          if (term.isVal) {
            q"override val $termName = thunk().$termName"
          } else {
            val paramLists = m.paramLists
            val paramDefs = paramLists.map {
              _.map { sym =>
                q"val ${sym.name.toTermName}: ${sym.typeSignature}"
              }
            }
            val paramNames = paramLists.map {
              _.map {
                _.name.toTermName
              }
            }
            q"override def $termName[..$termTypeParams](...$paramDefs) = thunk().$termName(...$paramNames)"
          }
      }.toSeq
      val revokerCapability = q"new $tpe { ..$implementedMembers }"
      // god awful hack, but it seems to resolve the type problems in termTypeParams
      val parsed = c.parse(showCode(revokerCapability))
      val args = List(q"$capability", q"{ thunk => $parsed }")
      q"""
         import ocaps.Revocable;
         Revocable[$tpe](...$args): Revocable[$tpe]
        """
    }

    // https://github.com/gmethvin/fastforward/blob/master/macros/src/main/scala/io/methvin/fastforward/package.scala
    def forward[T](c: blackbox.Context)(target: c.Expr[Any])(implicit tag: c.universe.WeakTypeTag[T]): c.universe.Tree = {
      import c.universe._
      val tpe = tag.tpe
      val implementedMembers: Seq[Tree] = tpe.members.collect {
        case member if member.isMethod && member.isAbstract =>
          val term = member.asTerm
          val termName = term.name.toTermName
          if (term.isVal) {
            q"override val $termName = $target.$termName"
          } else {
            val paramLists = member.asMethod.paramLists
            val paramDefs = paramLists.map {
              _.map { sym =>
                q"val ${sym.name.toTermName}: ${sym.typeSignature}"
              }
            }
            val paramNames = paramLists.map {
              _.map {
                _.name.toTermName
              }
            }
            q"override def $termName(...$paramDefs) = $target.$termName(...$paramNames)"
          }
      }.toSeq
      val impl = if (implementedMembers.isEmpty) {
        q"new $tpe { }"
      } else {
        q"new $tpe { ..$implementedMembers }"
      }
      q"$impl: $tpe"
    }
  }
}
