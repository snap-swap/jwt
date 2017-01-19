package io.igl.jwt

import spray.json.DefaultJsonProtocol._
import spray.json._

trait ClaimValue extends JwtValue {
  val field: ClaimField
}

trait ClaimField extends JwtField {
  def attemptApply(value: JsValue): Option[ClaimValue]
}

case class Iss(value: String) extends ClaimValue {
  override val field: ClaimField = Iss
  override val jsValue: JsValue = JsString(value)
}

object Iss extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsString(v) =>
        Some(apply(v))
      case _ =>
        None
    }
  }

  override val name = "iss"
}

case class Sub(value: String) extends ClaimValue {
  override val jsValue = JsString(value)
  override val field: ClaimField = Sub
}

object Sub extends ClaimField {
  override def attemptApply(value: JsValue): Option[Sub] = {
    value match {
      case JsString(v) =>
        Some(apply(v))
      case _ =>
        None
    }
  }


  override val name = "sub"
}

case class Aud(value: Either[String, Seq[String]]) extends ClaimValue {
  override val field: ClaimField = Aud
  override val jsValue: JsValue = value match {
    case Left(single) =>
      JsString(single)
    case Right(many) =>
      JsArray(many.map(JsString(_)): _*)
  }
}

object Aud extends ClaimField {
  def apply(value: String): Aud = Aud(Left(value))

  def apply(value: Seq[String]): Aud = Aud(Right(value))

  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsString(v) =>
        Some(Aud(Left(v)))
      case JsArray(v) =>
        Some(Aud(Right(v.map(_.convertTo[String]))))
      case _ =>
        None
    }
  }

  override val name = "aud"
}

case class Exp(value: Long) extends ClaimValue {
  override val field: ClaimField = Exp
  override val jsValue: JsValue = JsNumber(value)
}

case object Exp extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsNumber(v) =>
        Some(apply(v.toLong))
      case _ =>
        None
    }
  }

  override val name = "exp"
}

case class Nbf(value: Long) extends ClaimValue {
  override val field: ClaimField = Nbf
  override val jsValue: JsValue = JsNumber(value)
}

object Nbf extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsNumber(v) =>
        Some(apply(v.toLong))
      case _ =>
        None
    }
  }

  override val name = "nbf"
}

case class Iat(value: Long) extends ClaimValue {
  override val field: ClaimField = Iat
  override val jsValue: JsValue = JsNumber(value)
}

object Iat extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsNumber(v) =>
        Some(apply(v.toLong))
      case _ =>
        None
    }
  }

  override val name = "iat"
}

case class Jti(value: String) extends ClaimValue {
  override val field: ClaimField = Jti
  override val jsValue: JsValue = JsString(value)
}

object Jti extends ClaimField {
  override def attemptApply(value: JsValue): Option[ClaimValue] = {
    value match {
      case JsString(v) =>
        Some(apply(v))
      case _ =>
        None
    }
  }

  override val name = "jti"
}
