package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*

class TradesChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "trades"
    }
}

class TradesUpdate(
    val symbol: String,
    val timestamp: Date,
    val side: OrderSide,
    @SerializedName("qty")
    val quantity: BigDecimal,
    @SerializedName("price")
    val price: BigDecimal,
    @SerializedName("trade_id")
    val tradeId: String
) : Update {
    override val channel: String
        get() = TradesChannel.NAME
}