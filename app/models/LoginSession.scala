package models

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import javax.inject.{Inject, Singleton}
import play.api.db.Database
import play.api.Configuration

case class LoginSession(user: User, expirationTime: Long) {
  val serialized = user.id.get.value + ";" + expirationTime
  def isExpired(now: Long = System.currentTimeMillis): Boolean = now > expirationTime
  def toLoginSessionString: (String, String) = LoginSession.LoginSessionKey -> serialized
  def isAdmin = user.isAdmin
  def isSuper = user.isSuper
}

object LoginSession {
  val LoginSessionKey: String = "login"
  val SessionTimeout: Duration = 6.hour

  def loginSessionString(user: User, now: Long = System.currentTimeMillis): (String, String) =
    renewExpirationTime(LoginSession(user, 0), now).toLoginSessionString

  def renewExpirationTime(session: LoginSession, now: Long = System.currentTimeMillis): LoginSession = session.copy(
    expirationTime = now + LoginSession.SessionTimeout.toMillis
  )
}

@Singleton
class LoginSessionRepo @Inject() (
  conf: Configuration,
  db: Database,
  userRepo: UserRepo
) {
  def retrieveLogin(requestHeader: RequestHeader): Option[LoginSession] =
    requestHeader.session.get(LoginSession.LoginSessionKey).flatMap(get)

  def retrieveLogin(result: Result)(implicit requestHeader: RequestHeader): Option[LoginSession] =
    result.session.get(LoginSession.LoginSessionKey).flatMap(get)

  def apply(sessionString: String): LoginSession = {
    val args = sessionString.split(';').map(_.toLong)
    db.withConnection { implicit conn =>
      LoginSession(userRepo.getByUserId(UserId(args(0))).get, args(1))
    }
  }

   def get(sessionString: String): Option[LoginSession] = {
     val args = sessionString.split(';').map(_.toLong)
     db.withConnection { implicit conn =>
       userRepo.getByUserId(UserId(args(0))).map { user =>
         LoginSession(user, args(1))
       }
     }
   }

   def fromRequest(
     request: RequestHeader, now: Long = System.currentTimeMillis
   ): Option[LoginSession] = {
     val login = request.session.get(LoginSession.LoginSessionKey)
     login.flatMap { sessionString =>
       get(sessionString).flatMap { login =>
         if (login.expirationTime < now) None else Some(login)
       }
     }
   }
}

