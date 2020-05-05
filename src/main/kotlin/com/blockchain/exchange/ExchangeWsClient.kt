package com.blockchain.exchange

import com.blockchain.exchange.channel.*
import com.blockchain.exchange.message.*
import com.google.gson.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.reflect.Type
import java.math.BigDecimal
import java.net.URI
import java.util.*

class ExchangeDeserializer : JsonDeserializer<ExchangeMsg> {
    private fun extraFields(channelName: String, msg: JsonObject): Map<String, String>? {
        return msg.entrySet().filter {
            if (channelName == TradingChannel.NAME) it.key !in arrayOf("event", "channel", "seqnum")
            else it.key !in arrayOf("action", "event", "channel", "seqnum")
        }.map {
            it.key to it.value.asString
        }.toMap().let {
            if (it.isNotEmpty()) it
            else null
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ExchangeMsg {
        val jsonObj = json.asJsonObject
        val channel = jsonObj["channel"]!!.asString
        val event = try {
            WsEvent.valueOf(jsonObj["event"]!!.asString.toUpperCase())
        } catch (e: Exception) {
            null
        }

        return when (event) {
            WsEvent.SUBSCRIBED -> Subscribed(channel, extraFields(channel, jsonObj))
            WsEvent.UNSUBSCRIBED -> Unsubscribed(channel, extraFields(channel, jsonObj))
            WsEvent.REJECTED -> Rejection(channel, extraFields(channel, jsonObj))
            WsEvent.SNAPSHOT -> {
                when (channel) {
                    TradingChannel.NAME -> context.deserialize(jsonObj, TradingSnapshot::class.java)
                    BalancesChannel.NAME -> context.deserialize(jsonObj, BalancesSnapshot::class.java)
                    SymbolsChannel.NAME -> context.deserialize(jsonObj, SymbolsSnapshot::class.java)
                    L2Channel.NAME -> context.deserialize(jsonObj, L2Snapshot::class.java)
                    L3Channel.NAME -> context.deserialize(jsonObj, L3Snapshot::class.java)
                    TickerChannel.NAME -> context.deserialize(jsonObj, TickerSnapshot::class.java)
                    else -> JsonObj(channel, jsonObj)
                }
            }
            WsEvent.UPDATED -> {
                when (channel) {
                    TradingChannel.NAME -> TradingUpdate(context.deserialize(jsonObj, Order::class.java))
                    SymbolsChannel.NAME -> SymbolsUpdate(
                        jsonObj["symbol"]!!.asString,
                        context.deserialize(jsonObj, SymbolDetails::class.java)
                    )
                    L2Channel.NAME -> context.deserialize(jsonObj, L2Update::class.java)
                    L3Channel.NAME -> context.deserialize(jsonObj, L2Update::class.java)
                    TradesChannel.NAME -> context.deserialize(jsonObj, TradesUpdate::class.java)
                    HeartbeatChannel.NAME -> context.deserialize(jsonObj, HeartbeatUpdate::class.java)
                    PricesChannel.NAME -> context.deserialize(jsonObj, PricesUpdate::class.java)
                    TickerChannel.NAME -> context.deserialize(jsonObj, TickerUpdate::class.java)
                    else -> JsonObj(channel, jsonObj)
                }
            }
            null -> JsonObj(channel, jsonObj)
        }
    }
}

class ExchangeWsClient(
    wsUrl: String = "wss://ws.blockchain.com/mercury-gateway/v1/ws",
    headers: Map<String, String> = mapOf("Origin" to "https://exchange.blockchain.com"),
    private val listener: ExchangeClientListener
) : WebSocketClient(
    URI.create(wsUrl),
    headers
) {
    private val authChannel = AuthChannel(this)
    private val balancesChannel = BalancesChannel(this)
    private val tradingChannel = TradingChannel(this)
    private val symbolsChannel = SymbolsChannel(this)
    private val l2Channel = L2Channel(this)
    private val l3Channel = L3Channel(this)
    private val tradesChannel = TradesChannel(this)
    private val heartbeatChannel = HeartbeatChannel(this)
    private val pricesChannel = PricesChannel(this)
    private val tickerChannel = TickerChannel(this)

    private val gson = GsonBuilder().registerTypeAdapter(ExchangeMsg::class.java, ExchangeDeserializer()).create()
    private val channels = listOf(
        authChannel, balancesChannel, tradingChannel, symbolsChannel, l2Channel, l3Channel, tradesChannel,
        heartbeatChannel, pricesChannel, tickerChannel
    ).map { it.name to it }.toMap()

    override fun send(text: String?) {
        println("[DEBUG] Sending message: $text")
        super.send(text)
    }

    override fun onMessage(message: String?) {
        val exchangeMsg = gson.fromJson(message, ExchangeMsg::class.java)
        val handler = channels[exchangeMsg.channel]
            ?: return println("Unexpected channel ${exchangeMsg.channel} received, ignoring message [message='$message']")
        if (!handler.onMessage(exchangeMsg)) listener.onMessage(this, message)
    }

    override fun onOpen(handshakedata: ServerHandshake?) = listener.onConnect(this)
    override fun onClose(code: Int, reason: String?, remote: Boolean) =
        listener.onDisconnect(this, code, reason, remote)

    override fun onError(ex: Exception?) = listener.onError(this, ex)

    fun auth(apiKey: String) = authChannel.subscribe(mapOf("token" to apiKey))

    fun subscribe(channelName: String, vararg args: Pair<String, Any>) {
        channels[channelName]?.subscribe(mapOf(*args)) ?: throw IllegalArgumentException("Invalid channel")
    }

    fun unsubscribe(channelName: String, vararg args: Pair<String, Any>) {
        channels[channelName]?.unsubscribe(mapOf(*args)) ?: throw IllegalArgumentException("Invalid channel")
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
        execInst: ExecInst? = null
    ) = tradingChannel.placeOrder(
        clientOrderId,
        symbol,
        orderType,
        orderSide,
        quantity,
        price,
        stopPrice,
        timeInForce,
        minQuantity,
        expireDate,
        execInst
    )

    fun cancelOrder(orderID: String) = tradingChannel.cancelOrder(orderID)
    fun cancelAllOrders(symbol: String? = null) = tradingChannel.cancelAllOrders(symbol)
    fun listLiveOrders() = tradingChannel.listLiveOrders()

    fun onSubscribe(channelName: String, args: Map<String, Any>? = null) =
        listener.onSubscribe(this, channelName, args)

    fun onUnsubscribe(channelName: String, args: Map<String, Any>? = null) =
        listener.onUnsubscribe(this, channelName, args)

    fun onRejection(channelName: String, args: Map<String, Any>? = null) =
        listener.onRejection(this, channelName, args)

    fun onUpdate(channelName: String, update: Update) = listener.onUpdate(this, channelName, update)
    fun onSnapshot(channelName: String, snapshot: Snapshot) = listener.onSnapshot(this, channelName, snapshot)
}

interface ExchangeClientListener {
    fun onConnect(client: ExchangeWsClient)

    fun onSubscribe(client: ExchangeWsClient, channelName: String, args: Map<String, Any>?) {}
    fun onUnsubscribe(client: ExchangeWsClient, channelName: String, args: Map<String, Any>?) {}
    fun onRejection(client: ExchangeWsClient, channelName: String, args: Map<String, Any>?) {}
    fun onUpdate(client: ExchangeWsClient, channelName: String, update: Update) {}
    fun onSnapshot(client: ExchangeWsClient, channelName: String, snapshot: Snapshot) {}

    fun onMessage(client: ExchangeWsClient, message: String?) {}
    fun onError(client: ExchangeWsClient, e: Exception?) {}
    fun onDisconnect(client: ExchangeWsClient, code: Int, reason: String?, remote: Boolean) {}
}