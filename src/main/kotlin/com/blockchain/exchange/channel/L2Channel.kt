package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class L2Channel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "l2"
    }
}

class L2BookEntry(
    val num: Long,
    @SerializedName("px")
    val price: BigDecimal,
    @SerializedName("qty")
    val quantity: BigDecimal
)

class L2Update(
    val symbol: String,
    val bids: Array<L2BookEntry>,
    val asks: Array<L2BookEntry>
) : Update {
    override val channel: String
        get() = L2Channel.NAME
}

class L2Snapshot(
    val symbol: String,
    val bids: Array<L2BookEntry>,
    val asks: Array<L2BookEntry>
) : Snapshot {
    override val channel: String
        get() = L2Channel.NAME
}