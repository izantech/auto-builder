@file:OptIn(DelicateKotlinPoetApi::class)

package app.izantech.plugin.autobuilder.processor

import app.izantech.plugin.autobuilder.processor.model.AutoBuilderClass
import app.izantech.plugin.autobuilder.processor.model.ModelProperties
import app.izantech.plugin.autobuilder.processor.util.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ksp.writeTo

private val SuppressedWarnings = arrayOf(
    "ConstPropertyName",
    "KotlinRedundantDiagnosticSuppress",
    "MemberVisibilityCanBePrivate",
    "NEWER_VERSION_IN_SINCE_KOTLIN",
    "RedundantNullableReturnType",
    "RedundantVisibilityModifier",
    "unused",
)

internal class ModelGenerator(
    private val resolver: Resolver,
    private val codeGenerator: CodeGenerator,
) : KSDefaultVisitor<AutoBuilderClass, Unit>() {
    override fun defaultHandler(node: KSNode, data: AutoBuilderClass) = Unit

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: AutoBuilderClass
    ) {
        val originatingFile = classDeclaration.containingFile

        FileSpec.builder(data.packageName, "${data.name}.defaults")
            .suppressWarnings(*SuppressedWarnings)
            .addType(generateDefaultsObject(originatingFile, data))
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = true))

        FileSpec.builder(data.packageName, "${data.name}.builder")
            .suppressWarnings(*SuppressedWarnings)
            .addType(generateImplementation(originatingFile, data))
            .addType(generateBuilder(originatingFile, data))
            .addFunctions(generateModelExtensions(originatingFile, data))
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = true))
    }

    // region Companion
    private fun generateDefaultsObject(
        originatingFile: KSFile?,
        data: AutoBuilderClass,
    ) = with(data) {
        TypeSpec.objectBuilder(defaultsClassName)
            .addSuperinterface(data.className)
            .addProperties(generateDefaultsProperties(properties))
            .addOptionalOriginatingKSFile(originatingFile)
            .build()
    }

    private fun generateDefaultsProperties(
        properties: ModelProperties,
    ) = properties.mapNotNull { property ->
        if (property.hasCustomDefaultValue) return@mapNotNull null
        PropertySpec.builder(property.name, property.typeName)
            .let {
                val defaultValue = property.defaultValue
                if (defaultValue == null || property.isLateinit) {
                    it.hidden()
                } else {
                    it.initializer(defaultValue)
                        .addAnnotations(property.annotations)
                }
            }
            .mutable(property.isMutable)  // Preserve mutability from interface
            .addModifiers(KModifier.OVERRIDE)
            .build()
    }

    // endregion

    // region ModelImpl
    private fun generateImplementation(
        originatingFile: KSFile?,
        data: AutoBuilderClass,
    ) = with(data) {
        TypeSpec.classBuilder(implClassName)
            .addAnnotations(annotations)
            .addModifiers(KModifier.PRIVATE)
            .addSuperinterface(className)
            .primaryConstructor(generateImplementationPrimaryConstructor(properties))
            .addProperties(generateImplementationProperties(properties))
            .addFunction(generateImplementationEquals(properties, implClassName))
            .addFunction(generateImplementationHashCode(properties))
            .addFunction(generateImplementationToString(properties, className))
            .addOptionalOriginatingKSFile(originatingFile)
            .build()
    }

    private fun generateImplementationPrimaryConstructor(properties: ModelProperties) =
        FunSpec.constructorBuilder()
            .addParameters(properties.map { property ->
                ParameterSpec.builder(property.name, property.typeName).build()
            })
            .build()

    private fun generateImplementationProperties(properties: ModelProperties) =
        properties.map { property ->
            PropertySpec.builder(property.name, property.typeName)
                .initializer(property.name)
                .addAnnotations(property.annotations)
                .addModifiers(KModifier.OVERRIDE)
                .mutable(property.isMutable)
                .build()
        }

    private fun generateImplementationEquals(
        properties: ModelProperties,
        implClassName: ClassName,
    ) = with(resolver) {
        val args = properties.joinToString(" &&\n\t") {
            if (it.resolvedType.isArray) {
                "this.${it.name}.contentEquals(other.${it.name})"
            } else {
                "this.${it.name} == other.${it.name}"
            }
        }
        FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY.copy(nullable = true))
            .returns(Boolean::class)
            .addStatement("if (this === other) return true")
            .addStatement("if (javaClass != other?.javaClass) return false")
            .addStatement("other as %T", implClassName)
            .addStatement("return %L", args)
            .build()
    }

    private fun generateImplementationHashCode(properties: ModelProperties): FunSpec = with(resolver) {
        val javaHash = ClassName("java.util", "Objects").member("hash")

        // Build the hash arguments, handling arrays specially
        val hashArgs = properties.joinToString(",\n    ") { prop ->
            when {
                prop.resolvedType.isArray -> {
                    // For arrays, use contentHashCode() to properly hash the contents
                    if (prop.typeName.isNullable) {
                        "${prop.name}?.contentHashCode() ?: 0"
                    } else {
                        "${prop.name}.contentHashCode()"
                    }
                }
                else -> prop.name
            }
        }

        return FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Int::class)
            .apply {
                if (properties.isEmpty()) {
                    addStatement("return 0")
                } else {
                    addStatement("return %M(\n    %L\n)", javaHash, hashArgs)
                }
            }
            .build()
    }

    private fun generateImplementationToString(
        properties: ModelProperties,
        className: ClassName,
    ) = with(resolver) {
        val args = buildList {
            add("append(\"${className.simpleNames.joinToString(separator = ".")}(\")")
            addAll(properties.mapIndexed { index, it ->
                val content = if (it.resolvedType.isArray) {
                    "{${it.name}.contentToString()}"
                } else {
                    it.name
                }
                val trailingComma = if (index < properties.count() - 1) ", " else ""
                "append(\"${it.name}=\$${content}${trailingComma}\")"
            })
            add("append(\")\")")
        }
        val formattedArgs = args.joinToString(
            prefix = "  ",
            separator = "\n  "
        )

        FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return buildString {\n%L\n}", formattedArgs)
            .build()
    }
    // endregion

    // region Builder
    private fun generateBuilder(
        originatingFile: KSFile?,
        data: AutoBuilderClass,
    ) = with(data) {
        val builderProperties = generateBuilderProperties(properties, defaultsMemberName)

        TypeSpec.classBuilder(builderClassName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addAnnotation(PublishedApi::class)
                    .addParameter(
                        ParameterSpec.builder("source", className.copy(nullable = true))
                            .defaultValue("null")
                            .build()
                    )
                    .build()
            )
            .addProperties(builderProperties)
            .addFunctions(generateBuilderJavaMethods(builderClassName, properties))
            .addFunction(
                generateBuildFunction(className, implClassName, builderClassName, properties, builderProperties, defaultsMemberName)
            )
            .addOptionalOriginatingKSFile(originatingFile)
            .build()
    }

    private fun generateBuildFunction(
        className: ClassName,
        implClassName: ClassName,
        builderClassName: ClassName,
        properties: ModelProperties,
        builderProperties: List<PropertySpec>,
        defaultsMemberName: MemberName,
    ): FunSpec {
        return FunSpec.builder("build")
            .returns(className)
            .apply {
                val hasCustomDefaults = properties.any { it.hasCustomDefaultValue }
                if (!hasCustomDefaults) {
                    val constructorCode = CodeBlock.builder().apply {
                        add("return %T(\n", implClassName)
                        indent()
                        properties.forEach { property ->
                            when {
                                property.isLateinit -> {
                                    val errorMessage = AutoBuilderErrors.uninitializedLateinit(property.source)
                                    add("%L = %L ?: throw UninitializedPropertyAccessException(%S),\n", property.name, property.name, errorMessage)
                                }
                                else -> {
                                    add("%L = %L,\n", property.name, property.name)
                                }
                            }
                        }
                        unindent()
                        add(")\n")
                    }.build()
                    addCode(constructorCode)
                } else {
                    val builderPropertyMap = builderProperties.associateBy { it.name }
                    val builderSimpleName = builderClassName.simpleName

                    addStatement("val resolvedValues = mutableMapOf<String, Any?>()")
                    addStatement("val resolving = mutableSetOf<String>()")
                    addStatement("val computations = mutableMapOf<String, () -> Any?>()")
                    addCode("@Suppress(%S)\n", "UNCHECKED_CAST")
                    addCode("fun <T> resolve(name: String): T {\n")
                    addCode("  val cached = resolvedValues[name]\n")
                    addCode("  if (cached != null || resolvedValues.containsKey(name)) return cached as T\n")
                    addCode("  if (!resolving.add(name)) {\n")
                    addStatement("    throw IllegalStateException(%P)", "Circular property default detected for \$name")
                    addCode("  }\n")
                    addCode("  val value = computations.getValue(name)() as T\n")
                    addCode("  resolving.remove(name)\n")
                    addCode("  resolvedValues[name] = value as Any?\n")
                    addCode("  return value\n")
                    addCode("}\n")

                    properties.forEach { property ->
                        val builderProperty = builderPropertyMap.getValue(property.name)
                        val builderIsNullable = builderProperty.type.isNullable
                        val builderAccess = CodeBlock.of("this@%L.%L", builderSimpleName, property.name)

                        val computation = when {
                            property.isLateinit -> {
                                val errorMessage = AutoBuilderErrors.uninitializedLateinit(property.source)
                                CodeBlock.of("%L ?: throw UninitializedPropertyAccessException(%S)", builderAccess, errorMessage)
                            }
                            property.hasCustomDefaultValue && builderIsNullable -> {
                                val contextBlock = CodeBlock.builder().apply {
                                    beginControlFlow("run")
                                    beginControlFlow("val context = object : %T", className)
                                    properties.forEach { other ->
                                        if (other != property) {
                                            addStatement(
                                                "override val %L: %T get() = resolve(%S)",
                                                other.name,
                                                other.typeName,
                                                other.name,
                                            )
                                        }
                                    }
                                    endControlFlow()
                                    addStatement("context.%L", property.name)
                                    endControlFlow()
                                }.build()
                                CodeBlock.of("%L ?: %L", builderAccess, contextBlock)
                            }
                            property.hasCustomDefaultValue -> builderAccess
                            builderIsNullable && property.defaultValue != null -> {
                                CodeBlock.of("%L ?: %M.%L", builderAccess, defaultsMemberName, property.name)
                            }
                            else -> builderAccess
                        }

                        val lambdaBlock = CodeBlock.builder().apply {
                            add("computations[%S] = {\n", property.name)
                            indent()
                            add("%L\n", computation)
                            unindent()
                            add("}\n")
                        }.build()

                        addCode(lambdaBlock)
                    }

                    val constructorCode = CodeBlock.builder().apply {
                        add("return %T(\n", implClassName)
                        indent()
                        properties.forEach { property ->
                            val builderProperty = builderPropertyMap.getValue(property.name)
                            when {
                                property.hasCustomDefaultValue -> {
                                    add("%L = resolve(%S),\n", property.name, property.name)
                                }
                                property.isLateinit -> {
                                    val errorMessage = AutoBuilderErrors.uninitializedLateinit(property.source)
                                    add("%L = %L ?: throw UninitializedPropertyAccessException(%S),\n", property.name, builderProperty.name, errorMessage)
                                }
                                else -> {
                                    add("%L = %L,\n", property.name, builderProperty.name)
                                }
                            }
                        }
                        unindent()
                        add(")\n")
                    }.build()
                    addCode(constructorCode)
                }
            }
            .build()
    }

    private fun generateBuilderProperties(
        properties: ModelProperties,
        defaultsMemberName: MemberName,
    ) = properties.map { property ->
        val isLateinit = property.isLateinit
        val hasDefaultValue = property.defaultValue != null || property.hasCustomDefaultValue
        // Properties with custom defaults need to be nullable in the builder since we defer evaluation
        val needsNullableType = isLateinit || !hasDefaultValue || property.hasCustomDefaultValue
        val typeName = property.typeName.runIf(needsNullableType) { copy(nullable = true) }
        PropertySpec.builder(property.name, typeName)
            .mutable()
            .runIf(property.useBuilderSetter) {
                addModifiers(KModifier.PRIVATE)
                addKdoc("Use set%L() method to set this property.", property.name.replaceFirstChar { it.uppercase() })
            }
            .let {
                if (!isLateinit && hasDefaultValue) {
                    // For properties with custom defaults, don't evaluate them at builder initialization
                    // They will be evaluated at build time if needed
                    if (property.hasCustomDefaultValue) {
                        // Only initialize from source, don't call the defaults object's getter
                        it.initializer("source?.%L", property.name)
                    } else {
                        // For inferred defaults, use the existing logic
                        val code = buildString {
                            if (property.typeName.isNullable) {
                                append("if (source != null) source.%L else ")
                            } else {
                                append("source?.%L ?: ")
                            }
                            append("%M.")
                            append(property.name)
                        }
                        it.initializer(code, property.name, defaultsMemberName)
                    }
                } else {
                    it.initializer("source?.%L", property.name)
                }
            }
            .setter(FunSpec.setterBuilder().addAnnotation(JvmSynthetic::class).build())
            .addAnnotations(property.annotations)
            .build()
    }

    private fun generateBuilderJavaMethods(
        builderClassName: ClassName,
        properties: ModelProperties
    ): List<FunSpec> {
        return properties.map { property ->
            FunSpec.builder("set${property.name.capitalizeCompat()}")
                .addParameter(property.name, property.typeName)
                .runIf(!property.useBuilderSetter) {
                    addAnnotation(hideFromKotlinAnnotation())
                }
                .addStatement(
                    "return %M { this.${property.name} = ${property.name} }",
                    MemberName("kotlin", "apply")
                )
                .returns(builderClassName)
                .build()
        }
    }
    // endregion

    // region Model extensions
    private fun generateModelExtensions(
        originatingFile: KSFile?,
        data: AutoBuilderClass,
    ) = with(data) {
        val initLambdaTypeName = LambdaTypeName.get(receiver = builderClassName, returnType = UNIT)
        val initLambdaParameter = ParameterSpec
            .builder("init", initLambdaTypeName)
            .defaultValue("%L", "{}")
            .build()

        val initializerFunction = FunSpec.builder(name)
            .addKdoc("@see %T", className)
            .addAnnotation(JvmSynthetic::class)
            .addModifiers(KModifier.INLINE)
            .addParameter(initLambdaParameter)
            .returns(className)
            .addStatement(
                "return %T().apply(${initLambdaParameter.name}).build()",
                builderClassName
            )
            .addOptionalOriginatingKSFile(originatingFile)
            .build()

        val copyFunction = FunSpec.builder("copy")
            .receiver(className)
            .addKdoc("@see %T", className)
            .addAnnotation(JvmSynthetic::class)
            .addModifiers(KModifier.INLINE)
            .addParameter(initLambdaParameter)
            .returns(className)
            .addStatement(
                "return %T(this).apply(${initLambdaParameter.name}).build()",
                builderClassName
            )
            .addOptionalOriginatingKSFile(originatingFile)
            .build()

        listOf(initializerFunction, copyFunction)
    }
    // endregion
}
