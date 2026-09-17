# What changed in this pass

Everything below was compiled and tested locally: `./gradlew assembleDebug` is clean with
zero warnings, and `./gradlew testDebugUnitTest` is 19/19 green.

---

## 🔴 Do this first: revoke a key

The zip I was given contained a **real Anthropic API key** in `.env` (108 characters, not
the placeholder). `.env` is in `.gitignore` so it never reached git, but it did travel
inside the zip, so treat it as leaked: **revoke it in the Anthropic console**.

`.env` in this copy is back to placeholders. Keys go in the in-app settings screen now,
which finally persists them (see below).

---

## The bug that mattered: the app was quietly making things up

`GeminiAnalysisService.generateTailoredAnalysis()` is a ~220-line hardcoded template. All
three providers called it whenever anything went wrong: bad key, no network, malformed
reply. The user saw a confident, plausible roast that had nothing to do with their CV,
with no indication anything had failed.

That path was not rare, it was the **normal** path on Claude:

- `max_tokens` was `4000` on `claude-opus-5`. Opus 5 runs adaptive thinking by default and
  thinking tokens count against `max_tokens`, so the budget was spent before the JSON
  finished. Truncated JSON → parse returns null → canned text. Every time.

Fixed:

- `max_tokens` raised to 16000, and `stop_reason` is now checked, so a truncated or
  refused reply is reported instead of silently swapped out.
- The silent fallback is gone. Failures now raise `AnalysisException` with a message
  written for a user ("That Claude API key was rejected. Check it in settings and try
  again.") and the ViewModel shows it.
- The canned engine survives as `OfflineSampleAnalysis`, but it is **opt-in** only: there
  is a "Show the offline demo instead" button on the error state. Anything it produces
  carries `isOfflineFallback = true` and the results screen shows an amber banner saying
  it is a demo, not your CV. Useful when the venue wifi dies; never mistaken for real
  analysis.

## Document upload was feeding the model garbage

- **DOCX was not implemented at all**, even though the file picker offered it. A `.docx`
  is a zip; it was read as UTF-8 and the binary was sent to the model as if it were a CV.
  Now unzipped properly, reading `word/document.xml` and keeping paragraph breaks.
- **PDF only worked on uncompressed files.** It regexed `(text) Tj` straight out of the
  raw bytes, but real PDFs store content in FlateDecode streams. Now the streams are
  inflated first, then the text operators are read, with escapes and line positioning
  handled.

Both are covered by `DocumentExtractorTest`, which runs real `.pdf` and `.docx` fixtures
through the parsers. Both of those files would have failed before.

## New: recruiters can roast their own job advert

A mode switch at the top of the home screen: **"I have a CV"** / **"I'm hiring"**.

In hiring mode you paste (or upload) a job advert and get:

- the roast, and **what a good candidate thinks while skim-reading it**
- a 0–100 score across clarity, honesty, how welcoming it is, how realistic the asks are,
  and whether anyone would actually apply
- **"Asks that nobody can meet"** — impossible requirements, like more years of a
  framework than it has existed
- **"You forgot to say"** — salary, location, seniority and other things candidates look
  for and could not find
- three prioritised fixes, before/after rewrites of real lines, and a rewritten opening
  paragraph with a copy button

The prompt is held to the same rules as the CV side: quote the real advert, invent
nothing, and never suggest wording that would exclude anyone. If the advert already
contains discriminatory wording it is flagged plainly as a legal and fairness problem
rather than played for laughs.

New files: `model/JobAdModels.kt`, `network/JobAdPromptHelper.kt`,
`ui/screens/JobAdResultsScreen.kt`. Covered by `JobAdPromptHelperTest` (7 tests) and
`JobAdResultsScreenTest` (4 tests, renders the whole screen).

## Results are written in plain English now

The prompt now has a hard "WRITE IN PLAIN ENGLISH" section: short sentences, everyday
words, no HR jargon unless it is explained in the same breath, and jargon like ATS spelled
out ("the software that scans CVs before a human sees them"). The jokes stay; they just
have to land on one read.

The screen labels followed:

| Before | After |
|---|---|
| PART 1: THE ROAST / PART 2: THE RESCUE | 1. THE ROAST / 2. THE FIXES |
| Detailed Roast Breakdown | What we found |
| What's actually wrong: | Why this hurts you |
| Overall Profile Strength | Your CV score |
| Buzzword Detector | Overused words we found |
| ❌ BEFORE / ✅ AFTER (Rescued) | ❌ WHAT YOU WROTE / ✅ TRY THIS INSTEAD |
| Summary Rescue | Your summary, rewritten |
| Mentioned but NOT demonstrated in work bullets | You listed these but never showed them |
| Specificity / Impact / Evidence | Is it specific? / Does it show results? / Proof for skills |

Every section also gained a one-line explainer underneath, so nothing needs a glossary.

## Smaller fixes

- **The score could contradict its own bars.** `scores.overall` came straight from the
  model. It is now a weighted mean computed from the category scores, so the big number
  always agrees with the breakdown next to it. The model is no longer asked for it.
- **API keys are persisted.** They lived in memory only, so every restart wiped whatever
  you typed into settings. Now in `SharedPreferences`.
- **"Analysis failed… Retrying…"** was a lie; nothing retried. Real messages now.
- **Connection leak:** OkHttp response bodies were never closed. Wrapped in `.use {}`.
- **Gemini API key moved out of the URL** into the `x-goog-api-key` header, so it stops
  landing in logs and proxies.
- **Dead code removed:** the `Greeting()` composable and its screenshot test, which
  screenshotted the words "Hello Robolectric" and asserted nothing. The screenshot test
  now guards `ScoreRadialMeter`, the one piece of custom drawing in the app.
- **~330 lines of duplication deleted.** Gemini carried its own copy of the system prompt
  and the JSON parser, already drifting from the shared one. All three providers now
  implement a single `complete()` method on `LlmAnalysisService`; the request building,
  error handling and parsing live in one place, which is why the job-advert feature only
  needed one new prompt rather than three new network paths.
- The LinkedIn **"Fetch"** button never fetched anything, it just revealed the paste box.
  Relabelled "Next" and the copy no longer implies a fetch that cannot happen.

## Project setup

- **The Gradle wrapper was missing from the zip entirely** — no `gradlew`, no
  `gradlew.bat`, no `gradle-wrapper.jar`. Regenerated at 9.3.1, so the project builds from
  the command line and on CI, not just inside Android Studio.
- **`local.properties` was shipped pointing at `/Users/hassannd/Library/Android/sdk`**, so
  it broke on any other machine. It is gitignored and has been left out of this copy;
  Android Studio writes it on first open.
