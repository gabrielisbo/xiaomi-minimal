# Minimal Text Launcher — Poco X5 Pro

Launcher Android 100% offline, minimalista e baseado em tipografia pura.

## Segurança e Blindagem
- **Zero Permissão de Internet:** Sem `<uses-permission android:name="android.permission.INTERNET" />` no `AndroidManifest.xml`.
- **WebView Isolada:** `blockNetworkLoads = true` e `Content-Security-Policy` estrita (`connect-src 'none'`).
- **Zero Consumo em Segundo Plano:** Cronômetro de sessão pausado via `visibilitychange` quando a tela apaga ou outro app entra em foco.

## Como gerar o APK pelo GitHub (Sem instalar Android Studio)
1. Suba os arquivos deste pacote em um repositório **Privado** no seu GitHub.
2. Acesse a aba **Actions** do seu repositório.
3. O workflow **Build Minimal Launcher APK** vai rodar automaticamente (e leva ~2 minutos).
4. Clique no build concluído e baixe o arquivo **MinimalLauncher-PocoX5Pro-APK** na seção **Artifacts**.
