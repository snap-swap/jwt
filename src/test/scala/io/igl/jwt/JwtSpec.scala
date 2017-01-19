package io.igl.jwt

import io.igl.jwt.claims._
import org.apache.commons.codec.binary.Base64
import org.scalatest.{Matchers, WordSpecLike}
import spray.json._

import scala.util.Success

class JwtSpec extends WordSpecLike with Matchers {

  val secret = "secret"

  def now: Long = System.currentTimeMillis / 1000

  "A DecodedJwt" should {
    "give the correct result when encrypted" in {
      val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ("JWT")), Seq(Sub("123456789")))
      val correctEncoding = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkifQ.qHdut1UR4-2FSAvh7U3YdeRR5r5boVqjIGQ16Ztp894"

      jwt.encodedAndSigned(secret) should be(correctEncoding)
    }

    "be equivalent to the same DecodedJwt after it has been encoded and decoded, given the same " +
      "secret was used and that the headers and claims previously set are demanded when decoding" in {
      val algorithm = Algorithm.HS256
      val requiredHeaders = Set[HeaderField](Typ)
      val requiredClaims = Set[ClaimField](Sub)
      val headers = Seq[HeaderValue](Typ("JWT"), Alg(algorithm))
      val claims = Seq[ClaimValue](Sub("1234567890"))

      val beforeJwt = new DecodedJwt(headers, claims)
      val afterJwt = DecodedJwt.validateEncodedJwt(
        beforeJwt.encodedAndSigned(secret),
        secret,
        algorithm,
        requiredHeaders,
        requiredClaims)

      afterJwt should be(Success(beforeJwt))
    }

    "not be created if a different secret is used when decoding an encoded jwt" in {
      val algorithm = Algorithm.HS256
      val requiredHeaders = Set[HeaderField](Typ)
      val requiredClaims = Set[ClaimField](Sub)
      val headers = Seq[HeaderValue](Typ("JWT"), Alg(algorithm))
      val claims = Seq[ClaimValue](Sub("1234567890"))

      val jwt = new DecodedJwt(headers, claims)

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret + secret,
        algorithm,
        requiredHeaders,
        requiredClaims).isFailure should be(true)
    }

    "use the last occurrence of a header/claim when multiple headers/claims of the same type are provided" in {
      val lastTyp = Typ("JWT")
      val lastSub = Sub("12345")
      new DecodedJwt(List(Typ("ASD"), lastTyp), Nil).getHeader[Typ] should be(Some(lastTyp))
      new DecodedJwt(Nil, Seq(Sub("asdf"), lastSub)).getClaim[Sub] should be(Some(lastSub))
    }

    "always have an algorithm header, even when one is not provided, in which case it should be set to \"none\"" in {
      new DecodedJwt(Nil, Nil).getHeader[Alg] should be(Some(Alg(Algorithm.NONE)))
      new DecodedJwt(Seq(Alg(Algorithm.HS256)), Nil).getHeader[Alg] should be(Some(Alg(Algorithm.HS256)))
    }

    "support the none algorithm" in {
      val alg = Alg(Algorithm.NONE)
      val jwt = new DecodedJwt(Seq(Typ("JWT")), Seq(Iss("foo")))
      val encoded = jwt.encodedAndSigned(secret)

      encoded should be("eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJpc3MiOiJmb28ifQ.")

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(Typ),
        Set(Iss)
      ) should be(Success(jwt))
    }

    "support the HS256 algorithm" in {
      val alg = Alg(Algorithm.HS256)
      val jwt = new DecodedJwt(Seq(alg, Typ("JWT")), Seq(Iss("foo")))
      val encoded = jwt.encodedAndSigned(secret)

      encoded should be("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJmb28ifQ.G1XNxLIxhWF4FFTI3TqZ6XIDorxNnx5J6kHe0jTb70s")

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(Typ),
        Set(Iss)
      ) should be(Success(jwt))
    }

    "support the HS384 algorithm" in {
      val alg = Alg(Algorithm.HS384)
      val jwt = new DecodedJwt(Seq(alg, Typ("JWT")), Seq(Iss("foo")))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(Typ),
        Set(Iss)
      ) should be(Success(jwt))
    }

    "support the HS512 algorithm" in {
      val alg = Alg(Algorithm.HS512)
      val jwt = new DecodedJwt(Seq(alg, Typ("JWT")), Seq(Iss("foo")))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(Typ),
        Set(Iss)
      ) should be(Success(jwt))
    }

    "give correct results when asked for various headers" in {
      val typ = Typ("JWT")
      val alg = Alg(Algorithm.HS256)
      val jwt = new DecodedJwt(Seq(typ, alg), Nil)

      jwt.getHeader[Typ] should be(Some(typ))
      jwt.getHeader[Alg] should be(Some(alg))
      jwt.getHeader[Cty.type] should be(None)
    }

    "give correct results when asked for various claims" in {
      val sub = Sub("foo")
      val iss = Iss("bar")
      val jwt = new DecodedJwt(Nil, Seq(sub, iss))

      jwt.getClaim[Sub] should be(Some(sub))
      jwt.getClaim[Iss] should be(Some(iss))
      jwt.getClaim[Exp] should be(None)
    }

    "not be created from an encoded jwt where the required headers contains the algorithm field" in {
      DecodedJwt.validateEncodedJwt("", secret, Algorithm.NONE, Set(Alg), Set()).isFailure should be(true)
    }

    "not be created from an encoded jwt with fields we don't recognise as being either required or ignored" in {
      val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ("JWT")), Seq(Iss("hindley")))
      val encoded = jwt.encodedAndSigned(secret)
      DecodedJwt.validateEncodedJwt(encoded, secret, Algorithm.HS256, Set(), Set(Iss)).isFailure should be(true)
      DecodedJwt.validateEncodedJwt(encoded, secret, Algorithm.HS256, Set(Typ), Set()).isFailure should be(true)
    }

    "be able to be created from an encoded jwt where we are ignoring some fields" in {
      val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ("JWT")), Seq(Iss("hindley")))
      val jwtIgnoringIss = new DecodedJwt(Seq(jwt.getHeader[Alg].get, jwt.getHeader[Typ].get), Nil)
      val jwtIgnoringTyp = new DecodedJwt(Seq(jwt.getHeader[Alg].get), Seq(jwt.getClaim[Iss].get))
      val encoded = jwt.encodedAndSigned(secret)
      DecodedJwt.validateEncodedJwt(encoded, secret, Algorithm.HS256, Set(Typ), Set(), Set(), Set(Iss.name)) should be(Success(jwtIgnoringIss))
      DecodedJwt.validateEncodedJwt(encoded, secret, Algorithm.HS256, Set(), Set(Iss), Set(Typ.name)) should be(Success(jwtIgnoringTyp))
    }

    "not be created from an encoded jwt where the algorithms do not match" in {
      val typ = Typ("JWT")
      val iss = Iss("hindley")
      val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), typ), Seq(iss))
      val encoded = jwt.encodedAndSigned(secret)
      DecodedJwt.validateEncodedJwt(encoded, secret, Algorithm.NONE, Set(typ.field), Set(iss.field)).isFailure should be(true)
    }

    "support all registered headers" in {
      val typ = Typ("JWT")
      val alg = Alg(Algorithm.HS256)
      val cty = Cty
      val headers = Seq(typ, alg, cty)

      val jwt = new DecodedJwt(headers, Seq())
      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        alg.value,
        Set(Typ, Cty),
        Set()) should be(Success(jwt))
    }

    "support all registered claims" in {
      import ClaimSeq._

      val alg = Alg(Algorithm.HS256)
      val iss = Iss("hindley")
      val sub = Sub("123456789")
      val audSingle = Aud("users")
      val audMany = Aud(Seq("admin", "users"))
      val exp = Exp(now + 100)
      val nbf = Nbf(now - 100)
      val iat = Iat(1234567890L)
      val jti = Jti("asdf1234")
      val claimsA = iss and sub and audSingle and exp and nbf and iat and jti
      val jwtA = new DecodedJwt(Seq(alg), claimsA)

      DecodedJwt.validateEncodedJwt(
        jwtA.encodedAndSigned(secret),
        secret,
        alg.value,
        Set(),
        claimsA.map(_.field).toSet) should be(Success(jwtA))

      val claimsB = iss and sub and audMany and exp and nbf and iat and jti
      val jwtB = new DecodedJwt(Seq(alg), claimsB)

      DecodedJwt.validateEncodedJwt(
        jwtB.encodedAndSigned(secret),
        secret,
        alg.value,
        Set(),
        claimsB.map(_.field).toSet) should be(Success(jwtB))
    }

    "not be created from an expired jwt" in {
      val jwt = new DecodedJwt(Seq(), Seq(Exp(now - 100)))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        Algorithm.NONE,
        Set(),
        Set(Exp)
      ).isFailure should be(true)
    }

    "be able to ignore the exp claim" in {
      val jwt = new DecodedJwt(Seq(Typ("JWT")), Seq(Exp(now - 100)))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        Algorithm.NONE,
        Set(Typ),
        Set(),
        Set(),
        Set(Exp.name)
      ) should be(Success(new DecodedJwt(Seq(Typ("JWT")), Seq())))
    }

    "not be created from a not yet valid jwt" in {
      val jwt = new DecodedJwt(Seq(), Seq(Nbf(now + 100)))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        Algorithm.NONE,
        Set(),
        Set(Nbf)
      ).isFailure should be(true)
    }

    "be able to ignore the nbf claim" in {
      val jwt = new DecodedJwt(Seq(Typ("JWT")), Seq(Nbf(now + 100)))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        Algorithm.NONE,
        Set(Typ),
        Set(),
        Set(),
        Set(Nbf.name)
      ) should be(Success(new DecodedJwt(Seq(Typ("JWT")), Seq())))
    }

    "support private unregistered fields" in {
      object Uid extends ClaimField {
        override protected def attemptApply: JsonToClaim = {
          case JsNumber(v) =>
            apply(v.toLong)
        }

        override val name: String = "uid"
      }

      case class Uid(value: Long) extends ClaimValue {
        override val field: ClaimField = Uid
        override val jsValue: JsValue = JsNumber(value)
      }

      val alg = Alg(Algorithm.HS256)
      val uid = Uid(123456789L)
      val jwt = new DecodedJwt(Seq(alg), Seq(uid))

      jwt.getClaim[Uid] should be(Some(uid))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        alg.value,
        Set(),
        Set(Uid)) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        jwt.encodedAndSigned(secret),
        secret,
        alg.value,
        Set(),
        Set(Uid)) should be(Success(jwt))
    }

    "check if a specific iss claim is required when creating from an encoded jwt" in {
      val alg = Alg(Algorithm.HS256)
      val iss = Iss("jeff")
      val jwt = new DecodedJwt(Seq(alg), Seq(iss))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Iss),
        iss = Some(iss)
      ) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Iss),
        iss = Some(Iss(iss.value + "a"))
      ).isFailure should be(true)
    }

    "check if a specific aud claim is required when creating from an encoded jwt" in {
      val alg = Alg(Algorithm.HS256)
      val aud = Aud("jeff")
      val jwt = new DecodedJwt(Seq(alg), Seq(aud))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Aud),
        aud = Some(aud)
      ) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Aud),
        aud = Some(Aud(aud.value.left + "a"))
      ).isFailure should be(true)
    }

    "check if a specific iat claim is required when creating from an encoded jwt" in {
      val alg = Alg(Algorithm.HS256)
      val iat = Iat(1234567890L)
      val jwt = new DecodedJwt(Seq(alg), Seq(iat))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Iat),
        iat = Some(iat)
      ) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Iat),
        iat = Some(Iat(iat.value + 1))
      ).isFailure should be(true)
    }

    "check if a specific sub claim is required when creating from an encoded jwt" in {
      val alg = Alg(Algorithm.HS256)
      val sub = Sub("jeff")
      val jwt = new DecodedJwt(Seq(alg), Seq(sub))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Sub),
        sub = Some(sub)
      ) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Sub),
        sub = Some(Sub(sub.value + "a"))
      ).isFailure should be(true)
    }

    "check if a specific jti claim is required when creating from an encoded jwt" in {
      val alg = Alg(Algorithm.HS256)
      val jti = Jti("asdf")
      val jwt = new DecodedJwt(Seq(alg), Seq(jti))
      val encoded = jwt.encodedAndSigned(secret)

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Jti),
        jti = Some(jti)
      ) should be(Success(jwt))

      DecodedJwt.validateEncodedJwt(
        encoded,
        secret,
        alg.value,
        Set(),
        Set(Jti),
        jti = Some(Jti(jti.value + "a"))
      ).isFailure should be(true)
    }

    "support Base64 Encoded Secret" in {
      val decoder = new Base64(true)
      val alg = Alg(Algorithm.HS256)
      val jwt = new DecodedJwt(Seq(alg, Typ("JWT")), Seq(Iss("foo")))
      val decodedSecret: Array[Byte] = decoder.decode(secret)
      val encoded = jwt.encodedAndSigned(decodedSecret)

      encoded should be("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJmb28ifQ.M-3mD1aZMseTJW_lnV2_YKuMXcMKIBVevaSYLU4P3zE")

      DecodedJwt.validateEncodedJwtWithEncodedSecret(
        encoded,
        decodedSecret,
        alg.value,
        Set(Typ),
        Set(Iss)
      ) should be(Success(jwt))
    }
  }
}