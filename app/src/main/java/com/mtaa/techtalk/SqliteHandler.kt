package com.mtaa.techtalk

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.mtaa.techtalk.activities.CREATED_AT

const val DATABASE_NAME = "account.db"
const val DATABASE_VERSION = 1

const val REVIEW_TABLE = "reviews"
const val PHOTO_TABLE = "photos"
const val ATTRIBUTES_TABLE = "attributes"

//Review columns
const val COLUMN_ID = "id"
const val COLUMN_REVIEW_ID = "review_id"
const val COLUMN_PRODUCT_ID = "product_id"
const val COLUMN_USER_ID = "user_id"
const val COLUMN_TEXT = "text"
const val COLUMN_SCORE = "score"
const val COLUMN_LIKES = "likes"
const val COLUMN_DISLIKES = "dislikes"
const val COLUMN_CREATED_AT = "created_at"
const val COLUMN_VOTE_STATUS = "vote_status"   //Tracking likes, dislikes changes

//Attributes columns
const val COLUMN_IS_POSITIVE = "is_positive"

//Photos columns
const val COLUMN_PATH = "path"

class SqliteHandler(context: Context,factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME,factory, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_REVIEW_TABLE =
            ("CREATE TABLE " + REVIEW_TABLE + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_REVIEW_ID + " INTEGER," +
                COLUMN_USER_ID + " INTEGER," +
                COLUMN_PRODUCT_ID + " INTEGER," +
                COLUMN_TEXT + " TEXT," +
                COLUMN_SCORE + " INTEGER," +
                COLUMN_LIKES + " INTEGER," +
                COLUMN_DISLIKES + " INTEGER," +
                CREATED_AT + " TEXT," +
                COLUMN_VOTE_STATUS + " TEXT" + ")")

        val CREATE_PHOTO_TABLE =
            ("CREATE TABLE " + PHOTO_TABLE  + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REVIEW_ID + " INTEGER,"+
                    COLUMN_VOTE_STATUS + " TEXT,"+
                    " FOREIGN KEY ($COLUMN_REVIEW_ID) REFERENCES $REVIEW_TABLE($COLUMN_ID));")

        val CREATE_ATTRIBUTES_TABLE =
            ("CREATE TABLE " + ATTRIBUTES_TABLE + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REVIEW_ID + " INTEGER," +
                    COLUMN_PATH + " TEXT," +
                    " FOREIGN KEY ($COLUMN_REVIEW_ID) REFERENCES $REVIEW_TABLE($COLUMN_ID));")


        db.execSQL(CREATE_REVIEW_TABLE)
        db.execSQL(CREATE_PHOTO_TABLE)
        db.execSQL(CREATE_ATTRIBUTES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $ATTRIBUTES_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $PHOTO_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $REVIEW_TABLE")
        onCreate(db)
    }

    fun addReview(reviewInfo:ReviewInfoItem)
    {
        val db = writableDatabase //Get db

        val values = ContentValues()
        values.put(COLUMN_REVIEW_ID,reviewInfo.review_id)
        values.put(COLUMN_USER_ID,reviewInfo.user_id)
        values.put(COLUMN_PRODUCT_ID,reviewInfo.product_id)
        values.put(COLUMN_TEXT,reviewInfo.text)
        values.put(COLUMN_SCORE,reviewInfo.score)
        values.put(COLUMN_LIKES,reviewInfo.likes)
        values.put(COLUMN_DISLIKES,reviewInfo.dislikes)
        values.put(CREATED_AT,reviewInfo.created_at)
        values.put(COLUMN_VOTE_STATUS,"none")

        db.insert(REVIEW_TABLE,null,values)
    }

    @SuppressLint("Recycle")
    fun getAllReviews(): MutableList<ReviewInfoItem> {

        val reviews = mutableListOf<ReviewInfoItem>()
        val db = writableDatabase
        var reviewsCursor : Cursor?
        try {
            reviewsCursor = db.rawQuery("select * from $REVIEW_TABLE", null)
        } catch (e: SQLiteException) {
            onCreate(db)
            return mutableListOf()
        }

        //Check all data
        if (reviewsCursor!!.moveToFirst()) {
            while (!reviewsCursor.isAfterLast) {
                val reviewId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_REVIEW_ID))
                val userId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_USER_ID))
                val productId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_PRODUCT_ID))
                val text = reviewsCursor.getString(reviewsCursor.getColumnIndex(COLUMN_TEXT))
                val score = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_SCORE))
                val likes = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_LIKES))
                val dislikes = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_DISLIKES))
                val createdAt = reviewsCursor.getString(reviewsCursor.getColumnIndex(COLUMN_CREATED_AT))
                val status = reviewsCursor.getString(reviewsCursor.getColumnIndex(COLUMN_VOTE_STATUS))

                reviews.add(ReviewInfoItem(text, mutableListOf(),mutableListOf(),likes,dislikes,productId,score,userId,reviewId,createdAt))
                /*userid = reviewsCursor.getString(reviewsCursor.getColumnIndex(DBContract.UserEntry.COLUMN_USER_ID))
                name = reviewsCursor.getString(reviewsCursor.getColumnIndex(DBContract.UserEntry.COLUMN_NAME))
                age = reviewsCursor.getString(reviewsCursor.getColumnIndex(DBContract.UserEntry.COLUMN_AGE))

                users.add(UserModel(userid, name, age))*/
                reviewsCursor.moveToNext()
            }
        }
        return reviews
    }

    fun deleteReviews() {
        val db = writableDatabase
        db.execSQL("delete from $REVIEW_TABLE")
    }

    /*fun readAllUsers(): ArrayList<UserModel> {
        val users = ArrayList<UserModel>()
        val db = writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("select * from " + DBContract.UserEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_ENTRIES)
            return ArrayList()
        }

        var userid: String
        var name: String
        var age: String
        if (cursor!!.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                userid = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_USER_ID))
                name = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_NAME))
                age = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_AGE))

                users.add(UserModel(userid, name, age))
                cursor.moveToNext()
            }
        }
        return users
    }*/

    /*fun insertUser(user: UserModel): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.UserEntry.COLUMN_USER_ID, user.userid)
        values.put(DBContract.UserEntry.COLUMN_NAME, user.name)
        values.put(DBContract.UserEntry.COLUMN_AGE, user.age)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.UserEntry.TABLE_NAME, null, values)

        return true
    }*/
}