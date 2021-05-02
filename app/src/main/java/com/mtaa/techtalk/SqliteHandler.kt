package com.mtaa.techtalk
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import androidx.activity.ComponentActivity
import com.mtaa.techtalk.activities.CREATED_AT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

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
const val COLUMN_UPDATE_STATUS = "update_status"   //Tracking updates

//Attributes columns
const val COLUMN_IS_POSITIVE = "is_positive"

//Photos columns
const val COLUMN_PATH = "path"
const val COLUMN_PHOTO_ID = "photo_id"

//Commands
const val NONE = 0
const val DELETE_ITEM = 3
const val UPDATE_ITEM = 2
const val ADD_ITEM = 1

const val PHOTO_ON_SERVER = "photo on server"

class SqliteHandler(context: Context,factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME,factory, DATABASE_VERSION) {

    val activity = context as Activity
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
                    COLUMN_UPDATE_STATUS + " INTEGER" + ")")

        val CREATE_PHOTO_TABLE =
            ("CREATE TABLE " + PHOTO_TABLE + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REVIEW_ID + " INTEGER," +
                    COLUMN_PHOTO_ID + " INTEGER," +
                    COLUMN_UPDATE_STATUS + " INTEGER," +
                    COLUMN_PATH + " TEXT," +
                    " FOREIGN KEY ($COLUMN_REVIEW_ID) REFERENCES $REVIEW_TABLE($COLUMN_REVIEW_ID));")

        val CREATE_ATTRIBUTES_TABLE =
            ("CREATE TABLE " + ATTRIBUTES_TABLE + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REVIEW_ID + " INTEGER," +
                    COLUMN_IS_POSITIVE + " INTEGER," +
                    COLUMN_TEXT + " TEXT," +
                    " FOREIGN KEY ($COLUMN_REVIEW_ID) REFERENCES $REVIEW_TABLE($COLUMN_REVIEW_ID));")


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

    //Adding reviews to remember them in Sqlite
    fun addReview(reviewInfo: ReviewInfoItem) {
        val db = writableDatabase //Get db

        val values = ContentValues()
        values.put(COLUMN_REVIEW_ID, reviewInfo.review_id)
        values.put(COLUMN_USER_ID, reviewInfo.user_id)
        values.put(COLUMN_PRODUCT_ID, reviewInfo.product_id)
        values.put(COLUMN_TEXT, reviewInfo.text)
        values.put(COLUMN_SCORE, reviewInfo.score)
        values.put(COLUMN_LIKES, reviewInfo.likes)
        values.put(COLUMN_DISLIKES, reviewInfo.dislikes)
        values.put(CREATED_AT, reviewInfo.created_at)
        values.put(COLUMN_UPDATE_STATUS, NONE)

        db.insert(REVIEW_TABLE, null, values)

        for (image in reviewInfo.images) {
            addPhoto(reviewInfo.review_id, PHOTO_ON_SERVER, image.image_id, NONE)
        }

        for (attr in reviewInfo.attributes) {
            addAttribute(reviewInfo.review_id, attr)
        }

    }

    fun addAttribute(reviewID: Int, attr: ReviewAttributeInfo) {

        val db = writableDatabase

        val isPositive = if (attr.is_positive) 1 else 0

        val values = ContentValues()
        values.put(COLUMN_REVIEW_ID, reviewID)
        values.put(COLUMN_TEXT, attr.text)
        values.put(COLUMN_IS_POSITIVE, isPositive)

        db.insert(ATTRIBUTES_TABLE, null, values)
    }

    fun addPhoto(reviewID: Int, path: String, photoId: Int, status: Int) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_REVIEW_ID, reviewID)
        values.put(COLUMN_UPDATE_STATUS, status)
        values.put(COLUMN_PHOTO_ID, photoId)
        values.put(COLUMN_PATH, path)

        db.insert(PHOTO_TABLE, null, values)
    }

    fun getAllReviews(canBeDeleted: Boolean): MutableList<ReviewInfoItem> {

        val reviews = mutableListOf<ReviewInfoItem>()
        val db = writableDatabase
        val reviewsCursor: Cursor?
        try {
            if (canBeDeleted)
                reviewsCursor = db.rawQuery("select * from $REVIEW_TABLE", null)
            else
                reviewsCursor = db.rawQuery(
                    "select * from $REVIEW_TABLE where $COLUMN_UPDATE_STATUS<$DELETE_ITEM",
                    null
                )
        } catch (e: SQLiteException) {
            onCreate(db)
            return mutableListOf()
        }

        //Check all data
        if (reviewsCursor!!.moveToFirst()) {
            while (!reviewsCursor.isAfterLast) {
                reviews.add(parseReview(reviewsCursor))
                reviewsCursor.moveToNext()
            }
        }
        return reviews
    }

    fun parseReview(reviewsCursor: Cursor): ReviewInfoItem {
        val reviewId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_REVIEW_ID))
        val userId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_USER_ID))
        val productId = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_PRODUCT_ID))
        val text = reviewsCursor.getString(reviewsCursor.getColumnIndex(COLUMN_TEXT))
        val score = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_SCORE))
        val likes = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_LIKES))
        val dislikes = reviewsCursor.getInt(reviewsCursor.getColumnIndex(COLUMN_DISLIKES))
        val createdAt = reviewsCursor.getString(reviewsCursor.getColumnIndex(COLUMN_CREATED_AT))
        val attributes = parseAttributes(reviewId)
        val photos = parseServerPhotos(reviewId)

        return ReviewInfoItem(
            text,
            attributes,
            photos,
            likes,
            dislikes,
            productId,
            score,
            userId,
            reviewId,
            createdAt
        )
    }

    fun parseServerPhotos(reviewID: Int): MutableList<ImageInfo> {
        val photos = mutableListOf<ImageInfo>()
        val db = writableDatabase
        var photoCursor: Cursor? = null
        try {
            photoCursor = db.rawQuery(
                "select * from $PHOTO_TABLE where $COLUMN_REVIEW_ID=$reviewID and $COLUMN_PHOTO_ID > 0 and $COLUMN_UPDATE_STATUS=$NONE",
                null
            )
        } catch (e: SQLiteException) {
            photoCursor?.close()
            onCreate(db)
            return mutableListOf()
        }

        //Check all data
        if (photoCursor!!.moveToFirst()) {
            while (!photoCursor.isAfterLast) {
                val photoId = photoCursor.getInt(photoCursor.getColumnIndex(COLUMN_PHOTO_ID))

                photos.add(ImageInfo(photoId))
                photoCursor.moveToNext()
            }
        }

        photoCursor.close()

        return photos
    }

    fun parseLocalPhotos(reviewID: Int): MutableList<Uri> {
        val photos = mutableListOf<Uri>()
        val db = writableDatabase
        var photoCursor: Cursor? = null
        try {
            photoCursor = db.rawQuery(
                "select * from $PHOTO_TABLE where $COLUMN_REVIEW_ID=$reviewID and $COLUMN_PHOTO_ID <0",
                null
            )
        } catch (e: SQLiteException) {
            photoCursor?.close()
            onCreate(db)
            return mutableListOf()
        }

        //Check all data
        if (photoCursor!!.moveToFirst()) {
            while (!photoCursor.isAfterLast) {
                val path = photoCursor.getString(photoCursor.getColumnIndex(COLUMN_PATH))

                photos.add(Uri.parse(path))
                photoCursor.moveToNext()
            }
        }
        photoCursor.close()

        return photos
    }

    fun parseAttributes(reviewID: Int): MutableList<ReviewAttributeInfo> {
        val attributes = mutableListOf<ReviewAttributeInfo>()
        val db = writableDatabase
        var attributeCursor: Cursor? = null
        try {
            attributeCursor = db.rawQuery(
                "select * from $ATTRIBUTES_TABLE where $COLUMN_REVIEW_ID=$reviewID",
                null
            )
        } catch (e: SQLiteException) {
            attributeCursor?.close()
            onCreate(db)
            return mutableListOf()
        }

        //Check all data
        if (attributeCursor!!.moveToFirst()) {
            while (!attributeCursor.isAfterLast) {
                val text = attributeCursor.getString(attributeCursor.getColumnIndex(COLUMN_TEXT))
                val positiveRaw = attributeCursor.getInt(
                    attributeCursor.getColumnIndex(
                        COLUMN_IS_POSITIVE
                    )
                )

                val isPositive = positiveRaw != 0

                attributes.add(ReviewAttributeInfo(text, isPositive))
                attributeCursor.moveToNext()
            }
        }
        attributeCursor.close()

        return attributes
    }

    fun getReview(id: Int): ReviewInfoItem? {
        val db = writableDatabase
        val reviewsCursor: Cursor?
        try {
            reviewsCursor =
                db.rawQuery("select * from $REVIEW_TABLE where $COLUMN_REVIEW_ID=$id", null)
        } catch (e: SQLiteException) {
            onCreate(db)
            return null
        }

        //Check all data
        if (reviewsCursor!!.moveToFirst() && !reviewsCursor.isAfterLast) {
            return parseReview(reviewsCursor)
        }

        return null
    }

    fun updateReview(
        reviewData: ReviewPutInfo,
        reviewID: Int,
        deletePhotosServer: MutableList<Int>,
        photoUri: List<Uri>
    ) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_UPDATE_STATUS, UPDATE_ITEM)
        values.put(COLUMN_TEXT, reviewData.text)
        values.put(COLUMN_SCORE, reviewData.score)

        //Delete old attributes
        db.execSQL("DELETE FROM $ATTRIBUTES_TABLE WHERE $COLUMN_REVIEW_ID = $reviewID")

        //Add new attributes
        for (attr in reviewData.attributes) {
            addAttribute(reviewID, ReviewAttributeInfo(attr.text, attr.is_positive))
        }

        //Server photo update status to delete
        for (photo in deletePhotosServer) {
            val photoValues = ContentValues()
            photoValues.put(COLUMN_UPDATE_STATUS, DELETE_ITEM)
            db.update(PHOTO_TABLE, photoValues, "$COLUMN_PHOTO_ID=$photo", null)
        }


        //Local photos can be deleted right now
        db.execSQL("DELETE FROM $PHOTO_TABLE WHERE $COLUMN_REVIEW_ID = $reviewID and $COLUMN_PHOTO_ID < 0")

        //Add new local photos
        for (photo in photoUri) {
            addPhoto(reviewID, photo.toString(), -1, ADD_ITEM)
        }

        db.update(REVIEW_TABLE, values, "$COLUMN_REVIEW_ID=$reviewID", null)
    }

    fun deleteReview(id: Int) {
        val db = writableDatabase

        val values = ContentValues()
        values.put(COLUMN_UPDATE_STATUS, DELETE_ITEM)

        db.update(REVIEW_TABLE, values, "$COLUMN_REVIEW_ID=$id", null)
    }

    fun reloadTables() {
        val db = writableDatabase

        db.execSQL("DROP TABLE IF EXISTS $ATTRIBUTES_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $PHOTO_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $REVIEW_TABLE")
        onCreate(db)
    }

    fun syncChanges(loadedCallback: () -> Unit,internetErrorCallback: () -> Unit) {
        val db = writableDatabase
        val reviewsCursor: Cursor?
        try {
            reviewsCursor =
                db.rawQuery("select * from $REVIEW_TABLE where $COLUMN_UPDATE_STATUS>0", null)
        } catch (e: SQLiteException) {
            onCreate(db)
            return
        }

        val auth =
            activity.getSharedPreferences("com.mtaa.techtalk", ComponentActivity.MODE_PRIVATE)
                .getString("token", "") ?: ""

        MainScope().launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread

                    //Check all data
                    if (reviewsCursor!!.moveToFirst()) {
                        while (!reviewsCursor.isAfterLast) {

                            //Update review
                            val review = parseReview(reviewsCursor)
                            reviewsCursor.moveToNext()

                            DataGetter.updateReview(
                                ReviewPutInfo(
                                    review.text,
                                    review.attributes,
                                    review.score
                                ), review.review_id, auth
                            )

                            //Upload new photos
                            var photosCursor: Cursor? = null
                            try {
                                photosCursor = db.rawQuery(
                                    "select * from $PHOTO_TABLE where $COLUMN_REVIEW_ID=${review.review_id} and $COLUMN_PHOTO_ID<0 and $COLUMN_UPDATE_STATUS=$ADD_ITEM",
                                    null
                                )
                            } catch (e: SQLiteException) {
                                photosCursor?.close()
                                onCreate(db)
                                continue
                            }

                            if (photosCursor!!.moveToFirst()) {
                                while (!photosCursor.isAfterLast) {
                                    val path = photosCursor.getString(
                                        photosCursor.getColumnIndex(
                                            COLUMN_PATH
                                        )
                                    )

                                    DataGetter.uploadPhoto(
                                        review.review_id,
                                        Uri.parse(path),
                                        auth,
                                        activity
                                    )
                                    photosCursor.moveToNext()
                                }
                            }
                            photosCursor.close()

                            //Delete old photos
                            try {
                                photosCursor = db.rawQuery(
                                    "select * from $PHOTO_TABLE where $COLUMN_REVIEW_ID=${review.review_id} and $COLUMN_PHOTO_ID>0 and $COLUMN_UPDATE_STATUS=$DELETE_ITEM",
                                    null
                                )
                            } catch (e: SQLiteException) {
                                photosCursor.close()
                                onCreate(db)
                                continue
                            }

                            if (photosCursor!!.moveToFirst()) {
                                while (!photosCursor.isAfterLast) {
                                    val id = photosCursor.getInt(
                                        photosCursor.getColumnIndex(COLUMN_PHOTO_ID)
                                    )

                                    DataGetter.deletePhoto(review.review_id, id, auth)
                                    photosCursor.moveToNext()
                                }
                            }
                            photosCursor.close()
                        }
                    }
                }

                //After networking stuff is done
                loadedCallback()
            } catch (e: Exception) {
                println(e.stackTraceToString())
                internetErrorCallback()
            }
            finally {
                reviewsCursor?.close()
            }
        }

    }

    fun setReviewsToUpdated() {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_UPDATE_STATUS, NONE)
        db.update(REVIEW_TABLE, values, "$COLUMN_UPDATE_STATUS>$NONE", null)
    }
}