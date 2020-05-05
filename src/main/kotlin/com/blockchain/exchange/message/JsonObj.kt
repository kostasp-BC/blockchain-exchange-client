package com.blockchain.exchange.message

import com.google.gson.JsonObject

class JsonObj(override val channel: String, val jsonObject: JsonObject) : ExchangeMsg