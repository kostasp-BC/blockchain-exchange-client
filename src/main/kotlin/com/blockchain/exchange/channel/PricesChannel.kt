package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Update
import java.math.BigDecimal

class PricesChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "prices"
    }
}

class PricesUpdate(
    val symbol: String,
    val price: Array<BigDecimal> // 6 values -> timestamp (not really a big decimal!?), OHLCV
) : Update {
    override val channel: String
        get() = PricesChannel.NAME
}