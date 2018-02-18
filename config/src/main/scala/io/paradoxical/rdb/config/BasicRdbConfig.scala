package io.paradoxical.rdb.config

import io.paradoxical.rdb.config.JDBCProps._
import java.net.URI
import java.util.Properties

case class JvmKeyStoreConfig(
  trust_cert_store_location: URI,
  trust_cert_store_password: String,
  client_cert_store_location: URI,
  client_cert_store_password: String
)

/**
 * WARNING: Please do not ever use this class directly, all connections should be done via a connection pool.
 *
 * See hikari
 *
 * @param url
 * @param credentials
 * @param security
 */
case class BasicRdbConfig(
  url: String,
  credentials: RdbCredentials,
  security: RdbSecurityConfig
)

case class RdbCredentials(
  user: String,
  password: String
) {
  def toJdbcProps: Properties = {
    val props = new Properties()
    props.setProperty(USER, user)
    props.setProperty(PASSWORD, password)
    props
  }
}

case class RdbSecurityConfig(
  key_store_config: JvmKeyStoreConfig,
  use_ssl: Boolean = true,
  require_ssl: Boolean = true,
  verify_server_certificate: Boolean = true
) {
  def toJdbcProps: Properties = {
    val props = new Properties()
    props.setProperty(USE_SSL, use_ssl.toString)
    props.setProperty(REQUIRE_SSL, require_ssl.toString)
    props.setProperty(VERIFY_SERVER_CERT, verify_server_certificate.toString)
    props.setProperty(TRUST_CERTIFICATE_KEY_STORE_URL, key_store_config.trust_cert_store_location.toString)
    props.setProperty(TRUST_CERTIFICATE_KEY_STORE_PASSWORD, key_store_config.trust_cert_store_password.toString)
    props.setProperty(CLIENT_CERTIFICATE_KEY_STORE_URL, key_store_config.client_cert_store_location.toString)
    props.setProperty(CLIENT_CERTIFICATE_KEY_STORE_PASSWORD, key_store_config.client_cert_store_password.toString)
    props
  }
}
