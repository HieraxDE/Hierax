package app.configurations

import app.widgets.CodeExecutor

class Environment() {
    var programmingLanguage: ProgrammingLanguage = ProgrammingLanguage.Go
    var compilerPath = previewCompilerPath()
    var lspPath = previewLspPath()
    val projectRootPath = ProjectRootPath
    var customCodeExecutors = arrayListOf<CodeExecutor>()
    var selectedCustomCodeExecutorIndex = 0
}

open class ProgrammingLanguage(val name: String? = null) {
    object Kotlin: ProgrammingLanguage("kotlin")

    object Java: ProgrammingLanguage("java")

    object Go: ProgrammingLanguage("go")
}

fun Environment.previewCompilerPath(): String {
    return when (programmingLanguage) {
        ProgrammingLanguage.Kotlin -> KotlinCompilerBinaryPath
        ProgrammingLanguage.Java -> JavaCompilerBinaryPath
        ProgrammingLanguage.Go -> GoCompilerBinaryPath
        else -> ""
    }
}

fun Environment.previewLspPath(): String {
    return when (programmingLanguage) {
        ProgrammingLanguage.Kotlin -> KotlinLspBinaryPath
        ProgrammingLanguage.Java -> JavaLspBinaryPath
        ProgrammingLanguage.Go -> GoLspBinaryPath
        else -> ""
    }
}
