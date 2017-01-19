package io.igl.jwt

import spray.json._

trait HeaderValue extends JwtValue {
  val field: HeaderField
}

trait HeaderField extends JwtField {
  def attemptApply(value: JsValue): Option[HeaderValue]
}

case class Typ(value: String) extends HeaderValue {
  override val field: HeaderField = Typ
  override val jsValue: JsValue = JsString(value)
}

object Typ extends HeaderField {
  override def attemptApply(value: JsValue): Option[Typ] = {
    value match {
      case JsString(v) =>
        Some(apply(v))
      case _ =>
        None
    }
  }

  override val name = "typ"
}

case class Alg(value: Algorithm) extends HeaderValue {
  override val field: HeaderField = Alg
  override val jsValue: JsValue = JsString(value.name)
}

object Alg extends HeaderField {
  override def attemptApply(value: JsValue): Option[Alg] = {
    value match {
      case JsString(v) =>
        Algorithm.getAlgorithm(v).map(apply)
      case _ =>
        None
    }
  }

  override val name = "alg"
}

case object Cty extends HeaderField with HeaderValue {
  override def attemptApply(value: JsValue): Option[HeaderValue] = {
    value match {
      case JsString(_) =>
        Some(Cty)
      case _ =>
        None
    }
  }

  override val name = "cty"
  override val field: HeaderField = this
  override val value = "JWT"
  override val jsValue: JsValue = JsString(value)
}
