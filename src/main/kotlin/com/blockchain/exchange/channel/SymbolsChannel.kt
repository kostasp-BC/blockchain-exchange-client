package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class SymbolsChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "symbols"
    }
}

enum class SymbolStatus {
    @SerializedName("open")
    OPEN,
    @SerializedName("closed")
    CLOSED,
    @SerializedName("suspended")
    SUSPENDED,
    @SerializedName("halt")
    HALT,
    @SerializedName("halt_freeze")
    HALT_FREEZE
}

class SymbolDetails(
    @SerializedName("base_currency")
    val baseCurrency: String,
    @SerializedName("base_currency_scale")
    val baseCurrencyScale: Int,
    @SerializedName("counter_currency")
    val counterCurrency: String,
    @SerializedName("counter_currency_scale")
    val counterCurrencyScale: Int,
    @SerializedName("min_price_increment")
    val minPriceIncrement: Long,
    @SerializedName("min_price_increment_scale")
    val minPriceIncrementScale: Int,
    @SerializedName("min_order_size")
    val minOrderSize: Long,
    @SerializedName("min_order_size_scale")
    val minOrderSizeScale: Int,
    @SerializedName("max_order_size")
    val maxOrderSize: Long,
    @SerializedName("max_order_size_scale")
    val maxOrderSizeScale: Int,
    @SerializedName("lot_size")
    val lotSize: Long,
    @SerializedName("lot_size_scale")
    val lotSizeScale: Int,
    @SerializedName("auction_price")
    val auctionPrice: BigDecimal,
    @SerializedName("auction_size")
    val auctionSize: BigDecimal,
    @SerializedName("auction_time")
    val auctionTime: String,
    @SerializedName("imbalance")
    val imbalance: BigDecimal,
    val status: SymbolStatus,
    val id: Int
)

class SymbolsSnapshot(
    val symbols: Map<String, SymbolDetails>
) : Snapshot {
    override val channel: String
        get() = SymbolsChannel.NAME
}

class SymbolsUpdate(val symbol: String, val symbolDetails: SymbolDetails) : Update {
    override val channel: String
        get() = SymbolsChannel.NAME
}