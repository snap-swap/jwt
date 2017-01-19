package io.igl.jwt

/**
  * A representation of a jwt field.
  */
trait JwtField {
  /** The value to use of the field name **/
  val name: String
}