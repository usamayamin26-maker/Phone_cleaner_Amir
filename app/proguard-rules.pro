# Project specific R8 rules. Libraries ship their own consumer rules (Room, kotlinx.serialization,
# Koin, LiteRT); add a rule here only for something this app reaches by name, with a comment that
# says what breaks without it.

# Keep line numbers so a release crash still points at the right line.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
