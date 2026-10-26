# 드라이브 뷰

경치 좋은 장소를 지도나 카테고리로 고르고, 사진과 거리·예상 시간을 확인한 뒤 차량 내비게이션으로 안내를 넘기는 목적지 추천 앱입니다.

## 실행 방법

1. Android Studio에서 이 프로젝트를 엽니다.
2. Pleos Connect 에뮬레이터를 실행합니다.
3. 실행 구성을 `app`으로 두고 실행합니다.

설치만 확인할 때는 에뮬레이터가 연결된 상태에서 아래를 실행합니다.

```
adb install -r apk/정경수_드라이브뷰_release.apk
```

첫 화면에서 지도 또는 카테고리로 장소를 고른 뒤, 장소 화면의 안내시작으로 차량 내비게이션에 경로를 넘깁니다.

## 테스트 환경

- Pleos Connect 에뮬레이터 (Pleos Connect v2.1, API 34)
- Android API 28 이상, Gradle 8.11.1, Kotlin
- 화면 방향: 가로
- 권한: `pleos.car.permission.NAVI_ROUTE_SEARCH`, `pleos.car.permission.NAVI_CUSTOM_MAP`

## APK

`apk/정경수_드라이브뷰_release.apk`
