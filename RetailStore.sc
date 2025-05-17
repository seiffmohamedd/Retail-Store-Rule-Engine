import scala.io.Source

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


val lines = Source.fromFile("E:\\ITI 9 Months\\Scala\\Retail-Store-Rule-Engine\\TRX1000.csv").getLines().toList.tail
val orders = lines.map(parseLineToOrder)

//orders.foreach(println)

def splitTimeStamp(o : Order ) :(Int , Int) ={

  // splitting time stamp to get day , month
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
  discount * o.quantity * o.unitPrice
}

def isCheeseOrWineProduct(o: Order): Boolean = {
  o.productName.contains("Wine") || o.productName.contains("Cheese")
}


def applyDiscCheeseOrWine(o: Order): Double = {
  val discount = if(o.productName.contains("Wine")) (5/100) else (10/100)
  discount* o.unitPrice * o.quantity
}

//def isSoldOn23March(o: Order): Boolean = {}
//def applyDiscOn23March(o: Order): Double = {}
//def isBoughtMoreThan5(o: Order): Boolean = {}
//def applyDiscBoughtMoreThan5(o: Order):Double = {}
