# What changed in this pass

## The downloaded CV would not open in Word

The exported .docx contained only three parts: `[Content_Types].xml`, `_rels/.rels` and
`word/document.xml`. That is enough for lenient readers, which is exactly why it got
shipped: the test round-tripped the file through the app's own DOCX reader and macOS
`textutil` opened it without complaint. Word is far stricter and called it unreadable.

The export now writes all seven parts a real .docx has:

| part | why Word wants it |
|---|---|
| `word/_rels/document.xml.rels` | **the missing one.** Word treats a document part with no relationships part as damaged |
| `word/styles.xml` | something real to lay the text out with |
| `docProps/core.xml`, `docProps/app.xml` | document properties, referenced from the package relationships |
| `<w:sectPr>` in the document | page size and margins, rather than leaving Word to guess |

Five new tests enforce it, and they check structure rather than readability: every part
present, every declared content type resolving to a part that exists, and every
relationship target resolving to a part that exists. The old "we can read our own file"
test is the one that let this through, so it is no longer the only check.

### About the file being 4.5 KB when the original was 130 KB

That part is expected and is not a fault. The CV arrives as a PDF, and only its **text**
survives extraction: the embedded fonts, images, and layout that make up most of those
130 KB cannot be recovered from extracted text. The export is the rewritten wording in a
clean document, not a copy of the original with edits applied.

If preserving the original formatting matters, the way to get it is to upload a **.docx**
rather than a PDF, and have the export rewrite the text inside the original file. That is
a different feature and is not built yet.

---

## Renamed to Roastumé

App label, top bar, share card and project name.

One deliberate inconsistency: the Gradle `rootProject.name` is the ASCII `Roastume`,
without the accent. It is not user-visible, and it ends up in generated paths and build
artefact names where non-ASCII characters cause trouble on some filesystems and CI.
Everything a user sees says Roastumé.

## The project would not open in Android Studio

`.idea/` was committed to the repo, and `.idea/misc.xml` pinned
`project-jdk-name="jbr-25"` with `languageLevel="JDK_25"`, while `.idea/compiler.xml`
pinned `bytecodeTargetLevel target="25"`. Those came from the machine the project was
first created on. Anyone whose Android Studio ships a different JBR opens the project
against a JDK that does not exist for them.

`.idea/` is now ignored in full and removed from version control. Android Studio
regenerates it per machine on first open.

Note this was never specific to the `dev` branch: `dev` and `main` were the same commit
when the problem was reported, so it was present on both, and had been since the initial
commit.

If it still fails to open after pulling, the next thing to check is the Gradle JDK, under
Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK. The build
also asks for a Java 25 toolchain in `gradle/gradle-daemon-jvm.properties`, which Gradle
will download by itself, but only if it can reach the network.

---

## Removed the LinkedIn input

The LinkedIn tab is gone. It never fetched anything: LinkedIn blocks automated profile
reads, so the "Fetch" button only ever revealed a paste box, which the Paste Text tab
already does. Two tabs now: **Upload CV** and **Paste Text**.

The hero line went with it, since it promised something the app no longer offers. Pasting
LinkedIn profile text still works perfectly well through Paste Text, and the prompt still
understands it.

---

## New: "Apply fixes & download" on the uploaded CV

On the Fixes tab there is now a card that takes the rewrites you have just read, puts them
back into your own CV, and saves the result as a **.docx** you can open in Word or Google
Docs.

**It edits, it does not regenerate.** The rewrite is a find-and-replace over the text of
the CV you uploaded: the rescued summary swaps in for the old one, each rewritten bullet
replaces the line it came from, and everything else is left exactly as you wrote it. No
second trip to the model, so the download cannot contain an employer, a date or a number
you have not already seen on screen.

Details that came out of testing:

- **Matching tolerates whitespace.** PDF extraction often hard-wraps a long bullet across
  two lines, so the line the model quotes back rarely matches the document character for
  character. Matching is done on a whitespace-normalised view of the text and mapped back
  to real offsets, which is what makes the wrapped case work.
- **Suggestions that cannot be placed are reported, not dropped.** If a line cannot be
  found the card says how many were left for you to apply by hand, rather than quietly
  producing a file with fewer changes than you expected.
- **Only the first occurrence of a repeated line is replaced.**
- **Saving needs no permissions.** On Android 10 and later the file goes straight into
  Downloads through MediaStore. Below that, scoped storage does not exist and writing to
  Downloads would mean requesting WRITE_EXTERNAL_STORAGE, so the file is written inside
  the app and handed to the system share sheet instead. A FileProvider is declared for
  that path.
- The .docx is built by hand rather than with a document library: a docx is a zip holding
  three small XML parts, which is not worth a dependency. It is verified by reading it
  back with the app's own DOCX reader, and by round-tripping text containing `&`, `<`,
  quotes and control characters.

13 new tests cover the rewriter, the docx writer and the exporter. 41 tests in total,
all passing.

---

## PDF upload: fixed properly this time

**Symptom:** uploading a résumé did nothing, or failed.

**Cause:** the PDF reader was hand-rolled. It pulled `(text) Tj` operators out of the file
and hoped. That only works on uncompressed PDFs written with simple fonts. Anything
exported from **Google Docs, Word, Canva, Pages or LaTeX** uses Type0 fonts with
**Identity-H** encoding, where the bytes inside `(...)` are two-byte *glyph IDs*, not
letters. Turning them back into text requires the font's ToUnicode CMap, which the parser
knew nothing about.

On a Chrome-exported CV it did not merely return junk: it **crashed with a
StackOverflowError**, the regex backtracking over binary glyph data.

Worse, the repo contained unit tests for that parser covering hex strings, TJ arrays and
UTF-16BE, and they all passed, because they fed it synthetic strings like `(Hello) Tj`
that no real PDF contains. Green tests, broken app.

**Fix:** PDF extraction now goes through **PDFBox** (`com.tom-roush:pdfbox-android`,
Apache-2.0), which already handles every stream filter, encoding, CMap and subset font.
The hand-rolled parser and its synthetic tests are gone.

Two details worth knowing:

- `sortByPosition` is deliberately **off**. With it on, a two-column CV is read straight
  across the page and the sidebar interleaves into the job history line by line
  (`"SKILLS EXPERIENCE"`, `"Python, Spark, Airflow, Senior Data Engineer, Meridian..."`).
  Content-stream order keeps each column intact. This was measured, not guessed.
- Failure messages are now specific. A scanned PDF says the words are a picture and to
  paste the text instead, rather than one catch-all sentence.

**Cost:** the APK grows from ~23 MB to ~31 MB. That is the price of reading the files
people actually have.

**Tested against real files**, not strings: a Chrome/Skia export (the Google Docs and Word
case), a two-column CV, a CV with accented names and smart quotes (`José Álvarez-Ferré`,
`Zürich`), a simple uncompressed PDF, a DOCX, and 50 KB of random bytes to prove it fails
cleanly instead of fatally. 23/23 tests pass.

---

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
