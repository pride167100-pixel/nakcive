package com.nakcive.app

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.nakcive.app.data.ThemePreferences

// 카카오맵 네이티브 앱 키. 공공데이터 서비스 키와 마찬가지로 개인 프로젝트라 소스에 직접 둔다.
// 저장소를 공개로 바꾸게 되면 이 값도 별도 설정 파일로 분리해야 한다.
private const val KAKAO_NATIVE_APP_KEY = "e2ebe37f1339fb3bda5f60c9250bc137"

class NakciveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, KAKAO_NATIVE_APP_KEY)
        ThemePreferences.init(this)
    }
}
