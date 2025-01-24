package com.example.server_ktor.views

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.logging.Logger
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(navController: NavHostController) {
    val logger = remember { Logger.getLogger("KtorServer") }
    var latitude by remember { mutableStateOf("Aguardando...") }
    var longitude by remember { mutableStateOf("Aguardando...") }
    var compassDirection by remember { mutableStateOf("Aguardando...") }

    val server = remember {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriodMillis = 10_000L
                timeoutMillis = 15_000L
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/chat") {
                    send("Bem-vindo ao WebSocket!") // Mensagem de boas-vindas
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val receivedText = frame.readText()
                                logger.info("Mensagem recebida: $receivedText")

                                // Atualizar as variáveis com base nos dados recebidos
                                val parts = receivedText.split(":")
                                Log.d("Dados: ", receivedText)
                                if (parts.size == 2) {
                                    when (parts[0]) {
                                        "latitude" -> latitude = parts[1]
                                        "longitude" -> longitude = parts[1]
                                        "azimuth" -> compassDirection = parts[1]
                                    }
                                }
                            }
                            is Frame.Close -> {
                                logger.info("Conexão encerrada: ${closeReason.await()}")
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        launchServer(server, logger)
    }

    DisposableEffect(Unit) {
        onDispose {
            logger.info("Parando o servidor...")
            server.stop(1_000, 2_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servidor WebSocket") }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Dados Recebidos:")
                Text("Latitude: $latitude")
                Text("Longitude: $longitude")
                Text("Azimuth: $compassDirection")
            }
        }
    )
}

private fun launchServer(server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>, logger: Logger) {
    CoroutineScope(Dispatchers.IO).launch {
        logger.info("Iniciando o servidor...")
        server.start(wait = true)
    }
}
