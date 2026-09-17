<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/489dfe15-efa5-444b-975c-be7213d214a7

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Add your API key in one of two places:
   - **In the app:** tap the settings icon (top right), pick a provider, paste the key.
     Keys are saved and survive a restart.
   - **At build time:** put it in `.env` (see `.env.example`). Never commit a real key.
5. Run the app on an emulator or physical device

Building from the command line works too:

```bash
./gradlew assembleDebug      # build the APK
./gradlew testDebugUnitTest  # run the tests
```

## Two modes

- **I have a CV** — upload a PDF/DOCX, paste text, or paste a LinkedIn profile. Get a
  roast, then concrete fixes and a score.
- **I'm hiring** — paste the job advert you are about to post. Find out how it reads to
  the candidates you want, and get the worst lines rewritten.

See [CHANGES.md](CHANGES.md) for what was fixed and added in the most recent pass.
