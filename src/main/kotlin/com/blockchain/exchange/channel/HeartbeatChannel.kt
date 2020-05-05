package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient
import com.blockchain.exchange.message.Update
import java.util.*

class HeartbeatChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "heartbeat"
    }
}

class HeartbeatUpdate(
    val timestamp: Date
) : Update {
    override val channel: String
        get() = HeartbeatChannel.NAME
}