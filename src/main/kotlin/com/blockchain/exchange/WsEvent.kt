package com.blockchain.exchange

enum class WsEvent {
    SUBSCRIBED,
    UNSUBSCRIBED,
    REJECTED,
    UPDATED,
    SNAPSHOT;

    override fun toString(): String = this.name.toLowerCase()
}