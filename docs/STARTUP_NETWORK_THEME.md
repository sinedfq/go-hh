# Анимация, загрузка и общая тема Trudomix

> Раздел об анимации ниже относится к предыдущей версии. Новая последовательность
> из HTML описана в [LOADER_HTML.md](LOADER_HTML.md); палитра и обработка сети сохранены.

Доработка от 26 октября 2026. Последнее уточнение: длинные хаотичные линии,
затем прежняя анимация логотипа; общий стиль Night Wave / White Wave.

## Что изменилось

Вместо коротких одинаковых отрезков используются 12 длинных нитей. У каждой
свои широкие случайные изгибы гладкого составного сплайна по размеру экрана.
Начала находятся за четырьмя углами, конец всегда один — центр экрана.
Траектории асимметричны, могут пересекаться между собой, но не образуют петель.
При новом запуске рисунок меняется; в пределах запуска seed сохраняется.
Длинный видимый участок сокращается, когда хвост догоняет передний конец.
Расчёт траекторий выполняется при изменении размера, а не на каждом кадре.

Линии исчезают к 2,8 с. Появление буквы (2,8–3,6 с), одна нейтральная волна
(3,7–5,5 с) и название (5,5–6,5 с) сохранены. Точка расположена в (73,59)
по красной отметке пользователя, без изменения размера или цвета. В SVG устранена лишняя неполная
команда Q, которую браузер считал ошибкой контура. PNG из icons не изменялись.

Сохранённой Canvas-версии в Git нет: последние коммиты содержат старый
растровый загрузчик. Поэтому исправления внесены точечно в рабочую реализацию.
Полный reset не выполнялся.

## Цвета и стили

Веб использует существующие CSS-переменные, Android — существующий ColorScheme.
Обе платформы используют одну таблицу основных цветов:

| Роль | Светлая тема | Тёмная тема |
| --- | --- | --- |
| Фон | #F8F7FC | #080816 |
| Карточки | #FFFFFF | #121222 |
| Дополнительная поверхность | #F0EDF7 | #1B1B30 |
| Основной текст | #191431 | #FAF8FF |
| Вторичный текст | #68647A | #A8A5BC |
| Акцент | #8257E6 | #9474ED |
| Дополнительный акцент / точка | #9653F5 | #9653F5 |
| Граница | #E5E1EF | #303047 |

Гамма соответствует фиолетово-синим волнам и нейтральному фону двух PNG.
Семантические цвета ошибок, успеха и предупреждений сохранены отдельно.
Выбор значка приложения не меняет цвета бизнес-интерфейса.
Системная тема и ручной выбор сохранены; веб по умолчанию следует системе,
а явное переключение сохраняется. Текст на основном акценте адаптирован по теме.

Причины отсутствовавшего оформления:

- Общая auth-switch находилась в CSS ленивого экрана входа. Теперь она находится
  в глобальном buttons.css и доступна на холодном входе в любой маршрут.
- Стили выбора значка были смешаны со старым PageLoader.css. Они выделены
  в BrandPicker.css, а стили фирменных PNG — в BrandContext.css.
- Старые локальные цвета перекрывали токены. В существующих правилах заменены
  именно цветовые роли, без переписывания сеток и массового добавления important.
- При переходе с мобильной ширины к 1024 px анимация ширины sidebar могла
  временно занять всю страницу. На десктопе её ширина ограничена 240 px.

Истории остаются компактной лентой круглых превью. Общая кнопка обновления
показывает загрузку и предотвращает параллельные запросы. Кнопка закрытия чата
остаётся в общем заголовке, а профиль — рядом с поиском и фильтрами.

## Загрузка и соединение

Длинная заставка воспроизводится только при запуске. AppLoader и PageLoadingState
используются на маршрутах и в локальных блоках; PageLoader и TrudomixLoadingScreen
сохранены как совместимые обёртки. Линейные индикаторы фактического прогресса
резюме, истории или загрузки файла не заменены фиктивной анимацией.
При обновлении главной страницы, ленты и поиска доступные данные сохраняются.

Состояния запуска: INITIALIZING, ANIMATING, READY, OFFLINE,
SERVER_UNAVAILABLE, ERROR. Отсутствие сети, недоступный сервер, таймаут,
ответ 5xx и авторизация различаются. HTTP 401/403 не называются отсутствием
интернета; сохранена существующая обработка сессии.

При ошибке после анимации остаются логотип и название, под ними — сообщение
и повторное подключение. Повтор сначала проверяет существующий /health
(до 8 с), затем повторно загружает данные. Заставка не проигрывается заново.
Повторные нажатия защищены от параллельных проверок. Android также предлагает
системные настройки сети и существующую настройку адреса сервера в debug.
В вебе нет фиктивной кнопки системных настроек или офлайн-доступа к данным.

Начальные Axios-запросы ограничены 12 с; обычные — 20 с, загрузка FormData —
120 с. Android дополнительно ограничивает общую инициализацию 20 с и отменяет
зависшие вызовы. После запуска потеря соединения показывает локальный баннер,
сохраняет маршрут и не возвращает пользователя к анимации.

## Основные файлы

- frontend/index.html, frontend/vite.config.js, frontend/public/splash.js,
  frontend/public/splash.css, frontend/public/loader-bootstrap.js.
- frontend/src/main.jsx, frontend/src/App.jsx, utils/startupLoading.js,
  utils/networkState.js, utils/networkState.test.js, utils/splash.test.js.
- frontend/src/components/common/BrandMark.jsx, AppLoader.jsx, PageLoadingState.jsx,
  PageLoader.jsx, PageLoader.css, NetworkErrorState.jsx, NetworkErrorState.css,
  ConnectionBanner.jsx, BrandPicker.jsx, BrandPicker.css, CompactRefreshButton.*,
  YandexMap.jsx, YandexMap.css. Удалён неиспользуемый frontend/public/loader.css.
- frontend/src/contexts/BrandContext.jsx, BrandContext.css.
- frontend/src/styles/variables.css, buttons.css, theme-dark.css, theme-surfaces.css,
  refresh.css, sidebar.css и локальные CSS карточек, форм, чатов, резюме и карты.
- frontend/src/pages/home/HomePage.*, search/SearchResultsPage.jsx,
  search/SavedSearchesPage.jsx, profile/ProfilePage.jsx,
  employer/EmployerStatsPage.jsx, components/social/SocialFeed.jsx.
- Импорты общего загрузчика: ChatApp, ChatList, ChatModal, NotificationBell,
  ResumeViewModal, MyApplicationsPage, EmployerApplicationsPage, EmployerStatsPage,
  MyCompanyPage, MyVacanciesPage, RecommendationsPage, VacancyPage.
- Android: MainActivity.kt, TrudomixApp.kt, data/api/ApiClient.kt,
  startup/StartupReadiness.kt, startup/NetworkIssue.kt.
- Android ui/loading: SplashTiming.kt, TrudomixSplash.kt, AppLoader.kt,
  TrudomixLoadingScreen.kt, NetworkErrorState.kt.
- Android ui/theme: Color.kt, Theme.kt, Tokens.kt; ui/components/ProfileAccess.kt,
  AppScreenHeader.kt, YandexVacancyMap.kt; ui/screens/vacancies/VacancyFeed.kt.
- Подключение AppLoader в Android: PhotoPicker, AvatarCropDialog, VacancyDetailScreen,
  VacancyMapScreen, CompanyStories, chats/ChatScreen, ResumeEditorScreen,
  LoginScreen, RegisterScreen.
- Проверки: SplashTimingTest, NetworkIssueTest, SplashSequenceTest,
  SplashNetworkStateTest, LoaderBrandTest, SearchProfileToolbarTest,
  WebResponsiveUiTest, WebStartupNetworkTest, WebLoaderBrandTest,
  WebFrontendPreviewTest.

В списке Android базовый путь — android/app/src/main/java/com/example/trudomix/.
Имена JSX/CSS без полного префикса относятся к frontend/src/.

## Проверки и ограничения

Модульные проверки: 16 JavaScript и 38 Android JVM тестов. Веб production-сборка
и Gradle assembleDebug проходят. Frontend lint не имеет ошибок, сохраняются
предупреждения существующего кода.

Снимки и сценарии браузера выполняются в Chromium WebView с изолированными
ответами API: реальные учётные записи и записи в базе не нужны.
Матрица ширин: 360, 430, 768, 1024, 1280 и 1920 px, обе темы.
Итоговый запуск SplashSequenceTest (2 сценария), LoaderBrandTest,
WebResponsiveUiTest и WebStartupNetworkTest завершился: OK (5 tests).
Проверены длинные нити, финальная фаза логотипа, размеры кнопок и историй,
общий заголовок чата, ошибка сети, таймаут, 503, восстановление и режим
уменьшенной анимации. Локальный Docker frontend обновлён, /health отвечает 200.
Контрольные снимки находятся в .cache/ui-review/ и во внешнем каталоге тестового
приложения эмулятора. Ошибочные старые кадры с зависшим System UI не учитываются.
Перед статичными снимками завершаются конечные анимации появления и перехода
цвета внутри контента. Самостоятельная анимация заставки проверяется отдельно
по её временным этапам, без этой стабилизации.

Полноценный офлайн-доступ к данным не добавлялся. Если веб-документ ещё не был
загружен и интернет полностью отсутствует, браузер не может скачать оболочку
сайта; фирменный экран обрабатывает ошибки после загрузки оболочки.
Стабильные 60 FPS на физическом устройстве не подтверждены. Android lint ранее
останавливался на двух ProduceStateDoesNotAssignValue в MarkdownContent.kt
и VideoPreview.kt; эти несвязанные компоненты не переписывались.
