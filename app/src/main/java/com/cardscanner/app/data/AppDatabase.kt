package com.cardscanner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    // Unit separator: never typed on a business card, so it is safe as a list delimiter.
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(SEPARATOR)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}

@Database(entities = [Card::class, Project::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cards.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Version 1 had no projects; existing cards move into a "My cards" project. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("INSERT INTO projects (id, name, createdAt) VALUES (1, 'My cards', ${System.currentTimeMillis()})")
                // SQLite can't add a foreign key to an existing table, so the cards table is rebuilt.
                db.execSQL(
                    "CREATE TABLE `cards_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`projectId` INTEGER NOT NULL, `name` TEXT NOT NULL, `jobTitle` TEXT NOT NULL, " +
                        "`company` TEXT NOT NULL, `phones` TEXT NOT NULL, `emails` TEXT NOT NULL, " +
                        "`website` TEXT NOT NULL, `address` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                        "`imagePath` TEXT, `rawText` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO cards_new (id, projectId, name, jobTitle, company, phones, emails, website, " +
                        "address, notes, imagePath, rawText, createdAt) SELECT id, 1, name, jobTitle, company, " +
                        "phones, emails, website, address, notes, imagePath, rawText, createdAt FROM cards"
                )
                db.execSQL("DROP TABLE cards")
                db.execSQL("ALTER TABLE cards_new RENAME TO cards")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_projectId` ON `cards` (`projectId`)")
            }
        }
    }
}
