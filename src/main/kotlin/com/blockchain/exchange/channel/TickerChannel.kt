package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class TickerChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "ticker"
    }
}

class TickerUpdate(
    val symbol: String,
    @SerializedName("last_trade_price")
    val lastTradePrice: BigDecimal?,
    @SerializedName("volume_24h")
    val volume24h: BigDecimal?,
    @SerializedName("price_24h")
    val price24h: BigDecimal?
) : Update {
    override val channel: String
        get() = TickerChannel.NAME
}

class TickerSnapshot(
    val symbol: String,
    @SerializedName("last_trade_price")
    val lastTradePrice: BigDecimal?,
    @SerializedName("volume_24h")
    val volume24h: BigDecimal?,
    @SerializedName("price_24h")
    val price24h: BigDecimal?
) : Snapshot {
    override val channel: String
        get() = TickerChannel.NAME
}