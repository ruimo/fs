package models

import anorm._
import anorm.SqlParser
import java.sql.Connection
import javax.inject._
import helpers.PasswordHash

case class UserId(value: Long) extends AnyVal

case class User(
  id: Option[UserId],
  name: String,
  email: String,
  passwordHash: Long,
  salt: Long,
  role: UserRole,
  deleted: Boolean
) {
  def isSuper: Boolean = role == UserRole.SUPER
  def isAdmin: Boolean = role == UserRole.ADMIN
}

@Singleton
class UserRepo @Inject() (
  passwordHash: PasswordHash
) {
  val AdminName = "administrator"

  val simple = {
    SqlParser.get[Option[Long]]("users.user_id") ~
    SqlParser.get[String]("users.user_name") ~
    SqlParser.get[String]("users.email") ~
    SqlParser.get[Long]("users.password_hash") ~
    SqlParser.get[Long]("users.salt") ~
    SqlParser.get[Int]("users.user_role") ~
    SqlParser.get[Boolean]("users.deleted") map {
      case id~name~email~passwordHash~salt~role~deleted => User(
        id.map(UserId.apply), name, email, passwordHash, salt, UserRole.byIndex(role), deleted
      )
    }
  }

  def create(
    name: String, email: String, passwordHash: Long, salt: Long, role: UserRole
  )(implicit conn: Connection): User = ExceptionMapper.mapException {
    SQL(
      """
      insert into users(
        user_id, user_name, email, password_hash, salt, user_role, deleted
      ) values (
        (select nextval('users_seq')),
        {name}, {email}, {passwordHash}, {salt}, {role}, false
      )
      """
    ).on(
      'name -> name,
      'email -> email,
      'passwordHash -> passwordHash,
      'salt -> salt,
      'role -> role.ordinal
    ).executeUpdate()

    val id = SQL("select currval('users_seq')").as(SqlParser.scalar[Long].single)
    User(
      Some(UserId(id)), name, email, passwordHash, salt, role, false
    )
  }

  def getByUserId(id: UserId)(implicit conn: Connection): Option[User] = SQL(
     "select * from users where user_id={id}"
   ).on(
     'id -> id.value
   ).as(
     simple.singleOpt
   )

  def list(
    page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderBy("user_name", Asc)
  )(
    implicit conn: Connection
  ): PagedRecords[User] = {
    import scala.language.postfixOps

    val offset: Int = pageSize * page
    val records: Seq[User] = SQL(
      s"select * from users where deleted = false order by $orderBy limit {pageSize} offset {offset}"
    ).on(
      'pageSize -> pageSize,
      'offset -> offset
    ).as(
      simple *
    )

    val count = SQL(
      "select count(*) from users where deleted = false"
    ).as(SqlParser.scalar[Long].single)
      
    PagedRecords(page, pageSize, (count + pageSize - 1) / pageSize, orderBy, records)
  }

  def count()(implicit conn: Connection): Long = SQL(
    "select count(*) from users"
  ).as(
    SqlParser.scalar[Long].single
  )

  def get(id: UserId)(implicit conn: Connection): Option[User] = SQL(
    "select * from users where user_id = {id}"
  ).on(
    'id -> id.value
  ).as(
    simple.singleOpt
  )

  def login(name: String, password: String)(implicit conn: Connection): Option[User] = SQL(
    "select * from users where user_name = {name}"
  ).on(
    'name -> name
  ).as(
    simple.singleOpt
  ).flatMap { rec =>
    if (passwordHash.generate(password, rec.salt) == rec.passwordHash) Some(rec) else None
  }

  def changePassword(
    id: UserId, currentPassword: String, newPassword: String
  )(
    implicit conn: Connection
  ): Boolean = {
    get(id) match {
      case None => false
      case Some(user) =>
        if (passwordHash.generate(currentPassword, user.salt) == user.passwordHash) {
          val (salt, hash) = passwordHash.generateWithSalt(newPassword)
          SQL(
            """
            update users set
              password_hash = {hash},
              salt = {salt}
            where user_id = {id}
            """
          ).on(
            'hash -> hash,
            'salt -> salt,
            'id -> user.id.get.value
          ).executeUpdate()
          true
        }
        else false
    }
  }
}
