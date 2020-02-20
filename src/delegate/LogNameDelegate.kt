package io.holunda.camunda.delegate

import mu.KLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate

object LogNameDelegate : KLogging(), JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        logger.info { "Executed delegate '${execution.currentActivityName}' for process '${execution.processInstanceId}'!" }
    }

}
