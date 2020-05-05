package com.blockchain.exchange.message

class Unsubscribed(
    override val channel: String,
    val extraFields: Map<String, Any>?
) : ExchangeMsg