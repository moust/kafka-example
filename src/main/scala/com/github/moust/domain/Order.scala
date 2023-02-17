package com.github.moust.domain

case class Order(
  id: String,
  customerId: String,
  state: OrderState,
  product: Product,
  quantity: Int,
  price: Double)
