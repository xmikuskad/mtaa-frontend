package com.mtaa.techtalk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class RegisterInfo(val name: String, val password: String, val email: String)
data class LoginInfo(val password: String, val email: String)
data class UserInfo(val name: String, val trust_score: Int, val reviews: MutableList<ReviewInfoItem>)
data class AuthInfo(val name: String, val key: String)
data class NameInfo(val name: String)

/**
 * Categories
 * */
data class CategoriesInfo(val categories: MutableList<CategoryInfo>)
data class CategoryInfo(val name: String, val category_id: Int)

/**
 * Brands
 * */
data class BrandInfo(val name: String, val brand_id: Int)
data class BrandsInfo(val brands: MutableList<BrandInfo>)

/**
 * Products
 * */
data class ProductInfo(val name: String, val score: Int, val price: Int, val product_id: Int)
data class AddedProduct(val name: String, val price: Int, val category_id: Int, val brand_id: Int)
data class ProductsInfo(val products: MutableList<ProductInfo>)

/**
 * Reviews GET
 * */
//Single review
data class ReviewInfo(val text: String, val attributes: MutableList<ReviewAttributeInfo>,
                      val images: MutableList<ImageInfo>, val likes:Int, val dislikes:Int,
                      val product_id: Int, val score: Int, val user_id: Int,
                      val created_at: String)
//When returning array of reviews
@Parcelize
data class ReviewInfoItem(val text: String, val attributes: MutableList<ReviewAttributeInfo>,
                          val images: MutableList<ImageInfo>, val likes:Int, val dislikes:Int,
                          val product_id: Int, val score: Int, val user_id: Int,
                          val review_id: Int, val created_at: String) :Parcelable

data class ReviewsInfo(val reviews: MutableList<ReviewInfoItem>)
@Parcelize
data class ReviewAttributeInfo(val text: String, val is_positive: Boolean):Parcelable
@Parcelize
data class ImageInfo(val image_id: Int):Parcelable

/**
 * Reviews POST
 * */
data class ReviewPostInfo(val text: String, val attributes: MutableList<ReviewAttributePostPutInfo>,
                          val product_id: Int, val score: Int)

data class ReviewAttributePostPutInfo(val text: String, val is_positive: Boolean)

/**
 * Reviews other
 */

data class ReviewPutInfo(val text: String, val attributes: MutableList<ReviewAttributePostPutInfo>, val score: Int)
data class ReviewVotesInfo(val likes:Int, val dislikes:Int)
data class ReviewIdInfo(val id:Int)

/***
 * Other data classes
 */

data class QueryAttributes(var order_by:String, var order_type:String, var brands:String, var min_price :Float, var max_price:Float, var min_score:Float)