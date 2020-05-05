package com.blockchain.exchange.channel

import com.blockchain.exchange.ExchangeWsClient

class AuthChannel(
    override val exchangeWsClient: ExchangeWsClient
) : Channel {
    override val name: String
        get() = NAME

    companion object {
        const val NAME = "auth"
    }
}