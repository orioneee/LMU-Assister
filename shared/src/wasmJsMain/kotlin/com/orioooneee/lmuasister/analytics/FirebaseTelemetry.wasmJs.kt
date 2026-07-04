package com.orioooneee.lmuasister.analytics

import com.orioooneee.lmuasister.config.BuildConfig
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun initWebTelemetry() {
    if (!BuildConfig.FIREBASE_WEB_ANALYTICS_ENABLED) return

    initFirebaseAnalyticsJs(
        apiKey = BuildConfig.FIREBASE_WEB_API_KEY,
        authDomain = BuildConfig.FIREBASE_WEB_AUTH_DOMAIN,
        projectId = BuildConfig.FIREBASE_WEB_PROJECT_ID,
        storageBucket = BuildConfig.FIREBASE_WEB_STORAGE_BUCKET,
        messagingSenderId = BuildConfig.FIREBASE_WEB_MESSAGING_SENDER_ID,
        appId = BuildConfig.FIREBASE_WEB_APP_ID,
        measurementId = BuildConfig.FIREBASE_WEB_MEASUREMENT_ID,
    )

    Telemetry.analytics = WebFirebaseAnalyticsSink
    Telemetry.userProperty(UserProperties.PLATFORM, "web")
}

private object WebFirebaseAnalyticsSink : Analytics {
    override fun logEvent(event: AnalyticsEvent) {
        logFirebaseEventJs(event.name, event.params.toAnalyticsParamsJson())
    }

    override fun logScreenView(screenName: String) {
        logFirebaseEventJs("screen_view", screenViewParamsJson(screenName))
    }

    override fun setUserId(id: String?) {
        setFirebaseUserIdJs(id)
    }

    override fun setUserProperty(key: String, value: String?) {
        setFirebaseUserPropertiesJs(userPropertyParamsJson(key, value))
    }
}

private fun Map<String, Any?>.toAnalyticsParamsJson(): String =
    buildJsonObject {
        forEach { (key, value) ->
            val param = when (value) {
                null -> null
                is String -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value.toString())
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value.toDouble())
                else -> JsonPrimitive(value.toString())
            }
            if (param != null) put(key, param)
        }
    }.toString()

private fun screenViewParamsJson(screenName: String): String =
    buildJsonObject {
        put("firebase_screen", JsonPrimitive(screenName))
        put("firebase_screen_class", JsonPrimitive("ComposeWasm"))
    }.toString()

private fun userPropertyParamsJson(key: String, value: String?): String =
    buildJsonObject {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }.toString()

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun initFirebaseAnalyticsJs(
    apiKey: String,
    authDomain: String,
    projectId: String,
    storageBucket: String,
    messagingSenderId: String,
    appId: String,
    measurementId: String,
): Unit =
    js(
        """
        {
          const state = globalThis.__lmuFirebaseAnalytics || {
            ready: false,
            loading: false,
            failed: false,
            queue: []
          };
          globalThis.__lmuFirebaseAnalytics = state;

          state.enqueue = state.enqueue || ((task) => {
            if (state.failed) return;
            if (state.ready && typeof state.send === "function") {
              state.send(task);
            } else {
              state.queue.push(task);
            }
          });

          if (!state.loading && !state.ready) {
            state.loading = true;

            const config = {
              apiKey,
              authDomain,
              projectId,
              storageBucket,
              messagingSenderId,
              appId,
              measurementId
            };

            Promise.all([
              import(/* webpackIgnore: true */ "https://www.gstatic.com/firebasejs/12.15.0/firebase-app.js"),
              import(/* webpackIgnore: true */ "https://www.gstatic.com/firebasejs/12.15.0/firebase-analytics.js")
            ]).then(async ([appModule, analyticsModule]) => {
              if (typeof analyticsModule.isSupported === "function") {
                const supported = await analyticsModule.isSupported();
                if (!supported) {
                  state.failed = true;
                  state.loading = false;
                  return;
                }
              }

              const app = appModule.getApps && appModule.getApps().length > 0
                ? appModule.getApp()
                : appModule.initializeApp(config);
              const analytics = analyticsModule.getAnalytics(app);

              state.send = (task) => {
                try {
                  if (task.type === "event") {
                    analyticsModule.logEvent(analytics, task.name, task.params || {});
                  } else if (task.type === "user_id") {
                    analyticsModule.setUserId(analytics, task.value ?? null);
                  } else if (task.type === "user_properties") {
                    analyticsModule.setUserProperties(analytics, task.params || {});
                  }
                } catch (error) {
                  console.warn("LMU Firebase Analytics event failed", error);
                }
              };

              state.ready = true;
              state.loading = false;
              const queued = state.queue.splice(0, state.queue.length);
              queued.forEach(state.send);
            }).catch((error) => {
              state.failed = true;
              state.loading = false;
              console.warn("LMU Firebase Analytics init failed", error);
            });
          }
        }
        """,
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun logFirebaseEventJs(eventName: String, paramsJson: String): Unit =
    js(
        """
        {
          const state = globalThis.__lmuFirebaseAnalytics;
          if (state && typeof state.enqueue === "function") {
            let params = {};
            try {
              params = JSON.parse(paramsJson || "{}");
            } catch (_) {
              params = {};
            }
            state.enqueue({ type: "event", name: eventName, params });
          }
        }
        """,
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun setFirebaseUserIdJs(userId: String?): Unit =
    js(
        """
        {
          const state = globalThis.__lmuFirebaseAnalytics;
          if (state && typeof state.enqueue === "function") {
            state.enqueue({ type: "user_id", value: userId == null ? null : userId });
          }
        }
        """,
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun setFirebaseUserPropertiesJs(paramsJson: String): Unit =
    js(
        """
        {
          const state = globalThis.__lmuFirebaseAnalytics;
          if (state && typeof state.enqueue === "function") {
            let params = {};
            try {
              params = JSON.parse(paramsJson || "{}");
            } catch (_) {
              params = {};
            }
            state.enqueue({ type: "user_properties", params });
          }
        }
        """,
    )
