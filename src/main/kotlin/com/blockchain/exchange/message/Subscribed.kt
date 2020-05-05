package com.blockchain.exchange.message

class Subscribed(
    override val channel: String,
    val extraFields: Map<String, Any>?
) : ExchangeMsg