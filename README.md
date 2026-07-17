# Jerusalem Mix

Galaxy S26용 Yamaha CL5 원격 제어 실험 앱입니다.

## 구조

- `docs/`: GitHub Pages와 Android APK가 함께 사용하는 화면 원본
- `app/`: 공용 화면을 WebView로 표시하고 CL5 TCP 49280 통신을 담당하는 Android 앱
- 웹판: 모의 데이터만 사용하며 실제 콘솔 명령을 보내지 않음
- APK: 공용 화면을 앱 내부에 포장하므로 인터넷 없이 실행 가능

## 안전 상태

공식 Yamaha 자료로 확인된 Fader, Mute(Fader On), Mix Send만 제어 허용 대상입니다.
Gain, PAN, PEQ 경로는 실험 잠금 뒤에 있으며 현장 검증이 필요합니다.
`+48V` 읽기와 쓰기는 현장 프로토콜 검증 전까지 코드에서 차단되어 있습니다.

## GitHub Pages

저장소 Settings → Pages → Deploy from a branch에서 배포 브랜치의 `/docs` 폴더를 선택합니다.
서비스 워커가 새 파일을 확인하므로 웹 디자인판은 다음 접속 때 최신 버전으로 갱신됩니다.

## 빌드

JDK 17 및 Android SDK 36 환경에서:

```sh
gradle testDebugUnitTest lintDebug assembleDebug
```

공용 웹 UI 계산 테스트:

```sh
deno test web-tests/core_test.js
```

APK 출력: `app/build/outputs/apk/debug/app-debug.apk`
