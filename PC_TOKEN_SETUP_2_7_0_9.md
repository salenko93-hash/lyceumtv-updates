# LyceumTV 2.7.0.9 — введення alerts.in.ua token з ПК

Після встановлення debug APK 2.7.0.9 у корені проєкту запустіть:

```powershell
.\SET_ALERTS_TOKEN.ps1
```

PowerShell попросить token приховано через `Read-Host -AsSecureString`.

Скрипт:
- підключається до TV через ADB;
- через `run-as ua.edu.cunl.tv.debug` передає token через STDIN у приватний sandbox;
- перезапускає LyceumTV;
- застосунок одразу шифрує token через `SecureTokenStore` / Android Keystore AES-GCM;
- видаляє тимчасовий plaintext-файл;
- перевіряє, що зашифровані `iv` і `data` створені.

Token не вбудований в APK і не записується в сам PowerShell-файл.

Інший TV:

```powershell
.\SET_ALERTS_TOKEN.ps1 -Tv "192.168.5.88:5555"
```

Механізм призначений для debug-пакета `ua.edu.cunl.tv.debug`, оскільки `adb run-as`
потребує debuggable build.
