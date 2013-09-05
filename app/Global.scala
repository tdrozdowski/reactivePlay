import play.api.mvc.{WithFilters, Filter, RequestHeader, Result}
import play.api.Play
import play.api.Play.current
/**
 * Created by terry on 9/4/13.
 */

object Global extends WithFilters(Cors)

object Cors extends Filter {

  lazy val config = Play.configuration
  lazy private val allowedHost = config.getString("auth.cors.host").getOrElse("http://localhost:8000")


  override def apply(next : RequestHeader => Result)(request : RequestHeader) : Result = {
    val result = next(request)
    result.withHeaders("Access-Control-Allow-Origin" -> allowedHost)
  }

}