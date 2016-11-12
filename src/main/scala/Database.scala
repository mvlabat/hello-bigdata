import com.mongodb.spark.MongoSpark
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SparkSession}
import com.mongodb.spark.sql._
import org.apache.spark.sql.functions._

/**
  * Handles mongodb sessions and returns specific model data
  */
class Database {

  case class User(email: String, password: String)
  case class Id(email: String, id: String)

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

  def emailDoesNotExist(email: String): Boolean = {
    val df = userSession.loadFromMongoDB().toDF()
    df.filter(df("email") equalTo email)
      .count() == 0
  }

  def userExists(email: String, password: String) = {
    val df = userSession.loadFromMongoDB().toDF()
    df.filter(df("email").equalTo(email) and df("password").equalTo(password))
      .count() == 1
  }

  def createUser(email: String, password: String) = {
    val rdd = userSession.sparkContext.parallelize(Seq(User(email, password)))
    userSession.createDataFrame(rdd).saveToMongoDB()
  }

  def setUserId(email: String, id: String) = {
    val df = userSession.loadFromMongoDB().toDF()
    val emailExists = df.filter(df("email").equalTo(email)).count() == 1

    // If email already exists, we have to override its value.
    if (emailExists) {
      // TODO: for some reason this line truncates "users" collection
      // MongoSpark.write(df.filter(df("email").notEqual(email))).mode("overwrite").save()
    }

    val rdd = idsSession.sparkContext.parallelize(Seq(Id(email, id)))
    idsSession.createDataFrame(rdd).saveToMongoDB()
  }
}
