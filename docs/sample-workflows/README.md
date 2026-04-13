# Sample build workflows

Yoinkins is a **build driver**, not a build author. It triggers GitHub Actions on your repo, watches the run, and downloads the resulting APK. It does not modify your repo or generate configs.

## Requirements

Your repo must provide:

- `.github/workflows/build-apk.yml` containing a job with a `workflow_dispatch` trigger.
- The workflow must upload an artifact named **`app-debug`** that contains the debug APK.

That's the whole contract. Anything beyond that (SDK setup, signing, caching, framework-specific build steps) is up to you.

## Starter templates

The files in this directory are reference workflows you can copy into your own repo at `.github/workflows/build-apk.yml` and customize. They are **not** injected by the app — you commit them yourself.

| File | For |
| --- | --- |
| [`android.yml`](android.yml) | Plain Android / Gradle projects |
| [`flutter.yml`](flutter.yml) | Flutter apps |
| [`react-native.yml`](react-native.yml) | React Native apps |
| [`godot.yml`](godot.yml) | Godot 4.x projects (expects `export_presets.cfg` with an Android preset committed to the repo) |

## Why your repo owns the config

Putting build configuration in your repo means:

- The build is reproducible from `git clone` + `gh workflow run` — no magic external agent required.
- You can edit, audit, and version the workflow like any other source file.
- Yoinkins stays a thin client: trigger, poll, deliver.
