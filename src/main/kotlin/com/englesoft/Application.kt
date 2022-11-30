package com.englesoft

import com.englesoft.model.TicTacToeGame
import io.ktor.server.application.*
import com.englesoft.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val game = TicTacToeGame()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting(game)
}
