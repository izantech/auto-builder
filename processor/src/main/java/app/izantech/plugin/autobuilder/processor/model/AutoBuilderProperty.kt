package app.izantech.plugin.autobuilder.processor.model

import app.izantech.plugin.autobuilder.annotation.DefaultValue
import app.izantech.plugin.autobuilder.annotation.Lateinit
import app.izantech.plugin.autobuilder.annotation.UseBuilderSetter
import app.izantech.plugin.autobuilder.processor.AutoBuilderErrors
import app.izantech.plugin.autobuilder.processor.util.annotationOrNull
import app.izantech.plugin.autobuilder.processor.util.defaultValueOrNull
import app.izantech.plugin.autobuilder.processor.util.instanceOf
import app.izantech.plugin.autobuilder.processor.util.toKAnnotations
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class AutoBuilderProperty private constructor(
    val name: String,
    val typeName: TypeName,
    val annotations: Iterable<AnnotationSpec>,
    val isMutable: Boolean,
    val hasCustomDefaultValue: Boolean,
    val resolvedType: KSType,
    val defaultValue: String?,
    val isLateinit: Boolean,
    val useBuilderSetter: Boolean,
    val source: KSPropertyDeclaration,
): Comparable<AutoBuilderProperty> {
    companion object {
        context(Resolver, KSPLogger)
        fun from(declaration: KSPropertyDeclaration): AutoBuilderProperty? {
            if (!declaration.isPublic()) return null

            // Warn about mutable properties
            if (declaration.isMutable) {
                warn(AutoBuilderErrors.mutableProperty(declaration), declaration)
            }

            val resolvedType = declaration.type.resolve()
            val defaultValue = resolvedType.defaultValueOrNull
            val isLateinit = declaration.annotationOrNull<Lateinit>() != null
            val useBuilderSetter = declaration.annotationOrNull<UseBuilderSetter>() != null
            val hasCustomDefaultValue = declaration.getter?.annotationOrNull<DefaultValue>() != null
            if (!isLateinit && !resolvedType.isMarkedNullable && defaultValue == null && !hasCustomDefaultValue) {
                error(AutoBuilderErrors.uninitializedProperty(declaration), declaration)
                return null
            }

            return AutoBuilderProperty(
                name = declaration.simpleName.asString(),
                typeName = declaration.typeName,
                annotations = declaration.kAnnotations,
                isMutable = declaration.isMutable,
                hasCustomDefaultValue = hasCustomDefaultValue,
                resolvedType = resolvedType,
                defaultValue = defaultValue,
                isLateinit = isLateinit,
                useBuilderSetter = useBuilderSetter,
                source = declaration,
            )
        }
    }

    override fun compareTo(other: AutoBuilderProperty): Int {
        return name.compareTo(other.name)
    }
}

context(Resolver)
private val KSPropertyDeclaration.kAnnotations
    get() = getter
        ?.annotations
        ?.filterNot { it.instanceOf<DefaultValue>() }
        .toKAnnotations()

private val KSPropertyDeclaration.typeName: TypeName
    get() {
        val typeName = type.toTypeName()
        val annotations = typeName.annotations + getter?.returnType?.annotations?.toKAnnotations().orEmpty()
        return typeName.copy(annotations = annotations)
    }
