package com.basesoftware.tradinghelperkotlin.util

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.basesoftware.tradinghelperkotlin.R
import com.basesoftware.tradinghelperkotlin.model.ApiRequestModel
import com.basesoftware.tradinghelperkotlin.model.ApiResponseModel
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYouListener
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.net.HttpURLConnection

object ExtensionUtil {

    fun ApiRequestModel.toJsonText() : String = Gson().toJson(this) // Data class ile oluşturulan request modelleri json stilinde stringe çevir

    fun String.toJsonObject() : JSONObject = JSONObject(this) // JSON stilindeki stringleri JSON objesine çevir [volley JsonObjectRequest için gerekli]

    fun JSONObject.toResponseModel() : ApiResponseModel = Gson().fromJson(this.toString(), ApiResponseModel::class.java) // Gelen JSONObject türündeki veriyi data class'a çevir

    fun HttpURLConnection.connectionSettings() {

        this.apply {
            requestMethod = "POST"
            useCaches = false
            allowUserInteraction = false
            doInput = true
            doOutput = true
        }

    } // HttpUrlConnection settings ayarları

    fun Any?.isNotNull() = this != null

    private fun String.convertChar() : String = replace(",",".")



    @BindingAdapter("android:shareImageCode")
    @JvmStatic
    fun shareImageCode(view : CircleImageView, code : String?) {

        // Resim kodu

        GlideToVectorYou
            .init()
            .with(view.context)
            .withListener(object : GlideToVectorYouListener {

                override fun onLoadFailed() {

                    Handler(Looper.myLooper()!!).post {
                        Glide.with(view.context)
                            .load("https://avatars.githubusercontent.com/u/7644688?s=200&v=4")
                            .into(view)
                    }

                }

                override fun onResourceReady() {}

            })
            .load(Uri.parse("https://s3-symbol-logo.tradingview.com/$code--big.svg"), view)

    }

    @BindingAdapter("android:sharePrice")
    @JvmStatic
    fun sharePrice(view : TextView, price : Double?) {
        // Hissenin fiyatı

        if(price == null) view.text = "--"

        price?.let { view.text = String.format(if(it >= 100) "%.1f" else "%.2f", it).convertChar() }

    }

    @BindingAdapter("android:shareChangePrice")
    @JvmStatic
    fun shareChangePrice(view : TextView, changePrice : Double?) {

        // Hissenin değişen fiyatı (+yeşil -kırmızı 0 gri) -> [+0.16] / [-7.13] / [0.00]

        view.apply {

            if (changePrice == null) {
                setTextColor(ContextCompat.getColor(view.context, R.color.gray))
                text = "--"
            }

            changePrice?.let {

                when{
                    it < 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.red))
                        text = context.getString(R.string.negativeprice, String.format("%.2f", it)).convertChar()
                    }
                    it == 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.gray))
                        text = context.getString(R.string.zeroprice).convertChar()
                    }
                    it > 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.green))
                        text = context.getString(R.string.positiveprice, String.format("%.2f", it)).convertChar()
                    }
                }

            }

        }

    }

    @BindingAdapter("android:shareChangeAbs")
    @JvmStatic
    fun shareChangeAbs(view : TextView, changeAbs : Double?) {
        // Değişim yüzdesi (+yeşil -kırmızı 0 gri) -> [+0.44%] / [-4.00%] / [0.00%]

        view.apply {

            if(changeAbs == null) {

                setTextColor(ContextCompat.getColor(view.context, R.color.gray))
                text = "--"

            }

            changeAbs?.let {

                when {
                    it < 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.red))
                        text = context.getString(R.string.negativechangeabs, String.format("%.2f", it), "%").convertChar()
                    }
                    it == 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.gray))
                        text = context.getString(R.string.zerochangeabs).convertChar()
                    }
                    it > 0.00 -> {
                        setTextColor(ContextCompat.getColor(view.context, R.color.green))
                        text = context.getString(R.string.positivechangeabs, String.format("%.2f", it), "%").convertChar()
                    }
                }

            }

        }

    }

}