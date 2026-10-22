# Ветки и проверки Trudomix

`dev` — общая ветка разработки. В ней уже есть изменения из
`feat/trudomix-platform-update`. `main` обновляется отдельно при подготовке релиза.

Новую задачу начинаем от актуальной `dev`:

```powershell
git switch dev
git pull --ff-only
git switch -c feat/short-task-name
```

После изменений и локальных проверок:

```powershell
git add --all
git commit -m "feat: describe the change"
git push --set-upstream origin feat/short-task-name
```

На GitHub создаём Pull Request с целевой веткой `dev`. Перед слиянием смотрим
изменения и результаты workflow `Project checks`. Правила защиты веток пока
не настроены: наличие workflow само по себе не запрещает слияние с ошибками.

Проверки запускаются для Pull Request в `dev`/`main`, отправки изменений в эти
ветки и вручную через Actions. Проверяются:

- Go: тесты и сборка; интеграционные тесты используют отдельный PostgreSQL
  внутри задания, а не локальную или рабочую базу.
- Сайт: установка по lock-файлу, тесты, lint и production-сборка.
- Android: JVM-тесты, сборка debug APK и APK инструментальных тестов.
  Эмулятор в CI не запускается. Debug APK доступен в артефактах задания 7 дней;
  адрес сервера можно настроить в приложении. Конфигурация Firebase и ключи
  подписи в репозиторий не попадают.

Локальная работа описана в [DEVELOPMENT.md](DEVELOPMENT.md).
