package app.izantech.plugin.autobuilder.processor.model

import app.izantech.plugin.autobuilder.processor.AutoBuilderErrors
import app.izantech.plugin.autobuilder.processor.util.findAnnotation
import app.izantech.plugin.autobuilder.processor.util.getArgument
import app.izantech.plugin.autobuilder.processor.util.toKAnnotations
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName

internal class AutoBuilderClass private constructor(
    val packageName: String,
    val name: String,
    val properties: ModelProperties,
    val annotations: ModelAnnotations,
    val className: ClassName,
    val defaultsClassName: ClassName,
    val defaultsMemberName: MemberName,
    val implClassName: ClassName,
    val builderClassName: ClassName,
) {
    companion object {
        context(resolver: Resolver, logger: KSPLogger)
        fun from(symbol: KSClassDeclaration): AutoBuilderClass? {
            // Check if the interface has type parameters (generics)
            if (symbol.typeParameters.isNotEmpty()) {
                logger.error(AutoBuilderErrors.hasGenericType(symbol), symbol)
                return null
            }

            // Check if the interface has properties.
            val annotation = symbol.findAnnotation("AutoBuilder") ?: return null
            val inheritedProperties = annotation.getArgument("inheritedProperties") as? Boolean ?: false
            val allowEmpty = annotation.getArgument("allowEmpty") as? Boolean ?: false
            val properties = symbol.getProperties(
                useInherited = inheritedProperties,
            )
            if (properties.none() && !allowEmpty) {
                logger.error(AutoBuilderErrors.emptyInterface(symbol), symbol)
                return null
            }

            val packageName = symbol.packageName.asString()
            val className = symbol.toClassName()
            val name = className.simpleNames.joinToString(separator = "_")
            val defaultsClassName = ClassName(packageName, "${name}Defaults")
            val defaultsMemberName = MemberName(packageName, defaultsClassName.simpleName)
            val implClassName = ClassName(packageName, "${name}Impl")
            val builderClassName = ClassName(packageName, "${name}Builder")

            return AutoBuilderClass(
                packageName = packageName,
                name = name,
                properties = properties,
                annotations = symbol.kAnnotations,
                className = className,
                defaultsClassName = defaultsClassName,
                defaultsMemberName = defaultsMemberName,
                implClassName = implClassName,
                builderClassName = builderClassName,
            )
        }
    }
}

private val KSClassDeclaration.kAnnotations
    get() = annotations.toKAnnotations()

context(resolver: Resolver, logger: KSPLogger)
private fun KSClassDeclaration.getProperties(useInherited: Boolean): ModelProperties {
    val properties = when {
        useInherited -> getAllProperties()
        else -> getDeclaredProperties()
    }
    return properties
        .mapNotNull { AutoBuilderProperty.from(it) }
        .toSortedSet()
}
