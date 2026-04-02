package app.izantech.plugin.autobuilder.processor.util

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import java.math.BigDecimal
import java.math.BigInteger

context(resolver: Resolver)
internal val KSType.isPrimitive
    get() = this == resolver.builtIns.intType ||
            this == resolver.builtIns.doubleType ||
            this == resolver.builtIns.floatType ||
            this == resolver.builtIns.longType ||
            this == resolver.builtIns.shortType ||
            this == resolver.builtIns.byteType ||
            this == resolver.builtIns.charType ||
            this == resolver.builtIns.booleanType

context(resolver: Resolver)
internal val KSType.isArray: Boolean
    get() {
        if (starProjection().isAssignableFrom(resolver.builtIns.arrayType)) {
            return true
        }

        return makeNotNullable().run {
            instanceOf<IntArray>()
                    || instanceOf<FloatArray>()
                    || instanceOf<DoubleArray>()
                    || instanceOf<LongArray>()
                    || instanceOf<ShortArray>()
                    || instanceOf<ByteArray>()
                    || instanceOf<BooleanArray>()
                    || instanceOf<CharArray>()
        }
    }


context(resolver: Resolver)
internal val KSType.isString
    get() = resolver.builtIns.stringType.isAssignableFrom(this)

context(resolver: Resolver)
internal val KSType.canBeConst
    get() = isPrimitive || isString

context(resolver: Resolver)
internal val KSType.defaultValueOrNull
    get() = when (val projection = starProjection()) {
        resolver.builtIns.anyType -> "Any()"
        resolver.builtIns.unitType -> "Unit"
        resolver.builtIns.numberType, resolver.builtIns.byteType, resolver.builtIns.shortType, resolver.builtIns.intType -> "0"
        resolver.builtIns.longType -> "0L"
        resolver.builtIns.floatType -> "0f"
        resolver.builtIns.doubleType -> "0.0"
        resolver.builtIns.charType -> "'\u0000'"
        resolver.builtIns.booleanType -> "false"
        resolver.builtIns.stringType -> "\"\""
        resolver.builtIns.arrayType -> "emptyArray()"
        else -> when {
            resolver.builtIns.arrayType.isAssignableFrom(projection) -> "emptyArray()"
            projection.instanceOf<List<*>>() -> "emptyList()"
            projection.instanceOf<Set<*>>() -> "emptySet()"
            projection.instanceOf<Map<*, *>>() -> "emptyMap()"
            projection.instanceOf<IntArray>() -> "intArrayOf()"
            projection.instanceOf<FloatArray>() -> "floatArrayOf()"
            projection.instanceOf<DoubleArray>() -> "doubleArrayOf()"
            projection.instanceOf<LongArray>() -> "longArrayOf()"
            projection.instanceOf<ShortArray>() -> "shortArrayOf()"
            projection.instanceOf<ByteArray>() -> "byteArrayOf()"
            projection.instanceOf<BooleanArray>() -> "booleanArrayOf()"
            projection.instanceOf<CharArray>() -> "charArrayOf()"
            projection.instanceOf<BigDecimal>() -> "BigDecimal.ZERO"
            projection.instanceOf<BigInteger>() -> "BigInteger.ZERO"
            projection.instanceOf("androidx.compose.ui.text.AnnotatedString") -> "AnnotatedString(\"\")"
            projection.instanceOf<CharSequence>() -> "\"\""
            projection.isMarkedNullable -> "null"
            else -> null
        }
    }

context(resolver: Resolver)
internal fun KSType.instanceOf(name: String) =
    instanceOf(resolver.getClassDeclarationByName(name))

context(resolver: Resolver)
internal inline fun <reified T> KSType.instanceOf() =
    instanceOf(resolver.getClassDeclarationByName<T>())

context(resolver: Resolver)
internal fun KSType.instanceOf(declaration: KSClassDeclaration?) =
    declaration?.asStarProjectedType()?.isAssignableFrom(this) == true

context(resolver: Resolver)
internal inline fun <reified T> KSAnnotation.instanceOf() =
    annotationType.resolve().instanceOf<T>()

fun KSAnnotated.hasAnnotation(name: String): Boolean =
    annotations.any { it.shortName.asString() == name }

fun KSAnnotated.findAnnotation(name: String): KSAnnotation? =
    annotations.firstOrNull { it.shortName.asString() == name }

fun KSAnnotation.getArgument(name: String): Any? =
    arguments.firstOrNull { it.name?.asString() == name }?.value
