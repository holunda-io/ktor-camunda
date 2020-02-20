package io.holunda.camunda.features

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KLogging
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.delegate.VariableListener
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator
import org.camunda.bpm.engine.repository.Deployment
import org.reflections.Reflections
import java.io.File
import kotlin.reflect.full.companionObject


class CamundaKtorFeature {

    class Configuration {
        var isJobExecutorActivate = true
    }

    companion object Feature : KLogging(), ApplicationFeature<ApplicationCallPipeline, Configuration, ProcessEngine> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<ProcessEngine>("ProcessEngine")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ProcessEngine {

            val configuration = Configuration().apply(configure)

            val engineConfiguration = StandaloneInMemProcessEngineConfiguration()

            registerDelegates(engineConfiguration)

            engineConfiguration.isJobExecutorActivate = configuration.isJobExecutorActivate

            engineConfiguration.idGenerator = StrongUuidGenerator()

            val processEngine = engineConfiguration.buildProcessEngine()

            deployment(processEngine.repositoryService)

            return processEngine
        }

        private fun registerDelegates(processEngineConfiguration: ProcessEngineConfigurationImpl) {
            val reflections = Reflections("io.holunda.camunda")

            if(processEngineConfiguration.beans == null) {
                processEngineConfiguration.beans = mutableMapOf()
            }

            reflections.getSubTypesOf(JavaDelegate::class.java).forEach { it.register(processEngineConfiguration) }
            reflections.getSubTypesOf(TaskListener::class.java).forEach { it.register(processEngineConfiguration) }
            reflections.getSubTypesOf(ExecutionListener::class.java).forEach { it.register(processEngineConfiguration) }
            reflections.getSubTypesOf(VariableListener::class.java).forEach { it.register(processEngineConfiguration) }
        }

        private fun deployment(repositoryService: RepositoryService): Deployment {
            val deployment = repositoryService.createDeployment()
            scanForBpmnFiles().forEach { file -> deployment.addClasspathResource(file) }
            return deployment.deploy()
        }

        private fun scanForBpmnFiles(): List<String> = Application::class.java.classLoader.getResources("").toList()
            .filter { it.protocol == "file" }
            .map { File(it.toURI()) }
            .filter { it.isDirectory }
            .map { dir ->
                dir.listFiles()!!
                    .filter { it.name.endsWith(".bpmn") }
                    .map { it.name }
            }
            .toList()
            .flatten()
    }


}

private fun <JavaDelegate> Class<JavaDelegate>.register(processEngineConfiguration: ProcessEngineConfigurationImpl) {
    CamundaKtorFeature.logger.debug { "Register '${this.javaClass.simpleName.decapitalize()}' as camunda java delegate!" }
    processEngineConfiguration.beans[this.javaClass.simpleName.decapitalize()] = this.javaClass.kotlin.companionObject
}
