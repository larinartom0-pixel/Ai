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
   - Тільки один готовий файл: `AIAgent-Release-APK` — підписаний релізний APK, оптимізований для встановлення без зайвих налагоджувальних файлів та AAB.

3. **Де скачати готовий APK**:
   - Перейдіть у вкладку **Actions** вашого GitHub репозиторію.
   - Виберіть останній запуск збірки (**Build Signed Release APK**).
   - У розділі **Artifacts** внизу сторінки скачайте `AIAgent-Release-APK`.
   - При пуші з тегом `v*` білдер автоматично прикріпить готовий APK до **GitHub Releases**!
