package controllers

import play.modules.reactivemongo.MongoController
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json._

/**
 * Created by terry on 9/4/13.
 */
object UserController extends Controller with MongoController {

  def users = db.collection[JSONCollection]("users")

  def findByEmail(email : String) = Json.obj("email" -> email)


  def get(email : String) = Action {
    Async {
      // our query: { "email" : "foo@bar.com" }
      val query = Json.obj(
        "email" -> email
      )

      val removePasswordXForm = (__ \ 'password).json.prune

      // ask the collection for a distinct record, as a JsValue (see Play JSON lib)
      users.find(query).one[JsValue].map {
        maybeResults =>
           if (maybeResults.isDefined)
             Ok(maybeResults.get.transform(removePasswordXForm).get)
          else
             BadRequest(Json.obj("error" -> s"Ugh, can't find a user with email : ${email}"))
      }
    }
  }

  def userEmails = Action {
    Async {
      val query = Json.obj()
      val filter = Json.obj("email" -> 1, "_id" -> 0)

      users.find(query, filter).cursor[JsValue].toList.map {
        results =>
          Ok(Json.toJson(results.map(email => (email \ "email").as[String])))
      }
    }
  }

  def create = Action(parse.json) {
    request =>
      Async {
        users.save(request.body).map {
          lastError =>
            if (lastError.ok)
              Ok(Json.obj("results" -> "success"))
            else
              BadRequest(Json.obj("error" -> s"Error occurred: ${lastError.errMsg.getOrElse("No details, sadly...")}"))
        }
      }
  }

  def update(id : String) = Action(parse.json) {
    request =>
      Async {
        val removeIdTransform = (__ \ '_id).json.prune
        users.update(Json.obj("_id" -> Json.obj("$oid" -> id)), request.body.transform(removeIdTransform).get).map {
          lastError =>
            if (lastError.ok)
              Ok(Json.obj("results" -> "success"))
            else
              BadRequest(Json.obj("error" -> s"Error occurred: ${lastError.errMsg.getOrElse("No details.  Argh!")}"))

        }
      }
  }

  def remove(email : String) = Action {
    Async {
      users.remove(findByEmail(email)).map {
        lastError =>
          if (lastError.ok)
            Ok(Json.obj("results" -> "success"))
          else
            BadRequest(Json.obj("error" -> s"Error occurred: ${lastError.errMsg.getOrElse("No details.  Bummer.")}"))
      }
    }
  }

}
