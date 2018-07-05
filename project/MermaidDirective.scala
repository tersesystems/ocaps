import com.lightbend.paradox.markdown.{ContainerBlockDirective, Writer}
import org.pegdown.Printer
import org.pegdown.ast._

object MermaidDirective extends (Writer.Context => MermaidDirective) {
  def apply(context: Writer.Context): MermaidDirective = MermaidDirective(context.properties)
}

case class MermaidDirective(variables: Map[String, String]) extends ContainerBlockDirective("mermaid") {
  import scala.collection.JavaConverters._

  def render(node: DirectiveNode, visitor: Visitor, printer: Printer): Unit = {
    def printTree(parent: Node): Unit = {
      parent.getChildren.asScala.headOption match {
        case Some(verbatim: VerbatimNode) =>
          printer.println.print(verbatim.getText)
        case Some(other) =>
          throw new IllegalStateException(s"Unknown node $other")
        case None =>
          throw new IllegalStateException(s"No node found!")
      }
    }
    printer.print(s"""<div class="mermaid">""")
    printTree(node.contentsNode)
    printer.print("""</div>""")
  }

}