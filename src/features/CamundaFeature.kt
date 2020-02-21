package io.holunda.camunda.features

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KLogging
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.delegate.VariableListener
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator
import org.camunda.bpm.engine.repository.Deployment
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.regex.Pattern


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
            engineConfiguration.isJobExecutorActivate = configuration.isJobExecutorActivate
            engineConfiguration.idGenerator = StrongUuidGenerator()
            registerDelegates(engineConfiguration)

            return engineConfiguration.buildProcessEngine().also { deployment(it) }
        }

        private fun registerDelegates(processEngineConfiguration: ProcessEngineConfigurationImpl) {
            val reflections = Reflections("io.holunda.camunda")

            if(processEngineConfiguration.beans == null) {
                processEngineConfiguration.beans = mutableMapOf()
            }

            processEngineConfiguration.beans.putAll(reflections.getSubTypesOf(JavaDelegate::class.java)
                .map {
                    logger.debug { "Register '${it.simpleName.decapitalize()}' as camunda task listener!" }
                    Pair(it.simpleName.decapitalize(), it.kotlin.objectInstance)
                }.toMap())

            processEngineConfiguration.beans.putAll(reflections.getSubTypesOf(TaskListener::class.java)
                .map {
                    logger.debug { "Register '${it.simpleName.decapitalize()}' as camunda task listener!" }
                    Pair(it.simpleName.decapitalize(), it.kotlin.objectInstance)
                }.toMap())

            processEngineConfiguration.beans.putAll(reflections.getSubTypesOf(ExecutionListener::class.java)
                .map {
                    logger.debug { "Register '${it.simpleName.decapitalize()}' as camunda execution listener!" }
                    Pair(it.simpleName.decapitalize(), it.kotlin.objectInstance)
                }.toMap())

            processEngineConfiguration.beans.putAll(reflections.getSubTypesOf(VariableListener::class.java)
                .map {
                    logger.debug { "Register '${it.simpleName.decapitalize()}' as camunda variable listener!" }
                    Pair(it.simpleName.decapitalize(), it.kotlin.objectInstance)
                }.toMap())
        }

        private fun deployment(processEngine: ProcessEngine): Deployment {
            val deployment = processEngine.repositoryService.createDeployment()
            Reflections(ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forResource("processes"))
                    .setScanners(ResourcesScanner())
                 )
                .getResources(Pattern.compile(".*\\.bpmn"))
                .forEach { file -> deployment.addClasspathResource(file) }
            return deployment.deploy()
        }
    }

}
