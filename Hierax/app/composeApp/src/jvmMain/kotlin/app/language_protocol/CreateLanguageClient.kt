package app.language_protocol

import app.configurations.ProgrammingLanguage
import kotlinx.coroutines.CoroutineScope
import java.io.File

fun createLanguageClientForLanguage(language: ProgrammingLanguage, scope: CoroutineScope, projectRootPath: String): LspClientManager {
    when (language) {
        ProgrammingLanguage.Go -> {
            return LspClientManager(scope, "gopls", File(projectRootPath))
        }

        else -> throw Throwable("this language is not supported yet: " + language.name)
    }
}
