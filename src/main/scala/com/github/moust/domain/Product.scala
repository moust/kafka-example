package com.github.moust.domain

import enumeratum._

sealed trait Product extends EnumEntry

case object Product extends Enum[Product] with CirceEnum[Product] {

  case object Jumpers  extends Product
  case object Underpants extends Product
  case object Stockings  extends Product

  val values = findValues

}
