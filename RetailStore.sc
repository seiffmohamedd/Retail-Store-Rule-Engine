import scala.io.Source

case class Order(timestamp: String,productName: String, expiryDate: String, quantity: Int, unitPrice: Float, channel: String,
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


def isLessThan30(): Boolean{}
def applyDiscLessThan30(): Double{}
def isCheeseOrWineProduct(): Boolean{}
def applyDiscCheeseOrWine(): Double{}
def isSoldOn23March(): Boolean{}
def applyDiscOn23March(): Double{}
def isBoughtMoreThan5(): Boolean{}
def applyDiscBoughtMoreThan5():Double{}
