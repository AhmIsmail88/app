package com.rasd.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.rasd.app.data.dao.*
import com.rasd.app.data.database.RasdDatabase
import com.rasd.app.repository.*
import com.rasd.app.report.ReportBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN academicYear TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN logoPath TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE assessments ADD COLUMN term TEXT NOT NULL DEFAULT 'termly'")
        }
    }

    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RasdDatabase =
        Room.databaseBuilder(context, RasdDatabase::class.java, "rasd.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    @Singleton
    fun provideAssessmentDao(database: RasdDatabase): AssessmentDao = 
        database.assessmentDao()
    
    @Provides
    @Singleton
    fun provideAppSettingsDao(database: RasdDatabase): AppSettingsDao = 
        database.appSettingsDao()
    
    @Provides
    @Singleton
    fun provideTeacherCommentDao(database: RasdDatabase): TeacherCommentDao = 
        database.teacherCommentDao()
    
    @Provides
    @Singleton
    fun provideAssessmentScoreDao(database: RasdDatabase): AssessmentScoreDao = 
        database.assessmentScoreDao()
    
    @Provides
    @Singleton
    fun provideClassPeriodDao(database: RasdDatabase): ClassPeriodDao = 
        database.classPeriodDao()
    
    @Provides
    @Singleton
    fun provideAssessmentRepository(assessmentDao: AssessmentDao): AssessmentRepository {
        return AssessmentRepository(assessmentDao)
    }
    
    @Provides
    @Singleton
    fun provideTeacherCommentRepository(teacherCommentDao: TeacherCommentDao): TeacherCommentRepository {
        return TeacherCommentRepository(teacherCommentDao)
    }
    
    @Provides
    @Singleton
    fun provideClassPeriodRepository(periodDao: ClassPeriodDao): ClassPeriodRepository {
        return ClassPeriodRepository(periodDao)
    }

    @Provides
    @Singleton
    fun provideClassDao(database: RasdDatabase): ClassDao = database.classDao()

    @Provides
    @Singleton
    fun provideStudentDao(database: RasdDatabase): StudentDao = database.studentDao()

    @Provides
    @Singleton
    fun provideAttendanceRecordDao(database: RasdDatabase): AttendanceRecordDao = database.attendanceRecordDao()

    @Provides
    @Singleton
    fun provideBehaviorTypeDao(database: RasdDatabase): BehaviorTypeDao = database.behaviorTypeDao()

    @Provides
    @Singleton
    fun provideBehaviorEventDao(database: RasdDatabase): BehaviorEventDao = database.behaviorEventDao()

    @Provides
    @Singleton
    fun provideReportBuilder(
        classDao: ClassDao,
        studentDao: StudentDao,
        attendanceRecordDao: AttendanceRecordDao,
        assessmentDao: AssessmentDao,
        assessmentScoreDao: AssessmentScoreDao,
        behaviorEventDao: BehaviorEventDao,
        behaviorTypeDao: BehaviorTypeDao
    ): ReportBuilder = ReportBuilder(
        classDao, studentDao, attendanceRecordDao, assessmentDao,
        assessmentScoreDao, behaviorEventDao, behaviorTypeDao
    )
}