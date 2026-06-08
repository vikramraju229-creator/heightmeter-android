# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.heightmeter.app.measurement.model.Point3D { *; }
-keep class com.heightmeter.app.measurement.model.MeasurementResult { *; }
