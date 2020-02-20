package io.holunda.camunda

import com.fasterxml.jackson.databind.SerializationFeature
import io.holunda.camunda.features.CamundaKtorFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(@Suppress("UNUSED_PARAMETER") testing: Boolean = false) {

    val logger = LoggerFactory.getLogger("Application")

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    val engine =  install(CamundaKtorFeature) {
        isJobExecutorActivate = true
    }

    // Just as demo, start some processes:
    val process = engine.runtimeService.startProcessInstanceByKey("ktor-process")
    logger.info("Started process: " + process.processInstanceId)

    val process2 = engine.runtimeService.startProcessInstanceByKey("ktor-process")
    logger.info("Started process: " + process2.processInstanceId)

    // Setup routing to just start a process
    routing {
        route("process") {
            get("/startByKey/{key}") {
                val processInstance = engine.runtimeService.startProcessInstanceByKey(call.parameters["key"])
                logger.info("Started process: " + processInstance.processInstanceId)
                call.respond(HttpStatusCode.Created, processInstance.processInstanceId)
            }
        }
    }
}
