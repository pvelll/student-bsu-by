import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Line tables for release builds too: they end up in the app's dSYM, so Crashlytics
            // shows Kotlin files and line numbers.
            freeCompilerArgs += "-Xadd-light-debug=enable"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.components.resources)
            implementation(libs.material.icons.extended)

            implementation(libs.jb.lifecycle.viewmodel)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.jb.navigation.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.ksoup)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.androidx.security.crypto)

            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.config)

            implementation(libs.play.review)
            implementation(libs.play.app.update)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        androidUnitTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }
    }
}

// Upload key for Google Play. keystore.properties (not in git) lives in the project root:
//   storeFile=release.keystore
//   storePassword=...
//   keyAlias=upload
//   keyPassword=...
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "github.sushkpavel.studentbsuby"
    compileSdk = 37

    defaultConfig {
        applicationId = "github.sushkpavel.studentbsuby"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0-alpha-1"
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        fatal += "NewApi"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Without keystore.properties the release build is signed with the debug key:
            // it installs locally, but Google Play rejects it.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

compose.resources {
    packageOfResClass = "github.sushkpavel.studentbsuby.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("coreLibraryDesugaring", libs.desugar.jdk.libs)
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

abstract class CheckJava21CollectionCalls : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classes: DirectoryProperty

    private val owners = setOf(
        "java/util/Collection", "java/util/List", "java/util/ArrayList", "java/util/AbstractList",
        "java/util/SequencedCollection", "java/util/SequencedSet", "java/util/SequencedMap",
        "java/util/LinkedHashSet", "java/util/LinkedHashMap", "java/util/SortedSet",
        "java/util/TreeSet", "java/util/SortedMap",
    )

    private val methods = setOf(
        "removeFirst", "removeLast", "getFirst", "getLast", "addFirst", "addLast", "reversed",
        "putFirst", "putLast", "sequencedKeySet", "sequencedValues", "sequencedEntrySet",
    )

    @TaskAction
    fun check() {
        val calls = mutableListOf<String>()
        classes.get().asFileTree.matching { include("**/*.class") }.forEach { file ->
            ClassReader(file.readBytes()).accept(object : ClassVisitor(Opcodes.ASM9) {
                private var className = ""

                override fun visit(
                    version: Int, access: Int, name: String, signature: String?,
                    superName: String?, interfaces: Array<out String>?,
                ) {
                    className = name.replace('/', '.')
                }

                override fun visitMethod(
                    access: Int, name: String, descriptor: String, signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor = object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(
                        opcode: Int, owner: String, method: String, methodDescriptor: String,
                        isInterface: Boolean,
                    ) {
                        if (owner in owners && method in methods)
                            calls += "$className.$name() calls ${owner.replace('/', '.')}.$method (Android 15+)"
                    }
                }
            }, ClassReader.SKIP_FRAMES)
        }
        if (calls.isNotEmpty())
            throw GradleException(
                "Java 21 collection methods crash with NoSuchMethodError below Android 15:\n" +
                        calls.distinct().joinToString("\n") { "  $it" }
            )
    }
}

listOf("Debug", "Release").forEach { variant ->
    val check = tasks.register<CheckJava21CollectionCalls>("check${variant}Java21CollectionCalls") {
        group = "verification"
        classes.set(layout.buildDirectory.dir("tmp/kotlin-classes/${variant.lowercase()}"))
        dependsOn("compile${variant}KotlinAndroid")
    }
    tasks.matching { it.name in setOf("assemble$variant", "bundle$variant", "check") }
        .configureEach { dependsOn(check) }
}

// Firebase is optional: the plugins are applied only when google-services.json is supplied,
// matching the fact that the config file is not checked into the repository.
if (file("google-services.json").exists()) {
    pluginManager.apply(libs.plugins.googleServices.get().pluginId)
    pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
}
