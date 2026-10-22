# 38HX Browser (protótipo Android)

Navegador Android inicial em Kotlin/WebView, com tema escuro e azul, navegação básica e controles locais. A integração Proton VPN atual encaminha para o cliente oficial da Proton; não implementa um túnel VPN embutido.

## Compilar localmente
Requer JDK 17, Android SDK Platform 35 e Gradle 8.9. Na pasta raiz do projeto, execute:

```bash
gradle assembleDebug
```

O APK de depuração deve aparecer em `app/build/outputs/apk/debug/app-debug.apk`.

## Compilar pelo GitHub usando apenas o celular
Siga `LEIA-ME-PELO-CELULAR.txt`. O workflow em `.github/workflows/build.yml` compila o APK e publica o resultado como artefato do GitHub Actions.

## Limitações
- Este é um protótipo e deve ser testado antes de uso cotidiano.
- A VPN não está embutida: o navegador abre o aplicativo Proton VPN oficial.
- APK `debug` é para teste; distribuição pública requer revisão, assinatura apropriada e testes de segurança.
