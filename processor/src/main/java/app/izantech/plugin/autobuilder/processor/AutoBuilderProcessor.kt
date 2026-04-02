package app.izantech.plugin.autobuilder.processor

import app.izantech.plugin.autobuilder.annotation.AutoBuilder
import app.izantech.plugin.autobuilder.processor.model.AutoBuilderClass
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class AutoBuilderProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver) = with(resolver) {
        with(environment.logger) {
            val (validatedSymbols, deferredSymbols) = getSymbolsDefinitions(resolver)
                .partition(KSClassDeclaration::validate)

            val isJvm = environment.platforms.any { it.platformName == "JVM" }
            val modelGenerator = ModelGenerator(resolver, environment.codeGenerator, isJvm)
            validatedSymbols.forEach { symbol ->
                val autoBuilderClass = AutoBuilderClass.from(symbol)
                if (autoBuilderClass != null) {
                    modelGenerator.visitClassDeclaration(symbol, autoBuilderClass)
                }
            }

            deferredSymbols
        }
    }

    private fun getSymbolsDefinitions(resolver: Resolver) = with(resolver) {
        getSymbolsWithAnnotation(AutoBuilder::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter { symbol ->
                // Check if the definition is a public interface.
                val isPublicInterface = symbol.classKind == ClassKind.INTERFACE && symbol.isPublic()
                if (!isPublicInterface) {
                    environment.logger.error(AutoBuilderErrors.notPublicInterface(symbol), symbol)
                    return@filter false
                }
                true
            }
    }
}
