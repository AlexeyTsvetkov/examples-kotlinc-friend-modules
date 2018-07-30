import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    val outDir = File("build/tmp/kotlinc-out")
    if (outDir.exists()) {
        outDir.deleteRecursively()
    }
    outDir.mkdirs()

    val compileClasspath = File("build/compile-dependencies")
            .walk()
            .filter { it.isFile && it.extension == "jar" }
            .toList()

    val mainOutDir = outDir.resolve("main")
    compileToJvm(
            moduleName = "MyModule",
            sourceRoot = File("example/main"),
            outDir = mainOutDir,
            compileClasspath = compileClasspath
    ).takeIf { it == ExitCode.OK } ?: return


    val testOutDir = outDir.resolve("test")
    compileToJvm(
            moduleName = "MyModule",
            sourceRoot = File("example/test"),
            outDir = testOutDir,
            compileClasspath = listOf(mainOutDir) + compileClasspath,
            friendDirs = arrayOf(mainOutDir)
    )
}

private fun compileToJvm(
        moduleName: String,
        sourceRoot: File,
        outDir: File,
        compileClasspath: Collection<File>,
        friendDirs: Array<File> = emptyArray()
): ExitCode {
    val args = K2JVMCompilerArguments().apply {
        noStdlib = true
        this.moduleName = moduleName
        destination = outDir.canonicalPath
        classpath = compileClasspath.joinToString(File.pathSeparator) { it.canonicalPath }
        freeArgs = listOf(sourceRoot.canonicalPath)
        friendPaths = friendDirs.map { it.canonicalPath }.toTypedArray()
    }
    val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
    return K2JVMCompiler().exec(messageCollector, Services.EMPTY, args)
}