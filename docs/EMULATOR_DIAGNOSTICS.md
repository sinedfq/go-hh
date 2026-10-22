# Диагностика запуска Pixel_7 — 20 октября 2026

## Рабочий вариант

Запуск Android восстановлен на отдельном Emulator 36.5.11 с путями без кириллицы.
Получен рабочий стол Android 14; `adb devices` показал `device`,
а `getprop sys.boot_completed` вернул `1`.

- Совместимый SDK: `D:\Android\Sdk-Trudomix`.
- Профиль: `Trudomix_API34`, данные в `D:\Android\avd\Trudomix_API34.avd`.
- Основной SDK 37.2.12 и старый Pixel_7 не заменялись и не удалялись.
- Общие компоненты SDK подключены через directory junctions, без дублирования системных образов.
- Для Trudomix выбран совместимый SDK в `android/local.properties` и `android/emulator.local.json`.

Обнаружены две независимые стадии сбоя: VirtualScene в 37.2.12 и отказ чтения
`VerifiedBootParams.textproto` по пути с кириллицей. Во втором случае Android
перезагружался с `Failed to mount /system` и `Kernel panic - not syncing: Attempted to kill init`.
Путь без кириллицы восстановил чтение vbmeta и штатную проверку системного раздела.
Проверка Verified Boot не отключалась.

Также остановлены два Gradle 8.9 daemon, оставшиеся после сборок: до этого
свободный лимит виртуальной памяти Windows составлял около 1,2 ГБ.

Запуск:

```powershell
.\scripts\emulator.ps1 -Visible -WaitForBoot
```

## Первичная диагностика до исправления

Установлен Android Emulator 37.2.12, AVD Pixel_7 использует Android 14 (API 34), Google Play x86_64.
Запуск повторно разрешён, но Android не завершил загрузку.

## Подтверждённые сообщения

```text
Critical: Failed to load opengl32sw (Не найден указанный модуль.)
Warning: Software OpenGL failed. Falling back to system OpenGL.
initialize: Invalid scene argument: default.jpg
createScene: scene failed to load
emulatorSetupEnvironment: Cannot initialize virtual scene for the environment
emulator_window_setup: Could not setup environment
detected a hanging thread 'QEMU2 CPU0 thread'. No response for 15009 ms
```

В одном запуске adb кратковременно показывал `emulator-5554 offline`;
в остальных готового устройства не было. `sys.boot_completed=1` не получен.

Ошибка OpenGL сопровождается fallback, поэтому считать её единственной причиной нельзя.
Постоянно воспроизводится отказ инициализации виртуального окружения с последующим зависанием QEMU.
Точная первопричина отказа движка не установлена. Это происходит до запуска Trudomix.

## Проверено без сброса данных

- Удалены только зависшие процессы, созданные диагностическими запусками.
- Запуск без загрузки/сохранения snapshot.
- Ограничение памяти 2560 МБ и двух ядер.
- WHPX и диагностический запуск без аппаратного ускорения.
- SwiftShader, отключение Vulkan и VirtualScene параметрами запуска.
- Отключение виртуальных камер.
- Короткие пути Windows без кириллицы для SDK и каталога AVD.
- Запуск из каталога ресурсов и отключение display layout.

Успешного обхода не найдено. Файл `resources/default.jpg` существует и имеет JPEG-заголовок.
Не выполнялись Wipe Data, удаление AVD, переустановка SDK, изменение драйверов или настроек Windows.

Логи находятся в `.cache/emulator-*.log`, в том числе:

- `.cache/emulator-accelerated.log`
- `.cache/emulator-camera-off.log`
- `.cache/emulator-ascii-path.log`
- `.cache/emulator-no-layout.log`

Следующая отдельная проверка — запуск этого AVD на другой версии пакета Android Emulator
или проверка новой чистой AVD при сохранении существующей. Это позволит различить сбой
пакета Emulator и особенности текущего AVD; результат заранее не гарантирован.

Официальная документация:

- https://developer.android.com/studio/run/emulator-troubleshooting
- https://developer.android.com/studio/emulator_archive
