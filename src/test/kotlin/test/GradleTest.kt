package test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.io.File
import kotlin.test.Test

class GradleTest {

    @Test
    fun test() {
        val gradleVersion = GradleVersion.current().version
        val classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar).map { File(it) }

        val result = GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(File("test-project"))
            .withArguments("clean", "build", "--stacktrace", "--info")
            .withPluginClasspath(classpath)
            .build()


        try {
            result.task(":build")?.outcome?.let {
                if (it != TaskOutcome.SUCCESS) throw Exception("build failed")
            } ?: throw Exception("build failed")
        } catch (e: Exception) {
            println(result.output)
            throw Exception(e)
        }
    }

}