package com.github.moust.domain

import enumeratum._

sealed trait OrderValidationResult extends EnumEntry

case object OrderValidationResult extends Enum[OrderValidationResult] with CirceEnum[OrderValidationResult] {

  case object Pass  extends OrderValidationResult
  case object Fail  extends OrderValidationResult
  case object Error extends OrderValidationResult

  val values = findValues

}
