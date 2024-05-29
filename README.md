# Cisco Webex Android SDK Example

Using the [Webex Android SDK](https://github.com/webex/webex-android-sdk) - this instruments the code using OpenTelemetry, with a few updates that were made:
- enable core library desugaring by default within the SDK
- bumped source/targetCompatibility and jvmTarget to VERSION_17
- updated the code to use ViewBindings (required for otel)
- probably a few other things I'll remember


## Table of Contents

- [Usage](#usage)
- [Note](#note)

## Usage

You'll need to update local.properties with any OpenTelemetry collector details OR endpoints for events/traces. This currently uses Splunk RUM to report metrics, but
this can be modified according to how you are exporting traces.

## Note

 Please update the below constant in gradle.properties
 ```
 SCOPE=""
 ```

 Please update below constants in local.properties file
 ```
 CLIENT_ID=""
 CLIENT_SECRET=""
 REDIRECT_URI=""
 WEBHOOK_URL=""
 ```
