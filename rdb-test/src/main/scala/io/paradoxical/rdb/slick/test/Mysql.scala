package io.paradoxical.rdb.slick.test

import io.paradoxical.v2.DockerCreator
import io.paradoxical.{DockerClientConfig, EnvironmentVar}
import java.sql.{Connection, DriverManager}
import org.joda.time.DateTime
import scala.collection.JavaConverters._

case class Container(host: String, port: Int, private val dockerContainer: io.paradoxical.v2.Container) {
  def close() = dockerContainer.close()
}

object Mysql {
  def docker(tag: String = "5.7"): MysqlDocker = {
    val container =
      DockerCreator.build(
        DockerClientConfig.
          builder.
          pullAlways(true).
          imageName(s"mysql:${tag}").
          envVars(List(
            new EnvironmentVar("MYSQL_ALLOW_EMPTY_PASSWORD", "true")
          ).asJava).
          port(3306).
          build
      )

    val port = container.getTargetPortToHostPortLookup.get(3306)

    val docker = new MysqlDocker(Container(container.getDockerHost, port, container))

    val expiration = DateTime.now().plusSeconds(60)

    while (DateTime.now().isBefore(expiration) && !docker.isOpen) {
      Thread.sleep(100)
    }

    docker
  }
}

class MysqlDocker(val container: Container) {
  Class.forName("com.mysql.jdbc.Driver")

  def close() = container.close()

  def user: String = "root"

  def password: String = ""

  def isOpen: Boolean = {
    try {
      connect(_.close())

      true
    } catch {
      case e: Exception =>
        false
    }
  }

  def jdbc(db: String = ""): String = {
    s"${url(db)}?user=$user&useSSL=false"
  }

  def url(db: String = ""): String = {
    s"jdbc:mysql://${container.host}:${container.port}/${db}"
  }

  def createDatabase(db: String, charset: String = "utf8mb4", collation: String = "utf8mb4_unicode_ci"): String = {
    connect { conn =>
      conn.createStatement().execute(makeDatabaseString(db, charset, collation))
    }

    jdbc(db)
  }

  private def makeDatabaseString(db: String, charset: String, collation: String): String = {
    val createDbString = s"CREATE DATABASE $db"
    val charsetString = s"DEFAULT CHARACTER SET $charset"
    val collationString = s"DEFAULT COLLATE $collation"

    s"$createDbString $charsetString $collationString"
  }

  def dropDatabase(db: String): String = {
    connect { conn =>
      conn.createStatement().execute(s"drop database if exists ${db}")
    }

    jdbc(db)
  }

  private def connect[T](block: Connection => T): T = {
    block(DriverManager.getConnection(jdbc()))
  }

  def connectWith[T](jdbc: String)(block: Connection => T): T = {
    block(DriverManager.getConnection(jdbc))
  }
}
