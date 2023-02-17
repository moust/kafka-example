package com.github.moust.domain

import enumeratum._

sealed trait OrderState extends EnumEntry

case object OrderState extends Enum[OrderState] with CirceEnum[OrderState] {

  case object Created   extends OrderState
  case object Validated extends OrderState
  case object Failed    extends OrderState
  case object Shipped   extends OrderState

  val values = findValues

}
