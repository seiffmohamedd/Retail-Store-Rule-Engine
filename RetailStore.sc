import scala.io.Source
import java.sql.DriverManager
import java.util.logging.{Logger, FileHandler, SimpleFormatter, Level, LogRecord}
import java.text.SimpleDateFormat
import java.util.Date

// make a logger instance named "RulesEngineLogger".
val logger: Logger = Logger.getLogger("RulesEngineLogger")

// make a FileHandler that writes log records to a specified log file.
val fileHandler = new FileHandler("E:\\ITI 9 Months\\Scala\\Retail-Store-Rule-Engine\\rules_engine.log", true)

// formatter defines how each log record will appear in the log file.
fileHandler.setFormatter(new SimpleFormatter() {
  // make the format method to customize the output for each log entry.
  override def format(record: LogRecord): String = {
    // create a timestamp in the format "yyyy-MM-dd HH:mm:ss"
    val timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis))
    f"$timestamp%-20s ${record.getLevel.getName}%-10s ${record.getMessage}%s\n"
  }
})

// all logs sent to 'logger' will be written to the log file using the handler.
logger.addHandler(fileHandler)

// Disable the default behavior of logging to the console.
logger.setUseParentHandlers(false)


//each line in csv represent those fields
case class Order(timestamp: String ,
                 productName: String,
                 expiryDate: String ,
                 quantity: Int, unitPrice: Float,
                 channel: String,
                 paymentMethod: String)


//function that will split each line and have the case class constructor fields assigned
def parseLineToOrder(line: String): Order = {
  val fields = line.split(",")
  Order(
    timestamp = fields(0),
    productName = fields(1),
    expiryDate = fields(2),
    quantity = fields(3).toInt,
    unitPrice = fields(4).toFloat,
    channel = fields(5),
    paymentMethod = fields(6)
  )
}

// log that iam reading the file csv
logger.info("Started reading and parsing CSV file.")
val lines = Source.fromFile("E:\\ITI 9 Months\\Scala\\Retail-Store-Rule-Engine\\TRX1000.csv").getLines().toList.tail
// map each line to the function that splits based on the file delim
val orders = lines.map(parseLineToOrder)

//orders.foreach(println)

// function that extract the needed info from time stamp(day ,month)
// seperated from split expiry date function bec i will need this function in rule alone
//return tuble of int day and month
def splitTimeStamp(o : Order ) :(Int , Int) ={

  // splitting time stamp column to get day , month
  val stampDate = o.timestamp.split('T')
  val stampDateSplit = stampDate(0).split("-")
  val day= stampDateSplit(2).toInt
  val month = stampDateSplit(1).toInt

  (day , month)
}

// function that extract the needed info from expiry date (day ,month)
//return tuble of int day and month
def splitExpiryDate(o : Order ) :(Int , Int) ={

  val expDate = o.expiryDate.split("-")
  val expDay = expDate(2).toInt
  val expMonth= expDate(1).toInt

  (expDay , expMonth)
}

//first rule qual that gets from the above functions day , month and return true if
//they are in the same month(less than 30 days left) (assuming data only for 1 year -> 2023)
def isLessThan30(o: Order): Boolean = {
  val (stampDay , stampMonth )  = splitTimeStamp(o)
  val (expDay , expMonth )  = splitExpiryDate(o)
  stampMonth == expMonth
}

//apply the discount when rule 1 applied
// the formula gets the discount 29->1% , 28->2% so (30 - days left) 30 is the number of days in 1 month
def applyDiscLessThan30(o: Order): Double={
  val (stampDay , stampMonth )  = splitTimeStamp(o)
  val (expDay , expMonth )  = splitExpiryDate(o)
  val daysLeft = expDay - stampDay
  val discount = (30 - daysLeft ) / 100
  discount
}

// rule 2 i found that sometimes wine and cheese doesnt always come before (-) in the
//product name field so i used the contains function to fetch in the whole string and return true if found
def isCheeseOrWineProduct(o: Order): Boolean = {
  o.productName.contains("Wine") || o.productName.contains("Cheese")
}

// rule 2 calc if wine then 0.05 -> 5% and if cheese then 0.1 -> 10%
def applyDiscCheeseOrWine(o: Order): Double = {
  val discount = if(o.productName.contains("Wine")) (5/100) else (10/100)
  discount
}

//get the day , month from the extarct time stamp function here appears its benefit
// (instead of rewriting extract day , month logic from timestamp again)
//open closed
def isSoldOn23March(o: Order): Boolean = {
  val (day , month) = splitTimeStamp(o)
  day == 23 && month == 3
}
// 50% discount
def applyDiscOn23March(o: Order): Double = {
  0.5
}

def isBoughtMoreThan5(o: Order): Boolean = {
  o.quantity >= 5
}

def applyDiscBoughtMoreThan5(o: Order):Double = {
  val discount = if(o.quantity >=6 && o.quantity <=9) 0.05 else if(o.quantity >=10 && o.quantity <=14) 0.07 else 0.1
  discount
}

def isApp(o: Order ): Boolean = {
  o.channel == "App"
}

//  a 5% discount for every 5 items (or part thereof) bought via the App.
def applyDiscApp(o: Order): Double= {
  val base = o.quantity / 5
  val extra = if (o.quantity % 5 == 0) 0 else 1
  val group = base + extra
  val discount = group * 0.05
  discount
}


def isVisa(o:Order): Boolean={
  o.paymentMethod == "Visa"
}

def applyDiscVisa(o: Order): Double={
  0.05
}

// return the best two discounts for an order from a list of rules using higher order function.
def calculateBestTwoDiscounts(o: Order, rules: List[(Order => Boolean, Order => Double)]): Double = {
  // filter rules where the condition is true for the given order
  val applicableDiscounts = rules
    .filter { case (condition, _) => condition(o) }
    .map { case (_, discountFunc) =>
      // calc discount amount and log it
      val d = discountFunc(o) * o.unitPrice * o.quantity
      logger.fine(s"Applied rule: Discount = $d for product ${o.productName}")
      d }
  // get highest 2 discounts
  val topTwo = applicableDiscounts.sorted(Ordering[Double].reverse).take(2)

  if (topTwo.isEmpty) {
    logger.info(s"No discounts applicable for order: ${o.productName}")
    0.0
  } else {
    // get average
    val avgDiscount = topTwo.sum / topTwo.size
    logger.info(f"Order ${o.productName}: Best 2 discounts total = ${topTwo.sum}%.2f, average = $avgDiscount%.2f")
    avgDiscount
  }
}


val allRules = List(
  // each rule is a tuple of (condition function, discount calculation function)
  (isLessThan30 _, applyDiscLessThan30 _),
  (isCheeseOrWineProduct _, applyDiscCheeseOrWine _),
  (isSoldOn23March _, applyDiscOn23March _),
  (isBoughtMoreThan5 _, applyDiscBoughtMoreThan5 _),
  (isApp _, applyDiscApp _),
  (isVisa _, applyDiscVisa _)
)

//mysql conn string with my username and password
val url: String = "jdbc:mysql://localhost:3306/Retail_Store"
val username: String = "root"
val password: String = "seif"

//driver mysql
Class.forName("com.mysql.cj.jdbc.Driver")
val connection = DriverManager.getConnection(url, username, password)

//insert statement
val insertSql = "insert into Orders (timestamp, product_name, expiry_date, quantity, unit_price, channel, payment_method, final_price) values (?, ?, ?, ?, ?, ?, ?, ?)"
val pstmt = connection.prepareStatement(insertSql)

//for each order apply function to get best 2 discounts and calc final price to insert
//it in the db as a field in the created table
orders.foreach { order =>
  val discount = calculateBestTwoDiscounts(order, allRules)
  val finalPrice = order.unitPrice * order.quantity - discount

  pstmt.setString(1, order.timestamp)
  pstmt.setString(2, order.productName)
  pstmt.setString(3, order.expiryDate)
  pstmt.setInt(4, order.quantity)
  pstmt.setFloat(5, order.unitPrice)
  pstmt.setString(6, order.channel)
  pstmt.setString(7, order.paymentMethod)
  pstmt.setFloat(8, finalPrice.toFloat)

  pstmt.executeUpdate()
  logger.info(s"Inserted order for ${order.productName}, final price: $finalPrice%.2f")
}