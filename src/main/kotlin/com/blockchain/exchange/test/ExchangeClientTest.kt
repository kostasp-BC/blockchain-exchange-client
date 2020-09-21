package com.blockchain.exchange.test

import com.blockchain.exchange.ExchangeClientListener
import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.channel.*
import com.blockchain.exchange.message.Snapshot
import com.blockchain.exchange.message.Update
import com.tylerthrailkill.helpers.prettyprint.pp
import java.math.BigDecimal

fun main() {
    val API_KEY = "YOUR_API_KEY"

    val client = ExchangeWsClient(
        wsUrl = "wss://ws.dev.blockchain.info/mercury-gateway/v1/ws",
        listener = object : ExchangeClientListener {

            override fun onSubscribe(
                client: ExchangeWsClient,
                channelName: String,
                args: Map<String, Any>?
            ) {
                println("Subscribed to $channelName [${args?.map { (k, v) -> "$k=$v" }?.joinToString(", ")}]")
                when (channelName) {
                    AuthChannel.NAME -> {
                        println("Successfully authenticated.")
                        client.subscribe(BalancesChannel.NAME)
                        client.subscribe(TradingChannel.NAME, "cancelOnDisconnect" to "true")
                    }
                }
            }

            override fun onUnsubscribe(
                client: ExchangeWsClient,
                channelName: String,
                args: Map<String, Any>?
            ) {
                println("Unsubscribed from $channelName [${args?.map { (k, v) -> "$k=$v" }?.joinToString(", ")}]")
                when (channelName) {
                    AuthChannel.NAME -> println("De-authenticated")
                }
            }

            override fun onRejection(
                client: ExchangeWsClient,
                channelName: String,
                args: Map<String, Any>?
            ) {
                println("Rejection from $channelName [${args?.map { (k, v) -> "$k=$v" }?.joinToString(", ")}]")
                when (channelName) {
                    AuthChannel.NAME -> println("Authentication failed")
                    TradingChannel.NAME -> {
                        val action = args?.let { it["action"] as? String } ?: return
                        val text = args?.let { it["text"] as? String }
                        when (action) {
                            TradingAction.PLACE_ORDER.jsonValue, TradingAction.PLACE_MARGIN_ORDER.jsonValue -> {
                                val clOrdID = args?.let { it["clOrdID"] as? String }
                                println("Failed to place order - clientOrderId=$clOrdID, reason=$text")
                            }
                            TradingAction.CANCEL_ORDER.jsonValue -> {
                                val orderID = args?.let { it["orderID"] as? String }
                                println("Failed to cancel order - orderID=$orderID, reason=$text")
                            }
                            TradingAction.CANCEL_ALL_ORDERS.jsonValue -> {
                                println("Failed to bulk cancel orders - reason=$text")
                            }
                        }
                    }
                }
            }

            override fun onSnapshot(
                client: ExchangeWsClient,
                channelName: String,
                snapshot: Snapshot
            ) {
                println("Received snapshot from channel $channelName")
                when (channelName) {
                    TradingChannel.NAME -> (snapshot as TradingSnapshot).orders.forEach { it.pp() }
                    BalancesChannel.NAME -> (snapshot as BalancesSnapshot).balances.forEach { it.pp() }
                    SymbolsChannel.NAME -> (snapshot as SymbolsSnapshot).symbols.forEach { (symbol, info) -> print("$symbol: "); info.pp() }
                    L2Channel.NAME -> (snapshot as L2Snapshot).let {
                        println("${it.symbol}: ")
                        println("bids:")
                        it.bids.forEach { e -> e.pp() }
                        println("asks:")
                        it.asks.forEach { e -> e.pp() }
                    }
                    L3Channel.NAME -> (snapshot as L3Snapshot).let {
                        println("${it.symbol}: ")
                        println("bids:")
                        it.bids.forEach { e -> e.pp() }
                        println("asks:")
                        it.asks.forEach { e -> e.pp() }
                    }
                    TickerChannel.NAME -> (snapshot as TickerSnapshot).pp()
                }
            }

            override fun onConnect(client: ExchangeWsClient) {
                client.subscribe(HeartbeatChannel.NAME)
                client.subscribe(SymbolsChannel.NAME) // subscribes to all symbols
                client.subscribe(PricesChannel.NAME, "symbol" to "BTC-USD", "granularity" to 60)
                client.subscribe(TradesChannel.NAME, "symbol" to "BTC-USD")
                client.subscribe(TickerChannel.NAME, "symbol" to "BTC-USD")
                client.subscribe(L2Channel.NAME, "symbol" to "BTC-USD")
                client.subscribe(L3Channel.NAME, "symbol" to "BTC-USD")
                client.auth(API_KEY)
            }

            override fun onUpdate(
                client: ExchangeWsClient,
                channelName: String,
                update: Update
            ) {
                println("Received update from channel $channelName")
                when (channelName) {
                    TradingChannel.NAME -> {
                        when (update) {
                            is TradingUpdate -> {
                                val orderID = update.order.orderID
                                val clOrdID = update.order.clOrdID
                                update.pp()
                            }
                            is TradingMarginOrderDetails -> {
                                update.pp()
                            }
                        }
                    }
                    SymbolsChannel.NAME -> (update as SymbolsUpdate).pp()
                    L2Channel.NAME -> (update as L2Update).let {
                        println("${it.symbol}: ")
                        println("bids:")
                        it.bids.forEach { e -> e.pp() }
                        println("asks:")
                        it.asks.forEach { e -> e.pp() }
                    }
                    L3Channel.NAME -> (update as L3Update).let {
                        println("${it.symbol}: ")
                        println("bids:")
                        it.bids.forEach { e -> e.pp() }
                        println("asks:")
                        it.asks.forEach { e -> e.pp() }
                    }
                    TradesChannel.NAME -> (update as TradesUpdate).pp()
                    HeartbeatChannel.NAME -> (update as HeartbeatUpdate).pp()
                    PricesChannel.NAME -> (update as PricesUpdate).let {
                        println("${it.symbol}: ${it.price.joinToString(", ")}")
                    }
                    TickerChannel.NAME -> (update as TickerUpdate).pp()
                }
            }

            override fun onMessage(
                client: ExchangeWsClient,
                message: String?
            ) {
                println("[DEBUG] Received message: $message")
            }
        }
    )

    client.connect()
    Thread.sleep(5_000)
    (0..2).forEach {
        client.placeOrder(
            "Order$it",
            "BTC-USD",
            OrderType.LIMIT,
            OrderSide.BUY,
            BigDecimal("0.01"),
            BigDecimal("10000"),
            marginOrder = true
        )
    }
    client.cancelAllOrders("BTC-USD")
}