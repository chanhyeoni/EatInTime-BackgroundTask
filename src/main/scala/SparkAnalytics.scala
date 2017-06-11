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
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.{concat, lit}
import org.apache.spark.sql.types.{StructType,StructField,StringType,DoubleType}
import org.apache.spark.sql.functions.udf

import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.clustering.KMeansModel
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression}
import org.apache.spark.ml.linalg.{Vector, Vectors}


import scalaj.http._
import java.io._

import scala.math.Ordering



//import org.apache.spark.implicits._

class SparkAnalytics(appName: String, master: String, connectionString: String) extends java.io.Serializable {

	val isDebug = true
	// val isDebug = false;

	val spark = SparkSession.builder.master(master).appName(appName).getOrCreate()
	import spark.implicits._
	val sparkContext = spark.sparkContext
	val sqlContext = SQLContext.getOrCreate(sparkContext)

	def getDataFromMongoDB(tableName: String) : DataFrame = {
		/** 
		* getDataFromMongoDB retrieves the data from the MongoDB database using the connectionString and the parameter called the tableName.
		* It uses the loadFromMongoDB; it takes the uri which contains connectionString and tableName and gives the data in the DataFrame format
		* parameters
		* tableName (String) : the name of the table (collection) to retrieve the data from
		* returns
		* df (DataFrame) : the data in the format of DataFrame 
		**/
		val connStringwithTable = connectionString + "." + tableName
		val df  = sqlContext.loadFromMongoDB(ReadConfig(Map("uri" -> connStringwithTable)))
		// to do : give the most recent data --> how?
		return df
	}

	def normalizeData(data: DataFrame) : DataFrame = {
		/** 
		* normalizeData gets the data from the data frame and normalizes the number such that it lies between 0 and 1
		* parameters
		* data (DataFrame) : the raw data of type DataFrame from the data repository (this time, MongoDB)
		* returns
		* transformedDF (DataFrame) : the new dataframe object that contains normalized data
		**/
		
		data.registerTempTable("df_table")
		// get the columns
		val columns = data.columns
		// get the minimum value of the columnwise data
		var queryString = columns.map(x => "MIN(" + x + ") as minVal_" + x).mkString(", ")
		val minVal = sqlContext.sql("SELECT " + queryString + " FROM df_table").rdd.collect().apply(0)
		// get the maximum value of the columwise data
		queryString = columns.map(x => "MAX(" + x + ") as maxVal_" + x).mkString(", ")
		val maxVal = sqlContext.sql("SELECT " + queryString + " FROM df_table").rdd.collect().apply(0)

		// get the transformed data using the min-max scaler technique
		val transformedDF = data.select(
			(($"C2H5OH" - minVal(0).asInstanceOf[Double]) / (maxVal(0).asInstanceOf[Double] - minVal(0).asInstanceOf[Double])),
			(($"C3H8" - minVal(1).asInstanceOf[Double]) / (maxVal(1).asInstanceOf[Double] - minVal(1).asInstanceOf[Double])),
			(($"C4H10" - minVal(2).asInstanceOf[Double]) / (maxVal(2).asInstanceOf[Double] - minVal(2).asInstanceOf[Double])),
			(($"CH4" - minVal(3).asInstanceOf[Double]) / (maxVal(3).asInstanceOf[Double] - minVal(3).asInstanceOf[Double])),
			(($"CO" - minVal(4).asInstanceOf[Double]) / (maxVal(4).asInstanceOf[Double] - minVal(4).asInstanceOf[Double])),
			(($"H2" - minVal(5).asInstanceOf[Double]) / (maxVal(5).asInstanceOf[Double] - minVal(5).asInstanceOf[Double])),
			(($"NH3" - minVal(6).asInstanceOf[Double]) / (maxVal(6).asInstanceOf[Double] - minVal(6).asInstanceOf[Double])),
			(($"NO2" - minVal(7).asInstanceOf[Double]) / (maxVal(7).asInstanceOf[Double] - minVal(7).asInstanceOf[Double])),
			$"date",
			$"_id"
			).toDF("C2H5OH", "C3H8", "C4H10", "CH4", "CO", "H2", "NH3", "NO2", "date", "_id")

		// transform the data such that its column is now features and each row has a Vector of the data
		// use the user-defined function (UDF) to transform the multi-column data into the Venctor-based data
		val transformFunc = udf[Vector, Double, Double, Double, Double, Double, Double, Double, Double]{(a,b,c,d,e,f,g,h) => Vectors.dense(a,b,c,d,e,f,g,h)}
		val finalDF = transformedDF.withColumn(
			"features",
			transformFunc(
				transformedDF("C2H5OH"),
				transformedDF("C3H8"),
				transformedDF("C4H10"),
				transformedDF("CH4"),
				transformedDF("CO"),
				transformedDF("H2"),
				transformedDF("NH3"),
				transformedDF("NO2")
			)
		)

		return finalDF

	}

	def conductAnalytics(data: DataFrame) : KMeansModel = {
		// Trains a k-means model.
		val kmeans = new KMeans().setK(2).setSeed(1L)
		val model = kmeans.fit(data)

		// Evaluate clustering by computing Within Set Sum of Squared Errors.
		val WSSSE = model.computeCost(data)
		println(s"Within Set Sum of Squared Errors = $WSSSE")

		return model

	}

	def updateAlert(model: KMeansModel, testData: DataFrame, filePath: String): Int =  {
		// update the alert system if necessary

		// find the _id of the result collection whose filePath matches the parameter 'type'

		val predictedData = model.transform(testData).select("prediction").first().apply(0)

		var msg = "Your food is in good condition"
		if (predictedData.asInstanceOf[Int] > 0){
			// send the alert
			msg = "Alert! Your food is in danger of getting spoiled!"
		}

		val url = if (isDebug) "http://localhost:8080/statusAlert" else "https://rocky-lake-67126.herokuapp.com/statusAlert"
		val response: HttpResponse[String] = Http(url).param("type",filePath).param("msg", msg).asString

		return response.code		
	}


	def saveResult(model: KMeansModel, fileLocation: String): Int =  {
		// store the result in the disk or storage place
		// local : in the tmp folder
		// production : in the server (S3)
		model.write.overwrite.save(fileLocation)

		// store the result in the database by making an API call
		val url = if (isDebug) "http://localhost:8080/insertResult" else "https://rocky-lake-67126.herokuapp.com/insertResult"
		val response: HttpResponse[String] = Http(url).param("filePath",fileLocation).param("uid", model.uid).asString

	    return response.code
	}

	

	def stopSpark(){
		spark.stop()
	}
	
}