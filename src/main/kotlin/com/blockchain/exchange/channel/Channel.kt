package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.WsEvent
import com.blockchain.exchange.message.*
import com.google.gson.JsonObject

interface Channel {
    companion object {
        val ACTION_SUBSCRIBE = "subscribe"
        val ACTION_UNSUBSCRIBE = "unsubscribe"
    }

    val exchangeWsClient: ExchangeWsClient
    val name: String

    private fun jsonObject(action: String) = JsonObject().also {
        it.addProperty("action", action)
        it.addProperty("channel", name)
    }

    fun sendMsg(action: String, args: Map<String, Any>? = null) {
        exchangeWsClient.send(
            jsonObject(action).also { jsonObj ->
                args?.forEach { key, value ->
                    when (value) {
                        is Char -> jsonObj.addProperty(key, value)
                        is Number -> jsonObj.addProperty(key, value)
                        is String -> jsonObj.addProperty(key, value)
                        is Boolean -> jsonObj.addProperty(key, value)
                        else -> throw IllegalArgumentException("Invalid value type ${value.javaClass.name} for key $key")
                    }
                }
            }.toString()
        )
    }

    fun name() = name

    fun subscribe(args: Map<String, Any>? = null) = sendMsg(ACTION_SUBSCRIBE, args)

    fun unsubscribe(args: Map<String, Any>? = null) = sendMsg(ACTION_UNSUBSCRIBE, args)

    fun onMessage(msg: ExchangeMsg): Boolean {
        return when (msg) {
            is Subscribed -> {
                exchangeWsClient.onSubscribe(name, msg.extraFields)
                true
            }
            is Unsubscribed -> {
                exchangeWsClient.onUnsubscribe(name, msg.extraFields)
                true
            }
            is Rejection -> {
                exchangeWsClient.onRejection(name, msg.extraFields)
                true
            }
            is Update -> {
                exchangeWsClient.onUpdate(name, msg)
                true
            }
            is Snapshot -> {
                exchangeWsClient.onSnapshot(name, msg)
                true
            }
            else -> {
                false
            }
        }
    }
}