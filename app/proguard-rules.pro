-keep class com.stoneshield.app.domain.** { *; }

-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keepclassmembers class * {
    @dagger.hilt.android.internal.lifecycle.HiltViewModelMap <fields>;
}

-keepclassmembers class * {
    @com.google.dagger.hilt.android.internal.lifecycle.HiltViewModelMap.KeySet <fields>;
}
