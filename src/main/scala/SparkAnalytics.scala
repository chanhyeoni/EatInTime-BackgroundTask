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

import scala.math.Ordering
//import org.apache.spark.implicits._



class SparkAnalytics(appName: String, master: String, connectionString: String) extends java.io.Serializable {

	val spark = SparkSession.builder.master(master).appName(appName).getOrCreate()
	import spark.implicits._
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

	// val minMaxScaler = (dataPoint: Double, minValue: Double, maxValue: Double) => {
	// 	 ((dataPoint - minValue) / (maxValue - minValue)) * (1.0 - 0.0) + 0.0
	// }


	def normalizeData(data: DataFrame) : DataFrame = {
		/** normalizeData gets the data from the data frame and normalizes the number such that it lies between 0 and 1
		* parameters
		* data (DataFrame) : the raw data of type DataFrame from the data repository (this time, MongoDB)
		* 
		**/
		
		data.registerTempTable("df_table")
		// get the columns
		val columns = data.columns
		// get the minimum value of the columnwise data
		var queryString = columns.map(x => "MIN(" + x + ") as minVal_" + x).mkString(", ")
		val minVal = sqlContext.sql("SELECT " + queryString + " FROM df_table").rdd.collect().apply(0)

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
			(($"NO2" - minVal(7).asInstanceOf[Double]) / (maxVal(7).asInstanceOf[Double] - minVal(7).asInstanceOf[Double]))
			).toDF("C2H5OH", "C3H8", "C4H10", "CH4", "CO", "H2", "NH3", "NO2")

		return transformedDF

	}

	def stopSpark(){
		spark.stop()
	}
	
}