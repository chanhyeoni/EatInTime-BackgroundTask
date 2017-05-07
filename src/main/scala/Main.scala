
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema

object Main {
  def main(args: Array[String]) {
		// to do 
		// 1. tweak it such that it runs periodically
		// 2. figure out how to deploy this project on a server
  		
		val obj: SparkAnalytics = new SparkAnalytics("EatInTimeBackgroundTask", "local", "mongodb://heroku_tw4s316k:knsfmmk94vnt3onv3a3n88b5hq@ds145188.mlab.com:45188/heroku_tw4s316k");
		val df: DataFrame = obj.getDataFromMongoDB("rawData")
		val newDf = df.drop("_id", "date")
    	val transformedDF = obj.normalizeData(newDf)
    	
		obj.stopSpark()


  }
}