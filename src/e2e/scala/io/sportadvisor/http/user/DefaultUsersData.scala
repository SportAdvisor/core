package io.sportadvisor.http.user

import java.sql.DriverManager

import com.roundeights.hasher.Implicits._
import io.sportadvisor.core.user.UserModels.UserData
import io.sportadvisor.{BaseE2ETest, Preconditions}

/**
  * @author sss3 (Vladimir Alekseev)
  */
trait DefaultUsersData extends Preconditions { self: BaseE2ETest =>

  protected var user: UserData = UserData(1L, "", "", "", Some(""))

  override def setup(): Unit = {
    Class.forName(pgContainer.driverClassName)
    val password = "str0nGpass"
    val email = "vladimir@sportadvisor.io"
    val name = "Vladimir"
    val lang = "en"
    val connection =
      DriverManager.getConnection(pgContainer.jdbcUrl, pgContainer.username, pgContainer.password)
    val insertSql =
      s"""insert into public."ACCOUNTS" (name, email, password, language) VALUES('$name', '$email', '${password.sha256.hex}'
         |, '$lang') """.stripMargin
    connection.prepareStatement(insertSql).execute()
    val rs = connection
      .prepareStatement(s"""select id from public."ACCOUNTS" where email = '$email'""")
      .executeQuery()
    rs.next()
    val id = rs.getLong(1)

    connection.close()
    user = UserData(id, email, password, name, Some(lang))
    super.setup()
  }

}
