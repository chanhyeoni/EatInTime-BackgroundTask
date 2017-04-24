import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import com.mongodb.spark._
import com.mongodb.spark.sql._
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.ReadConfig;

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.DataFrame

class SparkAnalytics(appName: String, master: String, connectionString: String) {

	val spark = SparkSession.builder.master(master).appName(appName).getOrCreate()
	val sparkContext = spark.sparkContext

	//val conf = new SparkConf().setAppName(appName).setMaster(master) //.set("spark.mongodb.input.uri", connectionString)
	//val sparkContext = new SparkContext(conf)
	val sqlContext = SQLContext.getOrCreate(sparkContext)

	def getDataFromMongoDB(tableName: String) : DataFrame = {
		val connStringwithTable = connectionString + "." + tableName
		val df  = sqlContext.loadFromMongoDB(ReadConfig(Map("uri" -> connStringwithTable)))
		// to do : give the most recent data --> how?
		return df
		
	}

	def stopSpark(){
		spark.stop()
	}
	
}