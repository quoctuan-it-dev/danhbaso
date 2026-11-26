import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.quoctuan.danhbaso"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quoctuan.danhbaso"
        minSdk = 26
        targetSdk = 36
        // Mỗi lần phát hành bản mới: tăng versionCode lên 1 và đổi versionName (1.0 -> 1.1 ...)
        versionCode = 23
        versionName = "1.3.6"
        // Ngày phát hành = ngày build file APK (tự động, không cần sửa tay)
        buildConfigField(
            "String", "RELEASE_DATE",
            "\"" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\""
        )
    }

    buildTypes {
        release {
            // Bản release được tối ưu (R8) nên chạy mượt hơn nhiều so với bản debug.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TẠM THỜI ký bằng khóa debug để cài thử. Khi phát hành chính thức sẽ đổi sang khóa riêng (.jks).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Đặt tên file APK có số phiên bản, ví dụ: DanhBaSo-v1.0.1-release.apk
    applicationVariants.all {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "DanhBaSo-v${variant.versionName}-${variant.buildType.name}.apk"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Kiểm thử tự động (chạy trên máy tính, không cài lên điện thoại)
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
