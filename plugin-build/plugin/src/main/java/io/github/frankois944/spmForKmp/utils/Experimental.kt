package io.github.frankois944.spmForKmp.utils

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING, // or Level.ERROR
    message = "This API is experimental and may change in the future."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
public annotation class ExperimentalAPI
