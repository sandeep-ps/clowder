package api

import javax.inject.Inject

import com.wordnik.swagger.annotations.ApiOperation
import models.{UUID, User}
import play.api.libs.json._
import play.api.mvc.Action
import services.UserService
import java.util.Date

import services.mongodb.MongoDBEventService
import models.MiniUser
import models.Event

/**
 * API to interact with the users.
 *
 * @author Rob Kooper
 */
class Users @Inject()(users: UserService, events: MongoDBEventService) extends ApiController {
  /**
   * Returns a list of all users in the system.
   */
  @ApiOperation(value = "List all users in the system",
    responseClass = "User", httpMethod = "GET")
  def list() = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.UserAdmin)) { request =>
    Ok(Json.toJson(users.list))
  }

  /**
   * Returns a single user based on the id specified.
   */
  @ApiOperation(value = "Return a single user.",
    responseClass = "User", httpMethod = "GET")
  def findById(id: UUID) = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.GetUser)) { request =>
    users.findById(id) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => BadRequest("no user found with that id.")
    }
  }

  /**
   * Returns a single user based on the email specified.
   */
  @ApiOperation(value = "Return a single user.",
    responseClass = "User", httpMethod = "GET")
  def findByEmail(email: String) = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.GetUser)) { request =>
    users.findByEmail(email) match {
      case Some(x) => Ok(Json.toJson(x))
      case None => BadRequest("no user found with that email.")
    }
  }

  @ApiOperation(value = "Edit User Field.",
    responseClass = "None", httpMethod = "POST")
  def updateUserField(email: String, field: String, fieldText: Any) = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.GetUser)) { request =>
    users.updateUserField(email, field, fieldText)
    Ok(Json.obj("status" -> "success"))
  }

  @ApiOperation(value = "Add a dataset View.",
    responseClass = "None", httpMethod = "POST")
  def addUserDatasetView(email: String, dataset: UUID)= SecuredAction(parse.anyContent, authorization = WithPermission(Permission.GetUser)) { request =>
    users.addUserDatasetView(email, dataset)
    Ok(Json.obj("status" -> "success"))
  }

  @ApiOperation(value = "Create a List.",
    responseClass = "None", httpMethod = "POST")
  def createNewListInUser(email: String, field: String, fieldList: List[Any])= SecuredAction(parse.anyContent, authorization = WithPermission(Permission.GetUser)) { request =>
    users.createNewListInUser(email, field, fieldList)
    Ok(Json.obj("status" -> "success"))
  }

  @ApiOperation(value = "Follow a user",
    responseClass = "None", httpMethod = "POST")
  def follow(followeeUUID: UUID, name: String) = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.LoggedIn)) { request =>
    implicit val user = request.mediciUser
    user match {
      case Some(loggedInUser) => {
        val followerUUID = loggedInUser.id
        var mini_user = new MiniUser(id = loggedInUser.id, fullName = loggedInUser.fullName, avatarURL = loggedInUser.getAvatarUrl)
        var new_event = new Event(user = mini_user, object_id = Option(followeeUUID), object_name = Option(name), source_id = None, source_name = None, event_type = "follow_user", created=new Date())
        events.addEvent(new_event)
        users.followUser(followeeUUID, followerUUID)
        Ok(Json.obj("status" -> "success"))
      }
      case None => {
        Ok(Json.obj("status" -> "fail"))
      }
    }
  }

  @ApiOperation(value = "Unfollow a user",
    responseClass = "None", httpMethod = "POST")
  def unfollow(followeeUUID: UUID, name: String) = SecuredAction(parse.anyContent, authorization = WithPermission(Permission.LoggedIn)) { request =>
    implicit val user = request.mediciUser
    user match {
      case Some(loggedInUser) => {
        val followerUUID = loggedInUser.id
        var mini_user = new MiniUser(id = loggedInUser.id, fullName = loggedInUser.fullName, avatarURL = loggedInUser.getAvatarUrl)
        var new_event = new Event(user = mini_user, object_id = Option(followeeUUID), object_name = Option(name), source_id = None, source_name = None, event_type = "unfollow_user", created=new Date())
        events.addEvent(new_event)
        users.unfollowUser(followeeUUID, followerUUID)
        Ok(Json.obj("status" -> "success"))
      }
      case None => {
        Ok(Json.obj("status" -> "fail"))
      }
    }
  }
}
