package object doobie
  extends Aliases
     with hi.Aliases
     with free.Aliases {

  object implicits
    extends free.Instances
       with syntax.AllSyntax

  @deprecated(message = "import doobie._, doobie.implicits._", since = "0.5.0")
  object imports
    extends Aliases
      with hi.Aliases
      with free.Aliases
      with free.Instances
      with syntax.AllSyntax

}
