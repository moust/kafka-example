package com.github.moust.domain

case class OrderValidation(orderId: String, checkType: OrderValidationType, validationResult: OrderValidationResult)
