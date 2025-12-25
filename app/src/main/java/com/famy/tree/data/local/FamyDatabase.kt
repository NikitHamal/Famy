package com.famy.tree.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.famy.tree.data.local.converter.Converters
import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.LifeEventDao
import com.famy.tree.data.local.dao.MediaDao
import com.famy.tree.data.local.dao.RelationshipDao
import com.famy.tree.data.local.entity.FamilyMemberEntity
import com.famy.tree.data.local.entity.FamilyTreeEntity
import com.famy.tree.data.local.entity.LifeEventEntity
import com.famy.tree.data.local.entity.MediaEntity
import com.famy.tree.data.local.entity.RelationshipEntity

@Database(
    entities = [
        FamilyTreeEntity::class,
        FamilyMemberEntity::class,
        RelationshipEntity::class,
        LifeEventEntity::class,
        MediaEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FamyDatabase : RoomDatabase() {

    abstract fun familyTreeDao(): FamilyTreeDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun mediaDao(): MediaDao

    companion object {
        const val DATABASE_NAME = "famy_database"

        @Volatile
        private var INSTANCE: FamyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE family_members ADD COLUMN middle_name TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN birth_place_latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN birth_place_longitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN death_place_latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN death_place_longitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN interests TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN career_status TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE family_members ADD COLUMN relationship_status TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Education enhancements
                db.execSQL("ALTER TABLE family_members ADD COLUMN education_level TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE family_members ADD COLUMN alma_mater TEXT DEFAULT NULL")

                // Skills and achievements
                db.execSQL("ALTER TABLE family_members ADD COLUMN skills TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN achievements TEXT DEFAULT NULL")

                // Employment details
                db.execSQL("ALTER TABLE family_members ADD COLUMN employer TEXT DEFAULT NULL")

                // Cultural/demographic info
                db.execSQL("ALTER TABLE family_members ADD COLUMN ethnicity TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN languages TEXT DEFAULT NULL")

                // Contact information
                db.execSQL("ALTER TABLE family_members ADD COLUMN phone TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN email TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN address TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN address_latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN address_longitude REAL DEFAULT NULL")

                // Social and medical
                db.execSQL("ALTER TABLE family_members ADD COLUMN social_links TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN medical_info TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN blood_type TEXT DEFAULT NULL")

                // Death and memorial
                db.execSQL("ALTER TABLE family_members ADD COLUMN cause_of_death TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN burial_place TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN burial_latitude REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE family_members ADD COLUMN burial_longitude REAL DEFAULT NULL")

                // Military service
                db.execSQL("ALTER TABLE family_members ADD COLUMN military_service TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): FamyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FamyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FamyDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
