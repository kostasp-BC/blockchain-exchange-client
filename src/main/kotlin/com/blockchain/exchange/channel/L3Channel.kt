package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class L3Channel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "l3"
    }
}

class L3BookEntry(
    val id: String,
    @SerializedName("px")
    val price: BigDecimal,
    @SerializedName("qty")
    val quantity: BigDecimal
)

class L3Update(
    val symbol: String,
    val bids: Array<L3BookEntry>,
    val asks: Array<L3BookEntry>
) : Update {
    override val channel: String
        get() = L3Channel.NAME
}

class L3Snapshot(
    val symbol: String,
    val bids: Array<L3BookEntry>,
    val asks: Array<L3BookEntry>
) : Snapshot {
    override val channel: String
        get() = L3Channel.NAME
}