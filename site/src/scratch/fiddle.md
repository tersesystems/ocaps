# Scala Fiddle

```scalafiddle 
object Before {

  // #definition
  final class Document(private[this] var name: String) {
    def changeName(newName: String): Unit = name = newName
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")
    document.changeName("steve")
    println(s"result = $document")
  }
  // #usage
}
```


```scalafiddle
object AfterAttenuation {

  // #definition
  final class Document(private var name: String) extends Document.NameChanger {
    override def changeName(newName: String): Unit =  {
      name = newName
    }
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #access
  object Document {
    trait NameChanger {
      def changeName(name: String): Unit
    }
  }
  // #access

  // #usage
  def main(args: Array[String]): Unit = {
    val document = new Document("will")

    // expose a single facet through attenuation
    val nameChanger = new Document.NameChanger {
      override def changeName(name: String): Unit = document.changeName(name)
    }
    nameChanger.changeName("steve")
    println(s"result = $document")
  }
  // #usage
}
```



```scalafiddle
object AfterAmplification {

  // #definition
  final class Document(private var name: String) {
    private object capabilities {
      val nameChanger = new Document.NameChanger {
        override def changeName(newName: String): Unit =  {
          name = newName
        }
      }
    }
    override def toString: String = s"Document($name)"
  }
  // #definition

  // #access
  object Document {
    sealed trait NameChanger {
      def changeName(name: String): Unit
    }

    // Policy controls who has access to what
    class Access private {
      def nameChanger(doc: Document): NameChanger = {
        doc.capabilities.nameChanger
      }
    }

    object Access {
      def apply(): Access = new Access
    }
  }
  // #access

  // #usage
  def main(): Unit = {
    val document = new Document("will")
    val access = Document.Access()
    val nameChanger = access.nameChanger(document)
    nameChanger.changeName("steve")
    println(s"result = $document")
  }
  // #usage
}

AfterAmplification.main()
```
