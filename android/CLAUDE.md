# Android App Engineering Guide

Kotlin / ViewBinding / Retrofit2 / Coroutines. Targets API 26+. Package: `com.healthbot.app`.

## File structure

```
app/src/main/
  java/com/healthbot/app/
    api/
      HealthBotApi.kt      Retrofit interface — one suspend fun per endpoint
      RetrofitClient.kt    Singleton Retrofit instance (base URL: 10.0.2.2:8080)
      models/Models.kt     All request/response data classes
    utils/
      LocaleHelper.kt      EN/ZH locale switching + SharedPreferences
    MainActivity.kt        Home screen
    ChatbotActivity.kt     Main chat UI
    AiConsultationActivity.kt
    DoctorListActivity.kt
    VideoConsultationActivity.kt
    AppointmentActivity.kt
    PharmacyActivity.kt
    ChatAdapter.kt         RecyclerView adapter for chat messages
  res/
    layout/                One XML per Activity, plus item_* and bottom_sheet_*
    values/strings.xml     EN strings
    values-zh/strings.xml  ZH strings (must mirror values/strings.xml exactly)
    values/themes.xml      App theme
```

## Recipe: add a new screen

**Example: adding a "Health Tips" screen.**

1. **Add models** in `api/models/Models.kt`:
   ```kotlin
   data class HealthTip(
       val id: Long,
       val category: String,
       val content: String
   )
   ```

2. **Add API call** in `api/HealthBotApi.kt`:
   ```kotlin
   @GET("api/health-tips")
   suspend fun getHealthTips(): List<HealthTip>
   ```

3. **Create layout** `res/layout/activity_health_tips.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
       android:layout_width="match_parent" android:layout_height="match_parent"
       android:orientation="vertical">

       <androidx.appcompat.widget.Toolbar
           android:id="@+id/toolbar"
           android:layout_width="match_parent" android:layout_height="?attr/actionBarSize"
           android:background="?attr/colorPrimary" />

       <androidx.recyclerview.widget.RecyclerView
           android:id="@+id/rvTips"
           android:layout_width="match_parent" android:layout_height="match_parent" />
   </LinearLayout>
   ```

4. **Create Activity** `HealthTipsActivity.kt`:
   ```kotlin
   class HealthTipsActivity : AppCompatActivity() {

       private lateinit var binding: ActivityHealthTipsBinding

       override fun attachBaseContext(newBase: Context) {
           super.attachBaseContext(LocaleHelper.onAttach(newBase))
       }

       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           binding = ActivityHealthTipsBinding.inflate(layoutInflater)
           setContentView(binding.root)

           setSupportActionBar(binding.toolbar)
           supportActionBar?.setDisplayHomeAsUpEnabled(true)
           supportActionBar?.title = getString(R.string.health_tips)

           lifecycleScope.launch {
               try {
                   val tips = RetrofitClient.api.getHealthTips()
                   // bind tips to RecyclerView adapter
               } catch (e: Exception) {
                   Toast.makeText(this@HealthTipsActivity,
                       "Failed to load tips", Toast.LENGTH_SHORT).show()
               }
           }
       }

       override fun onSupportNavigateUp(): Boolean { finish(); return true }
   }
   ```

5. **Add strings** in both `values/strings.xml` and `values-zh/strings.xml`:
   ```xml
   <!-- values/strings.xml -->
   <string name="health_tips">Health Tips</string>

   <!-- values-zh/strings.xml -->
   <string name="health_tips">健康贴士</string>
   ```

6. **Register in AndroidManifest.xml**:
   ```xml
   <activity android:name=".HealthTipsActivity" android:screenOrientation="portrait" />
   ```

7. **Navigate to it** from another Activity:
   ```kotlin
   startActivity(Intent(this, HealthTipsActivity::class.java))
   ```

## Important conventions

- **ViewBinding always**: inflate `ActivityXxxBinding` — never `findViewById`
- **`attachBaseContext` always**: every Activity must override it and call `LocaleHelper.onAttach(newBase)` — omitting it breaks language switching
- **Toolbar + NoActionBar theme**: theme is `Theme.HealthBot` (parent `NoActionBar`); always call `setSupportActionBar(binding.toolbar)` manually — never rely on the window decor action bar
- **Coroutines for all network calls**: use `lifecycleScope.launch { }` + `try/catch` + `Toast.makeText()` on error
- **Never call Retrofit from the main thread**: all `RetrofitClient.api.*` calls are `suspend` — always inside a coroutine
- **`userId = 1L` hardcoded**: demo user is Alice Chen (id=1); pass it explicitly on every API call that needs it
- **String resources only**: never hardcode user-visible strings in Kotlin — add to both `values/strings.xml` and `values-zh/strings.xml`

## Key files quick reference

| File | Purpose |
|------|---------|
| `api/HealthBotApi.kt` | Retrofit interface — add new endpoint here |
| `api/models/Models.kt` | All data classes — add new request/response types here |
| `api/RetrofitClient.kt` | Singleton; base URL is `http://10.0.2.2:8080/` (emulator → host) |
| `utils/LocaleHelper.kt` | EN/ZH locale: `getLanguage()`, `setLocale()`, `onAttach()` |
| `res/values/themes.xml` | `Theme.HealthBot` extends `NoActionBar` — do not change the parent |

## Build requirements

- `gradle.properties` must contain `android.useAndroidX=true` and `android.enableJetifier=true`
- Min SDK 26, target SDK 34
- `android:usesCleartextTraffic="true"` in manifest allows HTTP to `10.0.2.2:8080` (emulator only)
