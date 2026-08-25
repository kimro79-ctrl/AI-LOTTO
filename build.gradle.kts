plugins {
    // 2026-08-27: 8.3.0은 16KB 페이지 크기 정렬을 지원하지 않아 8.7.3으로 올렸다.
    // (구글 공식 안내: AGP 8.5.1 이상부터 16KB 정렬을 자동으로 처리함)
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.20" apply false
}
