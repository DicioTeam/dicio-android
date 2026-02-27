import me.champeau.gradle.igp.gitRepositories
import org.eclipse.jgit.api.Git
import java.io.FileInputStream
import java.util.Properties

rootProject.name = "Dicio"
include(":app")
include(":skill")
// we use includeBuild here since the plugins are compile-time dependencies
includeBuild("sentences-compiler-plugin")
includeBuild("unicode-cldr-plugin")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // need to manually read version catalog because it is not available in settings.gradle.kts
    // this code is duplicate with the below but there is no way to avoid it...
    fun findInVersionCatalog(versionIdentifier: String): String {
        val regex = "^.*$versionIdentifier *= *\"([^\"]+)\".*$".toRegex()
        return File("gradle/libs.versions.toml")
            .readLines()
            .firstNotNullOf { regex.find(it)?.groupValues?.get(1) }
    }

    id("me.champeau.includegit") version findInVersionCatalog("includegitPlugin")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}


// All of the code below handles depending on libraries from git repos, in particular dicio-numbers
// and dicio-sentences-compiler. The git commits to checkout can be updated in the version catalog.
// If you want to use a local copy of the projects, you can add the following in `local.properties`:
// useLocalDicioLibraries=dicio-numbers:../dicio-numbers,dicio-sentences-compiler:../dicio-sentences-compiler

data class IncludeGitRepo(
    val name: String,
    val uri: String,
    val projectPath: String,
    val commit: String,
)

// need to manually read version catalog because it is not available in settings.gradle.kts
// this code is duplicate with the above but there is no way to avoid it...
fun findInVersionCatalog(versionIdentifier: String): String {
    val regex = "^.*$versionIdentifier *= *\"([^\"]+)\".*$".toRegex()
    return File("gradle/libs.versions.toml")
        .readLines()
        .firstNotNullOf { regex.find(it)?.groupValues?.get(1) }
}

// the list of repositories we want to include via git
val includeGitRepos: List<IncludeGitRepo> = listOf(
    IncludeGitRepo(
        name = "dicio-numbers",
        uri = "https://github.com/Stypox/dicio-numbers",
        projectPath = ":numbers",
        commit = findInVersionCatalog("dicioNumbers"),
    ),
    IncludeGitRepo(
        name = "dicio-sentences-compiler",
        uri = "https://github.com/Stypox/dicio-sentences-compiler",
        projectPath = ":sentences_compiler",
        commit = findInVersionCatalog("dicioSentencesCompiler"),
    ),
)

// read from local.properties whether the user wants to use a local clone of a library
private fun parseKeyValuePairs(input: String): Map<String, String> {
    if (input.isBlank()) {
        return mapOf()
    }
    val map = mutableMapOf<String, String>()
    val validKeys = includeGitRepos.map { it.name }.toMutableSet()
    for (pair in input.split(',')) {
        val parts = pair.split(":", limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid library specification in useLocalDicioLibraries: $pair")
        } else if (parts[0] !in validKeys) {
            throw IllegalArgumentException("Invalid or duplicate library name in useLocalDicioLibraries: ${parts[0]}")
        }
        validKeys.remove(parts[0])
        map[parts[0]] = parts[1]
    }
    return map
}
val localProperties: Properties = Properties().apply {
    try {
        load(FileInputStream(File(rootDir, "local.properties")))
    } catch (e: Throwable) {
        println("Warning: can't read local.properties: $e")
    }
}
val libsToUseLocally = parseKeyValuePairs(localProperties.getOrDefault("useLocalDicioLibraries", "").toString())

// finally actually include the libraries
for (repo in includeGitRepos) {
    if (repo.name in libsToUseLocally) {
        // user wants to use a local clone of this library
        includeBuild(libsToUseLocally[repo.name]!!) {
            dependencySubstitution {
                substitute(module("git.included.build:${repo.name}"))
                    .using(project(repo.projectPath))
            }
        }
    } else {
        // if the repo has already been cloned, the gitRepositories plugin is buggy and doesn't
        // fetch the remote repo before trying to checkout the commit (in case the commit changed),
        // and doesn't clone the repo again if the remote changed, so we need to do it manually
        val file = File("$rootDir/checkouts/${repo.name}")
        if (file.isDirectory) {
            val git = Git.open(file)
            val sameRemote = git.remoteList().call()
                .any { rem -> rem.urIs.any { uri -> uri.toString() == repo.uri } }
            if (sameRemote) {
                // the commit may have changed, fetch again
                git.fetch().call()
            } else {
                // the remote changed, delete the repository and start from scratch
                println("Git: remote for ${repo.name} changed, deleting the current folder")
                file.deleteRecursively()
            }
        }
    }
}

// tell gitRepositories to clone/update any repository that has not been already included locally
if (libsToUseLocally.size < includeGitRepos.size) {
    gitRepositories {
        for (repo in includeGitRepos) {
            if (repo.name !in libsToUseLocally) {
                include(repo.name) {
                    uri.set(repo.uri)
                    commit.set(repo.commit)
                    autoInclude.set(false)
                    includeBuild("") {
                        dependencySubstitution {
                            substitute(module("git.included.build:${repo.name}"))
                                .using(project(repo.projectPath))
                        }
                    }
                }
            }
        }
    }
}
