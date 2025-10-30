package app.izantech.plugin.autobuilder.annotation

/**
 * This annotation is used to generate a builder class for the annotated interface.
 *
 * @param inheritedProperties If true, the builder will include properties from super interfaces. By default is false since obtaining superclass properties is expensive.
 */
@Target(AnnotationTarget.CLASS)
annotation class AutoBuilder(
    val inheritedProperties: Boolean = false,
)

/**
 * This annotation is used to specify a default value for a property when generating a builder class.
 */
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class DefaultValue

/**
 * This annotation is used to specify a property as mandatory when generating a builder class.
 * That means the property must be initialized before the builder is built or an exception will be thrown.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Lateinit

/**
 * This annotation is used to force the use of the builder setter method instead of direct property assignment
 * in the builder DSL.
 *
 * When applied to a property, the property becomes private in the builder and you must use the
 * setPropertyName() method instead of direct assignment.
 *
 * Example:
 * ```kotlin
 * @AutoBuilder
 * interface Interaction {
 *     @UseBuilderSetter
 *     val onClick: () -> Unit
 *         @DefaultValue get() = {}
 * }
 *
 * // Usage:
 * val interaction = Interaction {
 *     // onClick = {} // This will not compile - property is private
 *     setOnClick { println("Clicked!") } // Must use this instead
 * }
 * ```
 *
 * This is useful for properties where you want to enforce a specific setter method,
 * particularly for lambda properties or when you need custom setter logic.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class UseBuilderSetter
