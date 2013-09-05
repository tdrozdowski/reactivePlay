package controllers

import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController

import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.gridfs._
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.bson.DefaultBSONHandlers._

import scala.concurrent.Future

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats

import play.api.libs.json.{JsValue, Json}

/**
 * Created by terry on 9/4/13.
 */
object GridFSController extends Controller with MongoController {

  def users = db.collection[JSONCollection]("users")
  val gridFS = new reactivemongo.api.gridfs.GridFS(db, "profilePics")

  def uploadProfilePic(email : String) = Action(gridFSBodyParser(gridFS)) {
    request =>
      val futureUpload = for {
        profileResults <- request.body.files.head.ref
        result <- users.update(Json.obj("email" -> email), Json.obj("$set" -> Json.obj("profilePic" -> Json.obj("_id" -> BSONFormats.toJSON(profileResults.id)))))
      } yield result

      Async {
        futureUpload.map {
          case _ =>
            Ok(Json.obj("results" -> "success"))
        }.recover {
          case e =>
            BadRequest(Json.obj("results" -> "error", "details" -> s"There was a problem with your request: ${e.getMessage}"))
        }
      }
  }

  def getProfilePic(email : String) = Action {
    Async {
       users.find(Json.obj("email" -> email)).one[JsValue].filter(_.isDefined).flatMap {
          maybeUser =>
            val profileId = (maybeUser.get \ "profilePic" \ "_id" \ "$oid").as[String]
            serve(gridFS, gridFS.find(BSONDocument("_id" -> new BSONObjectID(profileId))))
       }
    }

  }

}
