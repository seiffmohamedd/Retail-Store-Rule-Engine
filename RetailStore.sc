import scala.io.Source
import java.sql.DriverManager
import java.util.logging.{Logger, FileHandler, SimpleFormatter, Level, LogRecord}
import java.text.SimpleDateFormat
import java.util.Date


val logger: Logger = Logger.getLogger("RulesEngineLogger")
val fileHandler = new FileHandler("E:\\ITI 9 Months\\Scala\\Retail-Store-Rule-Engine\\rules_engine.log", true) // Append mode
fileHandler.setFormatter(new SimpleFormatter() {
  override def format(record: LogRecord): String = {
    val timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis))
    f"$timestamp%-20s ${record.getLevel.getName}%-10s ${record.getMessage}%s\n"
  }
})
logger.addHandler(fileHandler)
logger.setUseParentHandlers(false) // Disable console output

case class Order(timestamp: String ,
                 productName: String,
                 expiryDate: String ,
                 quantity: Int, unitPrice: Float,
                 channel: String,
                 paymentMethod: String)


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

logger.info("Started reading and parsing CSV file.")
val lines = Source.fromFile("E:\\ITI 9 Months\\Scala\\Retail-Store-Rule-Engine\\TRX1000.csv").getLines().toList.tail
val orders = lines.map(parseLineToOrder)

//orders.foreach(println)

def splitTimeStamp(o : Order ) :(Int , Int) ={

  // splitting time stamp column to get day , month
  val stampDate = o.timestamp.split('T')
  val stampDateSplit = stampDate(0).split("-")
  val day= stampDateSplit(2).toInt
  val month = stampDateSplit(1).toInt

  (day , month)
}

def splitExpiryDate(o : Order ) :(Int , Int) ={

  val expDate = o.expiryDate.split("-")
  val expDay = expDate(2).toInt
  val expMonth= expDate(1).toInt

  (expDay , expMonth)
}

def isLessThan30(o: Order): Boolean = {
  val (stampDay , stampMonth )  = splitTimeStamp(o)
  val (expDay , expMonth )  = splitExpiryDate(o)
  stampMonth == expMonth
}

def applyDiscLessThan30(o: Order): Double={
  val (stampDay , stampMonth )  = splitTimeStamp(o)
  val (expDay , expMonth )  = splitExpiryDate(o)
  val daysLeft = expDay - stampDay
  val discount = (30 - daysLeft ) / 100
  discount
}

def isCheeseOrWineProduct(o: Order): Boolean = {
  o.productName.contains("Wine") || o.productName.contains("Cheese")
}


def applyDiscCheeseOrWine(o: Order): Double = {
  val discount = if(o.productName.contains("Wine")) (5/100) else (10/100)
  discount
}

def isSoldOn23March(o: Order): Boolean = {
  val (day , month) = splitTimeStamp(o)
  day == 23 && month == 3
}
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

def applyDiscApp(o: Order): Double= {
  val discount = ((o.quantity - 1) / 5 + 1) * 0.05
  discount
}


def isVisa(o:Order): Boolean={
  o.paymentMethod == "Visa"
}

def applyDiscVisa(o: Order): Double={
  0.05
}

def calculateBestTwoDiscounts(o: Order, rules: List[(Order => Boolean, Order => Double)]): Double = {
  val applicableDiscounts = rules
    .filter { case (condition, _) => condition(o) }
    .map { case (_, discountFunc) =>
      val d = discountFunc(o) * o.unitPrice * o.quantity
      logger.fine(s"Applied rule: Discount = $d for product ${o.productName}")
      d }
  val topTwo = applicableDiscounts.sorted(Ordering[Double].reverse).take(2)

  if (topTwo.isEmpty) {
    logger.info(s"No discounts applicable for order: ${o.productName}")
    0.0
  } else {
    val avgDiscount = topTwo.sum / topTwo.size
    logger.info(f"Order ${o.productName}: Best 2 discounts total = ${topTwo.sum}%.2f, average = $avgDiscount%.2f")
    avgDiscount
  }
}

val allRules = List(
  (isLessThan30 _, applyDiscLessThan30 _),
  (isCheeseOrWineProduct _, applyDiscCheeseOrWine _),
  (isSoldOn23March _, applyDiscOn23March _),
  (isBoughtMoreThan5 _, applyDiscBoughtMoreThan5 _),
  (isApp _, applyDiscApp _),
  (isVisa _, applyDiscVisa _)
)

val url: String = "jdbc:mysql://localhost:3306/Retail_Store"
val username: String = "root"
val password: String = "seif"

Class.forName("com.mysql.cj.jdbc.Driver")
val connection = DriverManager.getConnection(url, username, password)

val insertSql = "insert into Orders (timestamp, product_name, expiry_date, quantity, unit_price, channel, payment_method, final_price) values (?, ?, ?, ?, ?, ?, ?, ?)"
val pstmt = connection.prepareStatement(insertSql)

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