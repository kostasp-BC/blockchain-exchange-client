package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Snapshot
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class BalancesChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "balances"
    }
}

class Balance(
    val currency: String,
    val balance: BigDecimal,
    val available: BigDecimal,
    @SerializedName("balance_local")
    val balanceLocal: BigDecimal,
    @SerializedName("available_local")
    val availableLocal: BigDecimal,
    val rate: BigDecimal
)

class BalancesSnapshot(val balances: Array<Balance>) : Snapshot {
    override val channel: String
        get() = BalancesChannel.NAME
}