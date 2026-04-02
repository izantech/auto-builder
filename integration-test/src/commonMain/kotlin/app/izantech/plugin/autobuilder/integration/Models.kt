package app.izantech.plugin.autobuilder.integration

import app.izantech.plugin.autobuilder.annotation.AutoBuilder
import app.izantech.plugin.autobuilder.annotation.DefaultValue
import app.izantech.plugin.autobuilder.annotation.Lateinit

@AutoBuilder
interface SimpleModel {
    val name: String
    val age: Int
}

@AutoBuilder(allowEmpty = true)
interface EmptyModel

@AutoBuilder
interface ModelWithLateinit {
    val required: String
    @Lateinit val lateinitProp: Exception
}

@AutoBuilder
interface ModelWithDefaults {
    val label: String
    val count: Int
        @DefaultValue get() = 42
    val enabled: Boolean
}
