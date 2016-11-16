import akka.actor.Actor
import com.mongodb.spark.MongoSpark
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import com.mongodb.spark.sql._

case class CheckEmailDoesNotExistMessage(email: String)
case class CheckUserExistsMessage(email: String, password: String)
case class CreateUserMessage(email: String, password: String)
case class SetUserIdMessage(email: String, id: String)

case class User(email: String, password: String)
case class Id(email: String, id: String)

/**
  * Handles mongodb sessions and returns specific model data
  */
class Database extends Actor {

  private val conf = new SparkConf()
    .setAppName("test_task")
    .setMaster("local")
    .set("spark.mongodb.input.uri", "mongodb://127.0.0.1:27017/test_task.input")
    .set("spark.mongodb.output.uri", "mongodb://127.0.0.1:27017/test_task.output")
    .set("spark.mongodb.input.partitioner", "MongoPaginateByCountPartitioner")
    .set("spark.mongodb.input.partitionerOptions.MongoPaginateByCountPartitioner.numberOfPartitions", "1")

  private val userSession = SparkSession.builder().config(conf)
    .config("spark.mongodb.input.collection", "users")
    .config("spark.mongodb.output.collection", "users")
    .getOrCreate()

  private val idsSession = SparkSession.builder().config(conf)
    .config("spark.mongodb.input.collection", "ids")
    .config("spark.mongodb.output.collection", "ids")
    .getOrCreate()

  def receive = {
    case CheckEmailDoesNotExistMessage(email) =>
      sender() ! emailDoesNotExist(email)

    case CheckUserExistsMessage(email, password) =>
      sender() ! userExists(email, password)


    case CreateUserMessage(email, password) =>
      createUser(email, password)
      sender() ! self

    case SetUserIdMessage(email, id) =>
      setUserId(email, id)
  }

  private def emailDoesNotExist(email: String): Boolean = {
    val df = userSession.loadFromMongoDB().toDF()
    df.filter(df("email") equalTo email)
      .count() == 0
  }

  private def userExists(email: String, password: String) = {
    val df = userSession.loadFromMongoDB().toDF()
    df.filter(df("email").equalTo(email) and df("password").equalTo(password))
      .count() == 1
  }

  private def createUser(email: String, password: String) = {
    val rdd = userSession.sparkContext.parallelize(Seq(User(email, password)))
    writeAndSave(userSession.createDataFrame(rdd), "users")
  }

  private def setUserId(email: String, id: String) = {
    val df = idsSession.loadFromMongoDB().toDF()
    val emailExists = df.filter(df("email").equalTo(email)).count() == 1

    // If email already exists, we have to override its value.
    if (emailExists) {
      val filtered = df.filter(df("email").notEqual(email))
      writeAndSave(filtered, "ids", "overwrite")
    }

    val rdd = idsSession.sparkContext.parallelize(Seq(Id(email, id)))
    writeAndSave(idsSession.createDataFrame(rdd), "ids")
  }

  private def writeAndSave(dataFrame: DataFrame, collection: String, mode: String = "append") = {
    MongoSpark.write(dataFrame).option("collection", collection).mode(mode).save()
  }
}
