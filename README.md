# Hierax 

### How to use 
- clone and build the project with this branch.
- open the `Hierax/app/composeApp/src/jvmMain/kotlin/app/widgets/Ide.kt` file.
- execute the main function on this file.

  ### How to get lsp working
  - download and install the lsp files of your programming language. (only Go supported for now)
  - create a folder as the root of your project files. let's name it `Hierax go project`.
  - create your code files in your desired language.
  - copy the absolute path of the folder you created.
  - open the `Hierax/app/composeApp/src/jvmMain/kotlin/app/configurations/Constants.kt` file and paste the project root path as the value of the `ProjectRootPath` variable.
 
  ### How to add lsp support of a new programming language
  - open the `Hierax/app/composeApp/src/jvmMain/kotlin/app/configurations/Environment.kt`.
  - add your programming language to this class.
    ```kotlin
    open class ProgrammingLanguage(val name: String? = null) {
        object Kotlin: ProgrammingLanguage("kotlin")

        object Java: ProgrammingLanguage("java")

        object Go: ProgrammingLanguage("go")
    }
  ```
- open the `Hierax/app/composeApp/src/jvmMain/kotlin/app/language_protocol/CreateLanguageProtocol.kt` file.
- add your logic for opening or executing your lsp binary to this function.
  ```kotlin
  fun createLanguageClientForLanguage(language: ProgrammingLanguage, scope: CoroutineScope, projectRootPath: String): LspClientManager {
      when (language) {
          ProgrammingLanguage.Go -> {
              return LspClientManager(scope, "gopls", File(projectRootPath))
          }

          else -> throw Throwable("this language is not supported yet: " + language.name)
      }
  }
  ``` 
