package com.englesoft.model

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

class TicTacToeGame {

    private val state = MutableStateFlow(GameState())

    private val playerSockets = ConcurrentHashMap<String, WebSocketSession>()

    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var delayGameJob: Job? = null

    init {
        state.onEach(::broadcast).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession): String? {
        val isPlayerX = state.value.connectedPlayers.any { it == "X" }
        val player = if (isPlayerX) "O" else "X"
        state.update {
            if (state.value.connectedPlayers.contains(player)) {
                return null
            }
            if (playerSockets.contains(player)) {
                playerSockets[player] = session
            }
            it.copy(connectedPlayers = it.connectedPlayers + player)
        }
        return player
    }

    fun disconnectPlayer(player: String) {
        playerSockets.remove(player)
        state.update {
            it.copy(connectedPlayers = it.connectedPlayers - player)
        }
    }

    suspend fun broadcast(state: GameState) {
        playerSockets.values.forEach { socket ->
            socket.send(
                Json.encodeToString(state)
            )
        }
    }

    fun finishTurn(player: String, x: Int, y: Int) {
        if (state.value.field[x][y] != null || state.value.winingPlayer != null) {
            return
        }
        if (state.value.playerAtTurn != player) {
            return
        }
        val currentPlayer = state.value.playerAtTurn
        state.update {
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }
            val isBoardFull = newField.all { it.all { it != null } }
            if (isBoardFull) {
                startNewRoundDelay()
            }
            it.copy(
                playerAtTurn = if (currentPlayer == "X") "O" else "X",
                field = newField,
                isBoardFull = isBoardFull,
                /*winingPlayer = getWinningPlayer()?.also {
                    startNewRoundDelay()
                }*/
            )
        }
    }

    /*private fun getWinningPlayer(): Any {
    }*/

    private fun startNewRoundDelay() {
        delayGameJob?.cancel()
        delayGameJob = gameScope.launch {
            delay(5000L)
            state.update {
                it.copy(
                    playerAtTurn = "X",
                    field = GameState.emptyField(),
                    winingPlayer = null,
                    isBoardFull = false
                )
            }
        }
    }

}