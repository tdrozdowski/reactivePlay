package controllers

import play.api.Play.current
import play.api.mvc.{WebSocket, Controller}
import play.modules.reactivemongo.{ReactiveMongoPlugin, MongoController}
import scala.concurrent.Future
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Logger
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.json.{Json, JsValue}
import reactivemongo.api.{Cursor, QueryOpts}

/**
 * Created by terry on 9/4/13.
 */
object ComplaintController extends Controller with MongoController {

  // let's be sure that the collections exists and is capped
  val futureCollection: Future[JSONCollection] = {
    val collection = db.collection[JSONCollection]("complaints")
    collection.stats().flatMap {
      case stats if !stats.capped =>
        // the collection is not capped, so we convert it
        Logger.debug("converting to capped")
        collection.convertToCapped(1024 * 1024, None)
      case _ => Future(collection)
    }.recover {
      // the collection does not exist, so we create it
      case _ =>
        Logger.debug("creating capped collection...")
        collection.createCapped(1024 * 1024, None)
    }.map { _ =>
      Logger.debug("the capped collection is available")
      collection
    }
  }

  def watchCollection = WebSocket.using[JsValue] { request =>
  // Inserts the received messages into the capped collection
    val in = Iteratee.flatten(futureCollection.map(collection => Iteratee.foreach[JsValue] { json =>
      Logger.debug("received " + json)
      collection.insert(json)
    }))

    // Enumerates the capped collection
    val out = {
      val futureEnumerator = futureCollection.map { collection =>
      // so we are sure that the collection exists and is a capped one
        val cursor: Cursor[JsValue] = collection
          // we want all the documents
          .find(Json.obj())
          // the cursor must be tailable and await data
          .options(QueryOpts().tailable.awaitData)
          .cursor[JsValue]

        // ok, let's enumerate it
        cursor.enumerate
      }
      Enumerator.flatten(futureEnumerator)
    }

    // We're done!
    (in, out)
  }
}
