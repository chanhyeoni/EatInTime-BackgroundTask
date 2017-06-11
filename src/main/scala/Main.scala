import org.apache.spark.sql.Dataset
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import java.text.SimpleDateFormat

object Main {


	def main(args: Array[String]) {
		// to do 
		// 1. tweak it such that it runs periodically
		// 2. figure out how to deploy this project on a server

		while (true){
			val obj: SparkAnalytics = new SparkAnalytics("EatInTimeBackgroundTask", "local", "mongodb://heroku_tw4s316k:knsfmmk94vnt3onv3a3n88b5hq@ds145188.mlab.com:45188/heroku_tw4s316k")

			// Scala uses infix operation (not using . and () )
			val df: DataFrame = obj getDataFromMongoDB "rawData"
			//val newDf = df.drop("_id", "date")
	    	val transformedDF = obj normalizeData df
	    	//val Array(trainingData, testData) = transformedDF.randomSplit(Array(0.7, 0.3))
	    	val dfForPrediction = transformedDF select "features"
	    	val model = obj conductAnalytics dfForPrediction
	    	val fileLocation = "tmp/freshness/k-means-logistic-regression-model"
	    	val msg = obj saveResult(model, fileLocation)
	    	if(msg != 200) {
	    	 	// save it to the error log file
	    	 	println("no good")
	    	}else{
	    		println("good!")
	    	}

	    	val testDF = transformedDF.sort("date").limit(1)
	    	val alert_msg = obj.updateAlert(model, testDF, fileLocation)

			obj.stopSpark()

			val format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
			println("successfully completed the job at " + format.format(new java.util.Date()))
			// stop the execution for 10 minutes (600000 milliseconds)
			Thread.sleep(600000)


		}
	}
}