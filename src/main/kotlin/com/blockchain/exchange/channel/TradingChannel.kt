package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.ExchangeMsg
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.google.gson.annotations.SerializedName
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

enum class OrderType(val jsonValue: String) {
    @SerializedName("limit")
    LIMIT("limit"),
    @SerializedName("market")
    MARKET("market"),
    @SerializedName("stop")
    STOP("stop"),
    @SerializedName("stopLimit")
    STOP_LIMIT("stopLimit")
}

enum class OrderSide(val jsonValue: String) {
    @SerializedName("buy")
    BUY("buy"),
    @SerializedName("sell")
    SELL("sell")
}

enum class ExecInst(val jsonValue: String) {
    @SerializedName("ALO")
    ADD_LIQUIDITY_ONLY("ALO")
}

enum class TimeInForce(val jsonValue: String) {
    @SerializedName("GTC")
    GOOD_TILL_CANCEL("GTC"),
    @SerializedName("GTD")
    GOOD_TILL_DATE("GTD"),
    @SerializedName("IOC")
    IMMEDIATE_OR_CANCEL("IOC"),
    @SerializedName("FOK")
    FILL_OR_KILL("FOK")
}

enum class OrderStatus {
    @SerializedName("pending")
    PENDING,
    @SerializedName("open")
    OPEN,
    @SerializedName("cancelled")
    CANCELLED,
    @SerializedName("partial")
    PARTIAL,
    @SerializedName("filled")
    FILLED,
    @SerializedName("expired")
    EXPIRED,
    @SerializedName("rejected")
    REJECTED
}

fun BigDecimal?.enforceNullOrZero(name: String) {
    if (this != null && this?.signum() != 0) throw IllegalArgumentException("$name must be null or zero")
}

fun BigDecimal?.enforcePositive(name: String) {
    if (this?.signum() != 1) throw IllegalArgumentException("$name must be positive")
}

fun TimeInForce.enforceIn(vararg expectedTif: TimeInForce) {
    if (this !in expectedTif) throw IllegalArgumentException("timeInForce should be one of ${expectedTif.joinToString(", ")}")
}

fun TimeInForce.enforceNotIn(vararg expectedTif: TimeInForce) {
    if (this in expectedTif) throw IllegalArgumentException("Invalid timeInForce")
}

fun ExecInst?.enforceNull() {
    if (this != null) throw IllegalArgumentException("execInst must be null")
}

enum class TradingAction(val jsonValue: String) {
    @SerializedName("NewOrderSingle")
    PLACE_ORDER("NewOrderSingle"),
    @SerializedName("NewOrderSingleMargin")
    PLACE_MARGIN_ORDER("NewOrderSingleMargin"),
    @SerializedName("CancelOrderRequest")
    CANCEL_ORDER("CancelOrderRequest"),
    @SerializedName("OrderMassCancelRequest")
    CANCEL_ALL_ORDERS("OrderMassCancelRequest"),
    @SerializedName("OrderMassStatusRequest")
    LIST_LIVE_ORDERS("OrderMassStatusRequest"),
    @SerializedName("PositionMarginDetails")
    GET_MARGIN_ORDER_DETAILS("PositionMarginDetails")
}

class TradingChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    private val dateFormatter = SimpleDateFormat("YYYYMMdd")

    companion object {
        const val NAME = "trading"
    }

    fun getMarginOrderDetails(
        requestId: String,
        symbol: String,
        collateralCurrency: String,
        side: OrderSide,
        amount: BigDecimal,
        leverageRatio: BigDecimal
    ) {
        val orderParams = mutableMapOf<String, Any>().also { params ->
            params["requestId"] = requestId
            params["symbol"] = symbol
            params["collateralCurrency"] = collateralCurrency
            params["side"] = side.jsonValue
            params["amount"] = amount.toPlainString()
            params["leverageRatio"] = leverageRatio.toPlainString()
        }

        sendMsg(TradingAction.GET_MARGIN_ORDER_DETAILS.jsonValue, orderParams)
    }

    fun placeOrder(
        clientOrderId: String,
        symbol: String,
        orderType: OrderType,
        orderSide: OrderSide,
        quantity: BigDecimal,
        price: BigDecimal? = null,
        stopPrice: BigDecimal? = null,
        timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
        minQuantity: BigDecimal? = null,
        expireDate: Date? = null,
        execInst: ExecInst? = null,
        marginOrder: Boolean = false,
        collateralCurrency: String = "USD",
        leverageRatio: BigDecimal = BigDecimal(1.0)
    ) {
        if (clientOrderId.length > 20) throw IllegalArgumentException("Client order ID must not be longer than 20 characters")

        quantity.enforcePositive("quantity")

        when (orderType) {
            OrderType.LIMIT -> {
                price.enforcePositive("price")
                stopPrice.enforceNullOrZero("stopPrice")
            }
            OrderType.MARKET -> {
                price.enforceNullOrZero("price")
                stopPrice.enforceNullOrZero("stopPrice")
                timeInForce.enforceNotIn(TimeInForce.GOOD_TILL_DATE)
            }
            OrderType.STOP -> {
                price.enforceNullOrZero("price")
                stopPrice.enforcePositive("stopPrice")
                timeInForce.enforceIn(TimeInForce.GOOD_TILL_CANCEL, TimeInForce.GOOD_TILL_DATE)
                execInst.enforceNull()
            }
            OrderType.STOP_LIMIT -> {
                price.enforcePositive("price")
                stopPrice.enforcePositive("stopPrice")
                timeInForce.enforceIn(TimeInForce.GOOD_TILL_CANCEL, TimeInForce.GOOD_TILL_DATE)
                execInst.enforceNull()
            }
        }

        val expireDateInt =
            when {
                timeInForce == TimeInForce.GOOD_TILL_DATE -> {
                    if (expireDate == null || expireDate.before(Date())) throw IllegalArgumentException("expireDate null or in the past")
                    dateFormatter.format(expireDate).toInt()
                }
                expireDate != null -> {
                    throw IllegalArgumentException("expireDate must be null")
                }
                else -> {
                    null
                }
            }

        if (timeInForce == TimeInForce.IMMEDIATE_OR_CANCEL) {
            minQuantity.enforcePositive("minQuantity")
        }

        val orderParams = mutableMapOf<String, Any>().also { params ->
            params["clOrdID"] = clientOrderId
            params["symbol"] = symbol
            params["side"] = orderSide.jsonValue
            params["ordType"] = orderType.jsonValue
            params["timeInForce"] = timeInForce.jsonValue
            params["orderQty"] = quantity.toPlainString()
            price?.also { params["price"] = price.toPlainString() }
            stopPrice?.also { params["stopPx"] = stopPrice.toPlainString() }
            minQuantity?.also { params["minQty"] = minQuantity.toPlainString() }
            expireDateInt?.also { params["expireDate"] = expireDateInt }
            execInst?.also { params["execInst"] = execInst }
            if (marginOrder) {
                params["collateralCurrency"] = collateralCurrency
                params["leverageRatio"] = leverageRatio
            }
        }

        sendMsg(if (marginOrder) TradingAction.PLACE_MARGIN_ORDER.jsonValue else TradingAction.PLACE_ORDER.jsonValue, orderParams)
    }

    fun cancelOrder(orderID: String) = sendMsg(TradingAction.CANCEL_ORDER.jsonValue, mapOf("orderID" to orderID))

    fun cancelAllOrders(symbol: String? = null) =
        sendMsg(TradingAction.CANCEL_ALL_ORDERS.jsonValue, if (symbol != null) mapOf("symbol" to symbol) else emptyMap())

    fun listLiveOrders() = sendMsg(TradingAction.LIST_LIVE_ORDERS.jsonValue)
}

class Order(
    val orderID: String,
    val clOrdID: String,
    val symbol: String,
    val side: OrderSide,
    @SerializedName("ordType")
    val type: OrderType,
    @SerializedName("orderQty")
    val quantity: BigDecimal,
    @SerializedName("leavesQty")
    val remainingQuantity: BigDecimal,
    @SerializedName("cumQty")
    val filledQuantity: BigDecimal,
    @SerializedName("avgPx")
    val averageFillPrice: BigDecimal,
    @SerializedName("ordStatus")
    val status: OrderStatus,
    val timeInForce: TimeInForce,
    val text: String,
    val execType: Char,
    val execID: String,
    val transactTime: Date,
    @SerializedName("lastPx")
    val lastFillPrice: BigDecimal,
    @SerializedName("lastShares")
    val lastFillQuantity: BigDecimal,
    val tradeId: String,
    val fee: BigDecimal,
    @SerializedName("price")
    val limitPrice: BigDecimal?,
    @SerializedName("stopPx")
    val stopPrice: BigDecimal?,
    val marginOrder: Boolean,
    val collateralCurrency: String?,
    val markPrice: BigDecimal?,
    val interestAmount: BigDecimal?,
    val positionMargin: BigDecimal?,
    val marginCallPrice: BigDecimal?,
    val liquidationPrice: BigDecimal?
)

class MarginOrderDetails(
    val requestId: String,
    val callPrice: BigDecimal,
    val liquidationPrice: BigDecimal,
    val bankruptcyPrice: BigDecimal
)

class TradingSnapshot(val orders: Array<Order>) : Snapshot {
    override val channel: String
        get() = TradingChannel.NAME
}

class TradingUpdate(val order: Order) : Update {
    override val channel: String
        get() = TradingChannel.NAME
}

class TradingMarginOrderDetails(val marginOrderDetails: MarginOrderDetails) : Update {
    override val channel: String
        get() = TradingChannel.NAME
}
