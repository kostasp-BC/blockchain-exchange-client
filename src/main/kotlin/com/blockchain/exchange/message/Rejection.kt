package com.blockchain.exchange.message

class Rejection(
    override val channel: String,
    val extraFields: Map<String, Any>?
) : ExchangeMsg