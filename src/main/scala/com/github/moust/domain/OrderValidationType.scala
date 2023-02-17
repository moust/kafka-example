package com.github.moust.domain

import enumeratum._

sealed trait OrderValidationType extends EnumEntry

case object OrderValidationType extends Enum[OrderValidationType] with CirceEnum[OrderValidationType] {

  case object InventoryCheck  extends OrderValidationType
  case object FraudCheck  extends OrderValidationType
  case object OrderDetaislCheck extends OrderValidationType

  val values = findValues

}
