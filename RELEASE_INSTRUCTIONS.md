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

## 📦 Як створити Android App Bundle (AAB) для Google Play:
```bash
STORE_PASSWORD="android" KEY_PASSWORD="android" gradle :app:bundleRelease
```
Результат буде в:
```
app/build/outputs/bundle/release/app-release.aab
```
