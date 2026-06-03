# Игнорировать предупреждения от сторонних библиотек и Minecraft
-dontwarn

# Отключаем сжатие и оптимизацию bytecode, оставляя исключительно обфускацию имён (переименование).
# Это обеспечивает максимальную совместимость и безопасность в среде Fabric.
-dontshrink
-dontoptimize

# Использование арабского словаря для обфускации имен классов, полей, методов и пакетов
-classobfuscationdictionary arabic-dictionary.txt
-obfuscationdictionary arabic-dictionary.txt
-packageobfuscationdictionary arabic-dictionary.txt

# Сохраняем важные атрибуты метаданных (аннотации, подписи дженериков, отладочную информацию для краш-логов)
-keepattributes SourceFile,LineNumberTable,Deprecated,Signature,InnerClasses,EnclosingMethod,*Annotation*,MethodParameters

# Сохраняем все точки входа Fabric Loader и их члены
-keep class * implements net.fabricmc.api.ModInitializer { *; }
-keep class * implements net.fabricmc.api.ClientModInitializer { *; }
-keep class * implements net.fabricmc.api.DedicatedServerModInitializer { *; }

# Сохраняем главный инициализатор мода
-keep class farvix.solution.Client { *; }

# Полностью сохраняем пакет с миксинами (Mixin-классы нельзя обфусцировать, иначе они не внедрятся в Minecraft)
-keep class farvix.solution.mixins.** { *; }

# Сохраняем интерфейсы аннотаций
-keep @interface farvix.solution.**

# Сохраняем все перечисления (enum) и их элементы нетронутыми во избежание EnumConstantNotPresentException
-keep class * extends java.lang.Enum { *; }

# Предотвращаем обфускацию встроенных библиотек (addtojar), чтобы не нарушить рефлексию
-keep class meteordevelopment.orbit.** { *; }
-keep class org.json.** { *; }
-keep class dev.redstones.mediaplayerinfo.** { *; }
-keep class com.jagrosh.** { *; }
-keep class com.github.jagrosh.** { *; }
-keep class club.minnced.discord.rpc.** { *; }

# Сохраняем системные и служебные библиотеки среды выполнения Minecraft/Fabric
-keep class org.slf4j.** { *; }
-keep class org.freedesktop.** { *; }
-keep class org.scijava.** { *; }
-keep class org.intellij.** { *; }
-keep class org.jetbrains.** { *; }
