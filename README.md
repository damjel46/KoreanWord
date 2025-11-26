# KoreanWord
문해력테스트


🛠 사용 기술 스택 (Tech Stack)
Language: Java

Platform: Android (Min SDK: 24)

UI: XML Layout (ScrollView, FrameLayout, Material Components)

Data: CSV Parsing, SharedPreferences (Local Storage)

IDE: Android Studio

✨ 주요 기능 (Key Features)
CSV 기반 퀴즈: 간편하게 단어 데이터를 추가/수정 가능.

무한 랜덤 학습: 순서를 외우지 않도록 매번 섞이는 문제.

학습 모드: 전체 단어 또는 즐겨찾기한 단어만 집중 학습 가능.

📝 개발 일지 (Dev Log)

📅 2025-11-24: 프로젝트 기획 및 핵심 로직 구현
기획: 한국인의 문해력 향상을 위한 '초성 단어 퀴즈' 어플리케이션 컨셉 수립.

데이터 구축: CSV 파일을 활용한 문제 은행(initial, word, mean, example) 구조 설계.

기능 구현:

CSV 파일 파싱 및 데이터 모델(WordItem) 연동.

AssetManager를 통한 파일 읽기 및 문제 출력 로직 구현.

기본적인 정답 확인 및 오답 처리 로직 작성.

📅 2025-11-25: UI/UX 개선 및 예외 처리
UI 개선:

화면 크기에 대응하기 위한 ScrollView 및 ConstraintLayout(또는 FrameLayout) 적용.

사용자 편의를 위해 키보드 '완료(Enter)' 키 입력 시 자동 제출 기능 추가.

데이터 처리: CSV 구분자를 쉼표(,)에서 버티컬 바(|)로 변경하여 예문 내 쉼표 충돌 문제 해결.

기능 분리: '힌트 보기', '정답 보기', '제출' 버튼의 역할 명확화 및 레이아웃 재배치.

📅 2025-11-26: 시각적 피드백 및 애니메이션 적용
피드백 강화:

Toast 메시지를 제거하고 화면 내 TextView를 통해 즉각적인 피드백 제공.

오답 시 카드가 흔들리는 애니메이션(shake.xml) 및 빨간색 테두리 효과 적용.

게임 요소 추가:

현재 문제 번호 표시 및 진행 상황 UI 적용.

'정답 보기' 사용 시 점수 미인정(패스) 처리 로직 구현.

📅 2025-11-27: 게임성 강화 (무한 모드 & 연속 정답)
로직 변경:

고정된 순서 출제 방식에서 Collections.shuffle을 이용한 무한 랜덤 출제 방식으로 변경.

연속 정답(Streak) 카운터 기능 추가 (오답 시 0으로 초기화).

어뷰징 방지: 정답을 미리 보고 제출할 경우 연속 정답 횟수가 오르지 않도록 플래그(isAnswerRevealed) 처리.

📅 2025-11-28: 데이터 영구 저장 및 심화 학습 기능
저장소 연동: SharedPreferences를 활용하여 앱 종료 후에도 데이터 유지.

즐겨찾기(Bookmark):

잘 안 외워지는 단어를 별표(⭐)로 저장하는 기능 구현.

'학습 모드 스위치' 추가: 전체 단어 vs 즐겨찾기 단어 선택 학습 기능.

단어 제외(Exclude):

너무 쉬운 단어는 휴지통(🗑️) 버튼으로 영구 삭제하는 기능 구현.

리셋 기능: 제외된 단어를 유지한 채 순서만 다시 섞는 '처음부터 다시 시작' 기능 추가.

📅 2025-11-29: 다크 모드 지원 및 최종 디자인 폴리싱
테마 적용:

눈의 피로를 줄이는 다크 모드(Dark Mode) 완벽 지원.

values-night/colors.xml을 활용한 자동 색상 전환 시스템 구축.

디자인 고도화:

파스텔 톤 컬러 팔레트 적용 (가독성 및 심미성 강화).

Material Design 가이드라인을 준수한 버튼 및 레이아웃 간격(Padding/Margin) 조정.

화면 중앙 정렬 및 시선 처리를 고려한 요소 배치 수정 (하단 여백 활용).
📅 2025-11-30: 데이터 영구 저장(Persistence) 구현
SharedPreferences 도입:

앱 종료 후에도 데이터가 유지되도록 저장소 연동.

제외된 단어(Excluded): Set<String> 형태로 저장하여 앱 재실행 시 해당 단어가 로드되지 않도록 필터링 로직 구현.

즐겨찾기(Bookmarks): 즐겨찾기 상태를 저장하여 재실행 시에도 별표(⭐)가 유지되도록 구현.

로직 최적화: readCsvFile 시 저장된 데이터를 대조하여 리스트를 구성하도록 수정.

📅 2025-12-01: 다크 모드(Dark Mode) 및 UI 고도화
테마 시스템 구축:

res/values/colors.xml (라이트 모드)와 res/values-night/colors.xml (다크 모드) 분리.

하드코딩된 색상 코드를 모두 제거하고 @color/resource_name 형태로 리팩토링.

다크 모드 시 눈의 피로를 줄이는 파스텔 톤(Pastel Tone) 컬러 팔레트 적용.

UI 레이아웃 개선:

제출 버튼: 직관적인 정사각형 형태로 변경 및 입력창 우측 배치.

하단 버튼: 힌트/정답 버튼의 너비를 조절(2/3 사이즈)하여 심미성 확보.

피드백 메시지: 시선 처리를 위해 카드 상단(연속 정답 위치)으로 재배치.

📅 2025-12-02: 앱 구조 개편 (Navigation & Home)
HomeActivity 생성:

앱의 진입점(Launcher)을 MainActivity에서 HomeActivity로 변경.

그룹별 학습: 초성 그룹(ㄱ-ㅁ, ㅂ-ㅊ, ㅋ-ㅎ)을 선택하여 학습하는 기능 추가.

모드 선택: '전체 랜덤 풀기'와 '즐겨찾기만 학습' 버튼 구현.

MainActivity 리팩토링:

기존의 모드 스위치(Switch)를 제거하고 Intent로 전달받은 모드(isBookmarkMode, selectedGroup)에 따라 데이터를 로드하도록 변경.

좌상단 뒤로가기(Back) 버튼 추가.

최고 기록 시스템:

연속 정답(Streak) 최고 기록을 저장하고 홈 화면에 "내 최고 연속 점수" 표시.

📅 2025-12-03: 버그 수정 및 디테일 잡기
피드백 타이밍 수정: 정답 시 "정답입니다!" 메시지가 뜨기 전에 다음 문제가 로드되어 메시지가 씹히는 현상 수정 (순서 변경 및 postDelayed 적용).

레이아웃 위치 조정: paddingBottom을 활용하여 전체 UI를 화면 중앙보다 살짝 상단(2/5 지점)에 위치하도록 조정 (시각적 안정감 확보).

📅 2025-12-04: 수익화(Monetization) - AdMob 연동
Google AdMob 통합:

play-services-ads 라이브러리 추가 및 매니페스트 설정.

레이아웃 변경: ScrollView 루트를 RelativeLayout으로 변경하여 광고 영역 확보.

배너 배치:

상단: 기본 배너(BANNER) 배치 및 상태바 간섭 방지(marginTop).

하단: 수익성 증대를 위한 대형 배너(LARGE_BANNER) 적용.
