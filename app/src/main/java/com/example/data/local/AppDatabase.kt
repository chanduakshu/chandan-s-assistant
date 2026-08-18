package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CallEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chandan_assistant_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.callDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(callDao: CallDao) {
            val now = System.currentTimeMillis()
            val sampleCalls = listOf(
                CallEntity(
                    callerName = "Ramesh Kumar",
                    phoneNumber = "+91 98450 12345",
                    organization = "ABC Logistics / BlueDart",
                    timestamp = now - 1000 * 60 * 45, // 45 mins ago
                    durationSeconds = 102,
                    language = "Kannada",
                    category = "Delivery / Courier",
                    summary = "Caller contacted Chandan regarding a package delivery scheduled for today. AI instructed driver to leave package with the security guard at the gate.",
                    importantDetails = "Package from Amazon, tracking #BL9921, instructed to leave with gate security.",
                    transcriptJson = """[{"sender":"AI_ASSISTANT","text":"ನಮಸ್ಕಾರ, ನಾನು ಚಂದನ್ ಅವರ AI Assistant. ಅವರು ಈಗ ಬ್ಯುಸಿಯಾಗಿದ್ದಾರೆ. ಯಾರು ಮಾತನಾಡುತ್ತಿದ್ದೀರಿ ಮತ್ತು ಏಕೆ ಕರೆ ಮಾಡಿದ್ದೀರಿ?"},{"sender":"CALLER","text":"ನಮಸ್ಕಾರ ಸರ್, ನಾನು ರಮೇಶ್ ಕುಮಾರ್, ABC ಲಾಜಿಸ್ಟಿಕ್ಸ್‌ನಿಂದ ಪಾರ್ಸೆಲ್ ಡೆಲಿವರಿ ಮಾಡಲು ಬಂದಿದ್ದೇನೆ."},{"sender":"AI_ASSISTANT","text":"ಸರಿ. ಪಾರ್ಸೆಲ್ ಅನ್ನು ಗೇಟ್ನಲ್ಲಿರುವ ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಬಿಡಿ. ಧನ್ಯವಾದಗಳು."},{"sender":"CALLER","text":"ಸರಿ ಮೇಡಂ, ಧನ್ಯವಾದಗಳು. ಸೆಕ್ಯುರಿಟಿ ಗಾರ್ಡ್ ಬಳಿ ಇಡುತ್ತೇನೆ."}]""",
                    hasRecording = true,
                    recordingDuration = 102,
                    isTrusted = true,
                    isSpam = false,
                    screeningStatus = "Screened by AI"
                ),
                CallEntity(
                    callerName = "Pooja Sharma",
                    phoneNumber = "+91 80 4912 3456",
                    organization = "HDFC Credit Cards / Telemarketing",
                    timestamp = now - 1000 * 60 * 180, // 3 hours ago
                    durationSeconds = 35,
                    language = "English",
                    category = "Spam / Telemarketing",
                    summary = "Telemarketing call offering pre-approved life-time free credit cards with loan schemes. AI politely declined on behalf of Chandan and terminated call.",
                    importantDetails = "Pre-approved platinum card offer. Caller attempted sales pitch twice.",
                    transcriptJson = """[{"sender":"AI_ASSISTANT","text":"Hi, I'm Chandan's AI Assistant. He's currently busy. May I know who's calling and why, please?"},{"sender":"CALLER","text":"Good afternoon! I am Pooja from HDFC Bank calling to offer a lifetime free pre-approved credit card with instant loan eligibility."},{"sender":"AI_ASSISTANT","text":"Thank you, but Chandan isn't interested right now. Have a great day!"},{"sender":"CALLER","text":"Sir, only 2 minutes required for activation..."},{"sender":"AI_ASSISTANT","text":"Chandan is not interested. Call disconnected."}]""",
                    hasRecording = true,
                    recordingDuration = 35,
                    isTrusted = false,
                    isSpam = true,
                    screeningStatus = "Spam Blocked"
                ),
                CallEntity(
                    callerName = "Vikram Aditya",
                    phoneNumber = "+91 99001 77889",
                    organization = "InnovateX Tech (Engineering Team)",
                    timestamp = now - 1000 * 60 * 60 * 12, // 12 hours ago
                    durationSeconds = 85,
                    language = "Kannada-English",
                    category = "Work / Professional",
                    summary = "Engineering lead reached out regarding deployment window and Sprint review synchronization. Urgent code review required on PR #402.",
                    importantDetails = "PR #402 needs review before 4 PM release window. Meeting shifted to 5:30 PM.",
                    transcriptJson = """[{"sender":"AI_ASSISTANT","text":"Hi, I'm Chandan's AI Assistant. He's currently busy. May I know who's calling and why, please?"},{"sender":"CALLER","text":"Hey, Vikram here from InnovateX. Chandan ge urgent PR review schedule bagge inform maadbekittu."},{"sender":"AI_ASSISTANT","text":"Thank you. I've noted the details and will inform Chandan."},{"sender":"CALLER","text":"Please tell him to check PR #402 before 4 PM deployment."},{"sender":"AI_ASSISTANT","text":"I understand. I'll mark this as urgent for Chandan."}]""",
                    hasRecording = true,
                    recordingDuration = 85,
                    isTrusted = true,
                    isSpam = false,
                    screeningStatus = "Screened by AI"
                ),
                CallEntity(
                    callerName = "Amma",
                    phoneNumber = "+91 94481 65432",
                    organization = "Home / Family",
                    timestamp = now - 1000 * 60 * 60 * 26, // Yesterday
                    durationSeconds = 64,
                    language = "Kannada",
                    category = "Personal / Family",
                    summary = "Mother called to check on dinner plans and requested to bring fresh fruits while coming home.",
                    importantDetails = "Reminded to buy fruits from Malleshwaram market. Free for dinner around 8:30 PM.",
                    transcriptJson = """[{"sender":"AI_ASSISTANT","text":"ನಮಸ್ಕಾರ, ನಾನು ಚಂದನ್ ಅವರ AI Assistant. ಅವರು ಈಗ ಬ್ಯುಸಿಯಾಗಿದ್ದಾರೆ. ಯಾರು ಮಾತನಾಡುತ್ತಿದ್ದೀರಿ ಮತ್ತು ಏಕೆ ಕರೆ ಮಾಡಿದ್ದೀರಿ?"},{"sender":"CALLER","text":"ಚಂದನ್, ಅಮ್ಮ ಕಣೋ. ಸಂಜೆ ಮನೆಗೆ ಬರುವಾಗ ಹಣ್ಣು ತಗೊಂಡು ಬಾ ಅಂತ ಹೇಳೋಕೆ ಕರೆ ಮಾಡಿದೆ."},{"sender":"AI_ASSISTANT","text":"ಧನ್ಯವಾದಗಳು. ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ದಾಖಲಿಸಿದ್ದೇನೆ ಮತ್ತು ಚಂದನ್ ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ."},{"sender":"CALLER","text":"ಸರಿ, ಬೇಗ ಬರೋಕೆ ಹೇಳು."}]""",
                    hasRecording = true,
                    recordingDuration = 64,
                    isTrusted = true,
                    isSpam = false,
                    screeningStatus = "Screened by AI"
                )
            )

            for (call in sampleCalls) {
                callDao.insertCall(call)
            }
        }
    }
}
