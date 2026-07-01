import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.Node

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.kotlin.ksp)
}

android {
  namespace = "org.tpmobile.minghuiradio"
  compileSdk {
    version = release(36) {
      minorApiLevel = 1
    }
  }

  defaultConfig {
    applicationId = "org.tpmobile.minghuiradio"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = getVersionNameFromResources()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug {
      resValue("string", "app_name", "MHRadio")
      buildConfigField("boolean", "MY_DEBUG", "true")
      applicationIdSuffix = ".debug"
    }
    release {
      resValue("string", "app_name", "明慧广播")
      buildConfigField("boolean", "MY_DEBUG", "false")

      isDebuggable = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
    resValues = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.foundation.layout)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.media3.session)
  testImplementation(libs.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.ui.compose)
  implementation(libs.androidx.media3.ui.compose.material3)

  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)

  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)

  implementation(libs.jsoup)
  implementation(libs.gson)


  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.navigation.compose)

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.guava)

  implementation(libs.accompanist.permissions)
}

fun getVersionNameFromResources(): String {
  val stringsFile = file("src/main/res/values/strings.xml")
  val xml = XmlSlurper().parse(stringsFile)
  var versionName: String? = null
  //println(xml.childNodes().forEach { node -> println((node as Node).attributes()) })
  xml.childNodes().forEach { node ->
    (node as Node).attributes().forEach { ss ->
      if (ss.value == "version_name") {
        versionName = node.text().toString()
      }
    }
  }
  if (versionName == null) {
    throw GradleException("String resource 'version_name' not found in strings.xml!")
  }
  println("versionName: $versionName")
  return versionName
}