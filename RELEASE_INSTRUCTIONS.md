# 🚀 Інструкція для збірки Release APK з готовим підписом

Цей проект уже повністю підготовлено для створення підписаного Release APK або AAB файлу без повторних запитів.

## 🔑 Дані ключа підпису (Keystore)
- **Файл сховища ключів**: `my-upload-key.jks` (знаходиться в корені проекту)
- **Key Alias**: `upload`
- **Store Password**: `android`
- **Key Password**: `android`
- **Алгоритм**: RSA 2048-bit, термін дії 10,000 днів

---

## 🛠️ Як створити підписаний Release APK

### Варіант 1: Через підготовлений скрипт
У терміналі виконайте:
```bash
./scripts/build-release.sh
```

### Варіант 2: Через команду Gradle
```bash
STORE_PASSWORD="android" KEY_PASSWORD="android" gradle :app:assembleRelease
```

### Де шукати готовий APK:
Після завершення збірки готовий підписаний APK буде тут:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🤖 Автоматична збірка через GitHub Actions (CI/CD)

У репозиторії налаштовано білдер `/.github/workflows/android.yml`:

1. **Автоматичний запуск**:
   - При кожному `push` у гілку `main` або створенні `pull request`.
   - При створенні тегу релізу (наприклад, `git tag v1.0.0 && git push origin v1.0.0`).
   - Вручну: вкладка **Actions** на GitHub -> **Android CI & Release Builder** -> кнопка **Run workflow**.

2. **Що створює білдер**:
   - `AIAgent-Release-APK` — готовий підписаний APK файл для встановлення на телефон.
   - `AIAgent-Debug-APK` — налагоджувальний APK.
   - `AIAgent-Release-AAB` — App Bundle для публікації в Google Play.

3. **Де скачати готовий APK**:
   - Перейдіть у вкладку **Actions** вашого GitHub репозиторію.
   - Виберіть останній запуск збірки (**Build & Sign APK**).
   - У розділі **Artifacts** внизу сторінки скачайте `AIAgent-Release-APK`.
   - При публікації з тегом `v*` білдер автоматично створить офіційний **GitHub Release** з прикріпленими файлами APK та AAB!
