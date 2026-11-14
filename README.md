# OMNIA AI — Android starter

OMNIA AI is a native Android chat client starter. It keeps conversations on the device and sends the conversation context to the official xAI Responses API, so a chat can continue after reopening the app.

## Important scope

- This project connects through an **xAI API key**. It does not sign in to Grok consumer accounts and does not use Grok website passwords or browser cookies.
- It does not transfer a consumer Grok subscription quota between accounts, rotate accounts when a limit is reached, or bypass usage limits.
- The xAI API has its own access, rate limits, and possible charges. Check the current xAI API pricing and account requirements before using it.
- API keys are encrypted with Android Keystore before being stored on the device. Conversation history is stored in the app's private local storage.
- Do not put API keys in source files, GitHub commits, screenshots, or public issue reports.

## Build APK with GitHub Actions

1. Create a **new, empty GitHub repository** for this project. Do not upload it into an unrelated app repository.
2. Upload the contents of this folder to the repository root, preserving the `.github/workflows/android.yml` path.
3. Open the repository's **Actions** tab and select **OMNIA AI Android Build**.
4. When the workflow finishes successfully, open the run and download the `omnia-ai-debug-apk` artifact.
5. Install the APK on your Android device. Android may ask you to allow installation from the browser or file manager you used.

The workflow uses JDK 17 and Gradle 8.9 and runs `:app:assembleDebug`.

## Configure Grok API

Open OMNIA AI → Settings → enter an xAI API key and a model ID available to that API key. The default model field is `grok-4.7`; change it if your API account exposes a different model.

Official references:
- xAI API docs: https://api.x.ai/docs/
- xAI API best practices: https://docs.x.ai/developers/model-capabilities/text/best-practices
- xAI API rate limits: https://docs.x.ai/developers/rate-limits

## Current MVP limitations

- Grok API text chat only; no images, voice, file uploads, or web-search tools yet.
- One API key is configured at a time.
- Context is stored locally and sent with each follow-up; very long chats may eventually need context summarization.
- No server or cloud sync is included.
