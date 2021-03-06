package io.igl.jwt

import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import io.igl.jwt.claims._
import org.apache.commons.codec.binary.Base64
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.reflect.ClassTag
import scala.util.Try

/**
  * A class representing a decoded jwt.
  *
  * When an [[Alg]] value is omitted it defaults to none. Where multiple headers or claims with the same field name are
  * provided, the last occurrence is used.
  *
  * @param headers_ the values of the headers to be set
  * @param claims_  the values of the claims to be set
  */
class DecodedJwt(headers_ : Seq[HeaderValue], claims_ : Seq[ClaimValue]) extends Jwt {

  // Sort headers and claims so that if multiple duplicate types are provided, the last header/claim of said type is selected
  private val headers = withOutDuplicateOccurrence(headers_.reverse :+ Alg(Algorithm.NONE)).reverse
  private val claims = withOutDuplicateOccurrence(claims_.reverse).reverse

  private def withOutDuplicateOccurrence(values: Seq[JwtValue]): Seq[JwtValue] = {

    def withOutOccurrence(target: JwtField, values: Seq[JwtValue]): Seq[JwtValue] = values match {
      case Seq() => Seq()
      case v +: vs if v.field.name == target.name => withOutOccurrence(target, vs)
      case v +: vs => v +: withOutOccurrence(target, vs)
    }

    values match {
      case Seq() => Seq()
      case v +: vs => v +: withOutDuplicateOccurrence(withOutOccurrence(v.field, vs))
    }
  }

  override def getHeader[T <: HeaderValue : ClassTag]: Option[T] = headers.collectFirst {
    case header: T => header.asInstanceOf[T]
  }

  override def getClaim[T <: ClaimValue : ClassTag]: Option[T] = claims.collectFirst {
    case claim: T => claim.asInstanceOf[T]
  }

  private val algorithm = getHeader[Alg].map(_.value).get

  def encodedAndSigned(secret: String): String = {
    encodedAndSigned(secret.getBytes(UTF_8))
  }

  def encodedAndSigned(secret: Array[Byte]): String = {
    def jsAssign(value: JwtValue) = value.field.name -> value.jsValue

    val encodedHeader: String = DecodedJwt.encodeBase64Url(CompactPrinter(headers.map(jsAssign).toMap.toJson))
    val encodedPayload: String = DecodedJwt.encodeBase64Url(CompactPrinter(claims.map(jsAssign).toMap.toJson))
    val encodedHeaderAndPayload: String = encodedHeader ++ ('.' +: encodedPayload)

    encodedHeaderAndPayload ++ ('.' +: DecodedJwt.encodedSignature(encodedHeaderAndPayload, algorithm, secret))
  }

  private def canEqual(other: Any): Boolean = other.isInstanceOf[DecodedJwt]

  override def equals(other: Any): Boolean = other match {
    case that: DecodedJwt =>
      (that canEqual this) &&
        (headers == that.headers) &&
        (claims.sortBy(_.field.name) == that.claims.sortBy(_.field.name))
    case _ => false
  }

  override def hashCode(): Int = headers.hashCode() ^ claims.hashCode()

  override def toString: String =
    "DecodedJwt(" + headers.toString() + ", " + claims.toString() + ")"
}

object DecodedJwt {

  /**
    * Returns the Base64 decoded version of provided string
    */
  private def decodeBase64(subject: String, charset: String): String = new String(Base64.decodeBase64(subject), charset)

  /**
    * Returns the Base64 url safe encoding of a byte array
    */
  private def encodeBase64Url(subject: Array[Byte]): String = Base64.encodeBase64URLSafeString(subject)

  /**
    * Returns the Base64 url safe encoding of a string
    */
  private def encodeBase64Url(subject: String): String = encodeBase64Url(subject.getBytes("utf-8"))

  /**
    * Returns the signature of a jwt.
    *
    * @param encodedHeaderAndPayload the encoded header and payload of a jwt
    * @param algorithm               the algorithm to be used
    * @param secret                  the secret to sign with
    * @return a string representing the signature of a jwt
    */
  private def encodedSignature(encodedHeaderAndPayload: String, algorithm: Algorithm, secret: Array[Byte] = Array()): String = {
    import io.igl.jwt.Algorithm._

    def hmac(alg: Algorithm) = {
      val mac: Mac = Mac.getInstance(alg.toString)
      mac.init(new SecretKeySpec(secret, alg.toString))
      encodeBase64Url(mac.doFinal(encodedHeaderAndPayload.getBytes("utf-8")))
    }

    algorithm match {
      case HS256 => hmac(HS256)
      case HS384 => hmac(HS384)
      case HS512 => hmac(HS512)
      case NONE => ""
    }
  }

  private def constantTimeIsEqual(as: Array[Byte], bs: Array[Byte]): Boolean = {
    as.length == bs.length match {
      case true => (as zip bs).foldLeft(0) { (r, ab) => r + (ab._1 ^ ab._2) } == 0
      case _ => false
    }
  }

  def validateEncodedJwt(jwt: String,
                         key: String,
                         requiredAlg: Algorithm,
                         requiredHeaders: Seq[HeaderField],
                         requiredClaims: Seq[ClaimField],
                         ignoredHeaders: Seq[String] = Seq.empty[String],
                         ignoredClaims: Seq[String] = Seq.empty[String],
                         iss: Option[Iss] = None,
                         aud: Option[Aud] = None,
                         iat: Option[Iat] = None,
                         sub: Option[Sub] = None,
                         jti: Option[Jti] = None,
                         charset: String = "UTF-8",
                         now: Long = System.currentTimeMillis / 1000): Try[Jwt] = {
    validateEncodedJwtWithEncodedSecret(
      jwt,
      key.getBytes(UTF_8),
      requiredAlg,
      requiredHeaders,
      requiredClaims,
      ignoredHeaders,
      ignoredClaims,
      iss,
      aud,
      iat,
      sub,
      jti,
      charset)
  }

  /**
    * Attempts to construct a DecodedJwt from an encoded jwt.
    *
    * Any fields found in the jwt that are not in either the required set or the ignore set, will cause validation to fail.
    * Including an algorithm field in the requiredHeaders set is not needed, instead use the requiredAlg parameter.
    *
    * @param jwt             an encrypted jwt
    * @param key             the key to use when validating the signature
    * @param requiredAlg     the algorithm to require and use when validating the signature
    * @param requiredHeaders the headers the encrypted jwt is required to use
    * @param requiredClaims  the claims the encrypted jwt is required to use
    * @param ignoredHeaders  the headers to ignore should the encrypted jwt use them
    * @param ignoredClaims   the claims to ignore should the encrypted jwt use them
    * @param iss             used optionally, when you want to only validate a jwt where its required iss claim is equal to this
    * @param aud             used optionally, when you want to only validate a jwt where its required aud claim is equal to this
    * @param iat             used optionally, when you want to only validate a jwt where its required iat claim is equal to this
    * @param sub             used optionally, when you want to only validate a jwt where its required sub claim is equal to this
    * @param jti             used optionally, when you want to only validate a jwt where its required jti claim is equal to this
    * @return returns a [[DecodedJwt]] wrapped in Success when successful, otherwise Failure
    */
  def validateEncodedJwtWithEncodedSecret(jwt: String,
                                          key: Array[Byte],
                                          requiredAlg: Algorithm,
                                          requiredHeaders: Seq[HeaderField],
                                          requiredClaims: Seq[ClaimField],
                                          ignoredHeaders: Seq[String] = Seq.empty[String],
                                          ignoredClaims: Seq[String] = Seq.empty[String],
                                          iss: Option[Iss] = None,
                                          aud: Option[Aud] = None,
                                          iat: Option[Iat] = None,
                                          sub: Option[Sub] = None,
                                          jti: Option[Jti] = None,
                                          charset: String = "UTF-8",
                                          now: Long = System.currentTimeMillis / 1000): Try[Jwt] = Try {

    require(requiredHeaders.map(_.name).size == requiredHeaders.size, "Required headers contains field name collisions")
    require(requiredClaims.map(_.name).size == requiredClaims.size, "Required claims contains field name collisions")
    require(!requiredHeaders.contains(Alg), "Alg should not be included in the required headers")

    // Extract the various parts of a JWT
    val (header, payload, signature) = jwt.split('.') match {
      case Array(_header, _payload, _signature) =>
        (_header, _payload, _signature)
      case Array(_header, _payload) =>
        (_header, _payload, "")
      case _ =>
        throw new IllegalArgumentException("Jwt could not be split into a header, payload, and signature")
    }

    // Validate headers
    val headerJson = Try {
      decodeBase64(header, charset).parseJson match {
        case header: JsObject =>
          header
        case _ =>
          throw new IllegalArgumentException()
      }
    }.getOrElse(throw new IllegalArgumentException("Decoded header could not be parsed to a JSON object"))

    val headers = headerJson.fields.flatMap {
      case (Alg.name, value) => Alg.attemptApply(value).map {
        case alg if alg.value == requiredAlg =>
          alg
        case _ =>
          throw new IllegalArgumentException("Given jwt uses a different algorithm ")
      }.orElse(throw new IllegalArgumentException("Algorithm values did not match"))
      case (field, value) =>
        requiredHeaders.find(x => x.name == field) match {
          case Some(requiredHeader) => requiredHeader.attemptApply(value)
          case None =>
            ignoredHeaders.find(_ == field).
              getOrElse(throw new IllegalArgumentException("Found header that is in neither the required or ignored sets"))
            None
        }
    }

    if (headers.size != requiredHeaders.size + 1)
      throw new IllegalArgumentException("Provided jwt did not contain all required headers")

    // Validate payload
    val payloadJson = Try {
      decodeBase64(payload, charset).parseJson match {
        case payload: JsObject => payload
        case _ => throw new IllegalArgumentException()
      }
    }.getOrElse(throw new IllegalArgumentException("Decoded payload could not be parsed to a JSON object"))

    val claims = payloadJson.fields.flatMap { case (field, value) =>
      requiredClaims.find(x => x.name == field) match {
        case Some(requiredClaim) => requiredClaim.attemptApply(value).map {
          case exp: Exp =>
            now < exp.value match {
              case true => exp
              case false => throw new IllegalArgumentException("Jwt has expired")
            }
          case nbf: Nbf =>
            now > nbf.value match {
              case true => nbf
              case false => throw new IllegalArgumentException("Jwt is not yet valid")
            }
          case _iss: Iss =>
            validateEquals(iss, _iss, "Iss didn't match required iss")
          case _aud: Aud =>
            validateEquals(aud, _aud, "Aud didn't match required aud")
          case _iat: Iat =>
            validateEquals(iat, _iat, "Iat didn't match required iat")
          case _sub: Sub =>
            validateEquals(sub, _sub, "Sub didn't match required sub")
          case _jti: Jti =>
            validateEquals(jti, _jti, "Jti didn't match required jti")
          case claim =>
            claim
        }
        case None =>
          ignoredClaims.find(_ == field).
            getOrElse(throw new IllegalArgumentException("Found claim that is in neither the required or ignored sets"))
          None
      }
    }

    if (claims.size != requiredClaims.size)
      throw new IllegalArgumentException("Provided jwt did not contain all required claims")

    // Validate signature
    val correctSignature = encodedSignature(header + ('.' +: payload), requiredAlg, key)

    if (constantTimeIsEqual(signature.getBytes("utf-8"), correctSignature.getBytes("utf-8"))) {
      new DecodedJwt(headers.toSeq, claims.toSeq)
    } else {
      throw new IllegalArgumentException("Signature is incorrect")
    }
  }

  private[this] def validateEquals(that: Option[ClaimValue], other: ClaimValue, message: String) = {
    that
      .map(equals(other, message, _))
      .getOrElse(other)
  }

  private[this] def equals(other: ClaimValue, message: String, value: ClaimValue) = {
    if (value.equals(other)) {
      other
    } else {
      throw new IllegalArgumentException(message)
    }
  }
}
