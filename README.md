# Cisco Webex Android SDK Example

Using the [Webex Android SDK](https://github.com/webex/webex-android-sdk) - this instruments the code using OpenTelemetry, with a few updates that were made:
- enable core library desugaring by default within the SDK
- bumped source/targetCompatibility and jvmTarget to VERSION_17
- bumped Kotlin version to 1.9.20
- bumped compileSDK to 34 (minSDK still set to 24)
- updated the code to use ViewBindings (required for otel)
- upgraded Gradle AGP to use v8.0.2
- probably a few other things I'll remember


## Table of Contents

- [Usage](#usage)
- [Todo](#todo)

## Usage

You'll need to update local.properties with any OpenTelemetry collector details OR endpoints for events/traces. This currently uses Splunk RUM to report metrics, but
this can be modified according to however you are exporting traces.

## Todo

- More OOTB instrumentation
- Add helper functions for GPS and Mobile data
- Bump Gradle AGP to 8.4.x?
- 
