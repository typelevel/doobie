package object doobie
  extends Aliases
     with doobie.hi.Modules
     with doobie.free.Modules
     with doobie.free.Types {

  object implicits
    extends free.Instances
       with syntax.AllSyntax

  @deprecated(message = "import doobie._, doobie.implicits._", since = "0.5.0")
  object imports
    extends Aliases
      with hi.Modules
      with free.Modules
      with free.Types
      with free.Instances
      with syntax.AllSyntax

}
