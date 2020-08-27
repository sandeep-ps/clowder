package services.mongodb

import java.util.Date

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import javax.inject.Inject
import models._
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import salat.dao.{ModelCompanion, SalatDAO}
import services.mongodb.MongoContext.context
import services.{DI, ExtractionRequestsService, ExtractionService}

/**
 * MongoDB implementation for ExtractionRequestsService to keep track of extraction requests
 * 
 */

class MongoDBExtractionRequestsService @Inject()(extractions: ExtractionService)extends ExtractionRequestsService {


  def getDTSRequests(): List[ExtractionRequests] = {
    var list_requests = List[ExtractionRequests]()
    val allRequests = ExtractionRequests.dao.collection.find()

    for (r <- allRequests) {
      var req = ExtractionRequests.toObject(r)

      list_requests = req :: list_requests
    }
    list_requests
  }

  def insertRequest(serverIP: String, clientIP: String, filename: String, fileid: UUID, fileType: String, filesize: Long, uploadDate: Date) = {
    ExtractionRequests.insert(new ExtractionRequests(serverIP, clientIP, fileid, filename, fileType, filesize, uploadDate, None, None, None))
   }


  
  def updateRequest(file_id:UUID,extractor_id:String) = {
    val requests = getRequests(file_id)
    Logger.debug("-----Updating Extraction Request----------")
    Logger.debug( "Number of Requests to be updated : "+requests.length);
    for (r <- requests) {

      var req = Json.parse(com.mongodb.util.JSON.serialize(r))

      var fileid1 = (req \ ("fileid")).toString()
      var fileid = fileid1.substring(1, fileid1.size - 1)

      Logger.debug("File Id for UPDATE MongoDB Extraction requests: " + fileid)

      var extime=extractions.getExtractionTime(file_id)
     
      var sortedTime = extime.sortBy(_.getTime())
      var len = sortedTime.size

      var ex = extractions.getExtractorList(file_id)
     
      val elist = ex.keySet.toList
      Logger.debug("----extractorlist:----"+ elist);
      if (len != 0) {
        var update = $set("extractors" -> elist, "startTime" -> sortedTime(0), "endTime" -> sortedTime(len - 1))
        var result = ExtractionRequests.dao.collection.update(r, update)
       
      }

    }
  }

  
  def getRequests(file_id:UUID) = {
    Logger.debug("GET Requests---- fileID"+ file_id.toString)
    //val query = MongoDBObject("endTime" -> None)
    val id=file_id.toString
    val query = MongoDBObject("fileid"->new ObjectId(file_id.stringify))
    var requests1=ExtractionRequests.find(query)
    Logger.debug("REQUEST Length: "+ requests1.length)
    
    var requests = ExtractionRequests.dao.collection.find(query)
    requests
  }

}

object ExtractionRequests extends ModelCompanion[ExtractionRequests, ObjectId] {
  // TODO RK handle exception for instance if we switch to other DB
  val COLLECTION = "dtsrequests"
  val mongos: MongoStartup = DI.injector.getInstance(classOf[MongoStartup])
  val dao = new SalatDAO[ExtractionRequests, ObjectId](collection = mongos.collection(COLLECTION)) {}

}  
