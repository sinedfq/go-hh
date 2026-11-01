# Android: кнопка поиска и проверка производительности

## Изменения

Кнопка поиска рисуется одним Canvas: тёмно-синий круг, тонкая голубая рамка,
точечный фон и неподвижная лупа. Координаты и фазовые смещения точек вычисляются
один раз; Stroke кэшируются по плотности экрана. Нет blur, RenderEffect,
изображений, отдельных Composable для точек или графических слоёв кнопки.

В покое по умолчанию используется статичный Dot Matrix. В `Настройки → Оформление`
можно включить волну. Это осознанный приоритет производительности: постоянная
анимация требует перерисовок даже тогда, когда остальной экран неподвижен.
Волна ограничена 30 обновлениями в секунду, состояние читается в draw-фазе.
Короткий импульс нажатия не задерживает вызов навигации. При `isLowRamDevice`,
memoryClass < 192 МБ, энергосбережении или нулевом animator_duration_scale
используется статичный вариант. Оба цикла принадлежат RESUMED-жизненному циклу
экрана; после ухода в фон они отменяются, а после возврата запускаются заново.

## Что обнаружено при просмотре кода

| Участок | Проблема и изменение |
| --- | --- |
| MainScreen, GlassNavigation | Ради нижнего меню записывалось содержимое всего экрана в Haze и применялось размытие 14 dp. Убраны захват и blur; сохранены полупрозрачная подложка, граница и лёгкая тень. |
| MainScreen | LaunchedEffect был привязан к selected: каждое переключение вкладки немедленно перезапускало unread-запрос. Теперь один цикл на RESUMED, экземпляр репозитория сохраняется. |
| MainScreen | Изменение unread инвалидировало родительский экран. Счётчик и запрос вынесены в отдельную композиционную область меню. |
| ChatListScreen, ChatViewModel | Периодическое обновление чатов принадлежит только видимому экрану и учитывает свежесть предыдущего ответа. Сохранены ручное обновление и защита от параллельных запросов. |
| ConversationViewModel | Каждый ответ объединялся с локальными сообщениями через вложенный none: O(n²) в главном потоке. Теперь индекс по ID и сортировка выполняются на Dispatchers.Default; атомарное обновление сохраняет сообщения, отправленные во время запроса. |
| Подписки на состояние | 31 файл использовал collectAsState для Flow. Подписки переведены на collectAsStateWithLifecycle; бизнес-операции ViewModel от этого не отменяются. |
| VacancyFeed | Стабильные ID уже были. Добавлен contentType карточек для повторного использования одинаковой структуры в lazy grid. |

Это найденные источники лишней работы, а не доказательство, что каждый из них
самостоятельно вызывал все замеченные пользователем лаги.

Проверены также загрузка фото, видео, сетевой клиент и фоновые задачи. Фото уже
используют AsyncImage/Coil с размерами из layout. Кадры видео извлекаются на IO,
ограничены семафором и LruCache. Обработка аватарок и Markdown уже вынесена
с UI-потока. Polling откликов и открытого диалога уже связан с жизненным циклом.
Эти механизмы сохранены. Heap dump не снимался: отсутствие утечек памяти не доказано.
Полноразмерный fallback видеокадра на API 26 требует отдельной проверки на устройстве.

## Методика замеров

Android Studio Profiler и Layout Inspector в этой сессии недоступны. Используются
Window.OnFrameMetricsAvailableListener, Process.getElapsedCpuTime,
Debug.MemoryInfo, dumpsys gfxinfo и am start -W. Это диагностический тест debug-сборки,
а не release Macrobenchmark и не измерение на физическом телефоне.

Одинаковый AVD Trudomix_API34, API 34, 3 ГБ RAM, 4 ядра, SwiftShader,
720×1600, density 280. До: APK 10, после: APK 11. Сценарий RenderingPerformanceTest:
120 локальных вакансий без сети и декодирования изображений, два разогревающих
свайпа и 12 свайпов по 400 мс. Данные, направления жестов и длительность одинаковы.
Контейнер Haze до изменения соответствует старому MainScreen; после используется
обычный контейнер, как в обновлённом приложении.

TOTAL_DURATION — системная длительность кадра; доля >16,67 мс показывает
превышение условного бюджета 60 Гц. Это не точное число потерянных кадров.
FrameMetrics может терять callback при перегрузке; FPS дисплея не измеряется.
CPU — время всего процесса за сценарий, включая тестовый код, а не процент CPU
реального пользовательского сеанса. PSS снимается в конце, не является пиком.
am start -W измеряет первое открытие Activity, а не окончание Splash или загрузки API.
Скорость открытия отдельных авторизованных экранов и медиа-сценарий не измерялись.

## Результаты, 1 ноября 2026

Медианы трёх прогонов. Волна в новой кнопке отключена — штатная настройка по умолчанию.

| Метрика | До, build 10 | После, build 11 |
| --- | ---: | ---: |
| p50 TOTAL_DURATION кадра | 292,70 мс | 268,20 мс |
| p95 TOTAL_DURATION кадра | 458,77 мс | 351,81 мс |
| Кадры >16,67 мс среди полученных callback | 100% | 100% |
| Время сценария | 8,14 с | 7,37 с |
| CPU-время процесса за сценарий | 8,41 с | 8,16 с |
| PSS в конце | 186,69 МиБ | 189,74 МиБ |
| Java heap в конце | 9,79 МиБ | 12,69 МиБ |
| Холодное открытие Activity, am start -W TotalTime | 2685 мс | 2576 мс |

В этом сценарии наблюдалось уменьшение времени кадра, особенно длинных кадров.
Память не уменьшилась. Все наблюдаемые кадры всё ещё выходили за бюджет 60 Гц:
устранение всех лагов и плавность на физическом телефоне не подтверждены.
Первый исходный прогон был существенно медленнее следующих: p50 459,67 мс
против 292,70 и 290,19 мс. Даже медианы трёх прогонов не устраняют влияние
разогрева, системных служб Android, SwiftShader, debug-инструментирования и нагрузки ПК.
Результаты не позволяют приписать весь эффект только отключению Haze.

CPU-время — сумма времени потоков процесса. Деление на wall time даёт примерно
103% одного ядра до и 111% после: более короткий сценарий не означает снижение
мгновенного процента CPU. Это не отдельный замер CPU самой кнопки.
Пик RAM, FPS дисплея, точное число пропущенных кадров, счётчик рекомпозиций
Layout Inspector и навигация по реальным авторизованным экранам не измерялись.
dumpsys gfxinfo после завершения instrumentation не нашёл уже закрытый процесс;
его статистика не использована. Для кадров использованы записанные FrameMetrics.
Разница холодного открытия невелика и не доказывает устойчивое ускорение запуска.

Исходные числа, все повторы и параметры окружения: [android-build11.json](performance/android-build11.json).
Исходный измерительный тест для build 10 сохранён отдельно; он добавляется только
в androidTest и не меняет production-код baseline.

Проверки Gradle: 49 JVM-тестов, debug APK и androidTest APK собраны успешно.

## Изменённые файлы

- [android/app/src/androidTest/java/com/example/trudomix/RenderingPerformanceTest.kt](../android/app/src/androidTest/java/com/example/trudomix/RenderingPerformanceTest.kt)
- [android/app/src/androidTest/java/com/example/trudomix/SearchButtonLifecycleTest.kt](../android/app/src/androidTest/java/com/example/trudomix/SearchButtonLifecycleTest.kt)
- [android/app/src/main/java/com/example/trudomix/MainActivity.kt](../android/app/src/main/java/com/example/trudomix/MainActivity.kt)
- [android/app/src/main/java/com/example/trudomix/TrudomixApp.kt](../android/app/src/main/java/com/example/trudomix/TrudomixApp.kt)
- [android/app/src/main/java/com/example/trudomix/data/local/VisualPerformance.kt](../android/app/src/main/java/com/example/trudomix/data/local/VisualPerformance.kt)
- [android/app/src/main/java/com/example/trudomix/ui/components/DotSearchButton.kt](../android/app/src/main/java/com/example/trudomix/ui/components/DotSearchButton.kt)
- [android/app/src/main/java/com/example/trudomix/ui/components/GlassNavigation.kt](../android/app/src/main/java/com/example/trudomix/ui/components/GlassNavigation.kt)
- [android/app/src/main/java/com/example/trudomix/ui/components/SelectedBrand.kt](../android/app/src/main/java/com/example/trudomix/ui/components/SelectedBrand.kt)
- [android/app/src/main/java/com/example/trudomix/ui/loading/TrudomixLoadingScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/loading/TrudomixLoadingScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/loading/TrudomixSplash.kt](../android/app/src/main/java/com/example/trudomix/ui/loading/TrudomixSplash.kt)
- [android/app/src/main/java/com/example/trudomix/ui/navigation/AppNavGraph.kt](../android/app/src/main/java/com/example/trudomix/ui/navigation/AppNavGraph.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/applications/ApplicationsScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/applications/ApplicationsScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/auth/LoginScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/auth/LoginScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/auth/PhoneLoginContent.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/auth/PhoneLoginContent.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/auth/PhoneOtpForm.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/auth/PhoneOtpForm.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/auth/RegisterScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/auth/RegisterScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatListScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatListScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatViewModel.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/chats/ChatViewModel.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/chats/ConversationViewModel.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/chats/ConversationViewModel.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/chats/MessageMerge.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/chats/MessageMerge.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/companies/CompanyScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/companies/CompanyScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/debug/LogViewerScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/debug/LogViewerScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/employer/EmployerScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/employer/EmployerScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/home/MainScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/home/MainScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/home/SavedSearchesScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/home/SavedSearchesScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/map/VacancyMapScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/map/VacancyMapScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/profile/ProfileScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/profile/ProfileScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/resume/ResumeEditorScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/resume/ResumeEditorScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/resume/ResumeScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/resume/ResumeScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/AppIconPicker.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/AppIconPicker.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/AppearanceScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/AppearanceScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/LoadingStylePicker.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/LoadingStylePicker.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/NotificationEventSettings.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/NotificationEventSettings.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/NotificationSettings.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/NotificationSettings.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/settings/SettingsDetailScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/settings/SettingsDetailScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/social/SocialFeed.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/social/SocialFeed.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyDetailScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyDetailScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyFeed.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyFeed.kt)
- [android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyListScreen.kt](../android/app/src/main/java/com/example/trudomix/ui/screens/vacancies/VacancyListScreen.kt)
- [android/app/src/main/java/com/example/trudomix/ui/theme/Theme.kt](../android/app/src/main/java/com/example/trudomix/ui/theme/Theme.kt)
- [android/app/src/test/java/com/example/trudomix/MessageMergeTest.kt](../android/app/src/test/java/com/example/trudomix/MessageMergeTest.kt)
- [android/app/src/test/java/com/example/trudomix/SearchMotionPolicyTest.kt](../android/app/src/test/java/com/example/trudomix/SearchMotionPolicyTest.kt)
- [docs/ANDROID_PERFORMANCE.md](../docs/ANDROID_PERFORMANCE.md)
- [docs/performance/android-build11.json](../docs/performance/android-build11.json)
- [docs/performance/baseline/RenderingPerformanceTest.kt](../docs/performance/baseline/RenderingPerformanceTest.kt)
