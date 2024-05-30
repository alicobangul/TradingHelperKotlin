package com.basesoftware.tradinghelperkotlin.util

import android.net.Uri
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.basesoftware.tradinghelperkotlin.R
import com.basesoftware.tradinghelperkotlin.model.domain.api.ApiResponseModel
import com.bumptech.glide.Glide
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYouListener
import com.google.gson.Gson
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import org.json.JSONObject
import java.util.Locale

object ExtensionUtil {

    fun String.toJsonObject() : JSONObject = JSONObject(this) // JSON stilindeki stringleri JSON objesine çevir [Volley JsonObjectRequest için gerekli]

    fun JSONObject.toResponseModel() : ApiResponseModel = Gson().fromJson(this.toString(), ApiResponseModel::class.java) // Gelen JSONObject türündeki veriyi modele çevir

    private fun String.convertChar() : String = replace(",",".") // Karakter çevir

    @BindingAdapter("android:shareImageCode")
    @JvmStatic
    fun shareImageCode(view : CircleImageView, code : String?) {

        // SVG türündeki resimlerin gösterilmesi için GlideToVectorYou kütüphanesi, eğer resim gösterilemez ise default resmi gösterir

        GlideToVectorYou
            .init()
            .with(view.context)
            .withListener(object : GlideToVectorYouListener {

                override fun onLoadFailed() {

                    Single
                        .fromCallable {
                            Glide.with(view.context)
                            .load(Constant.DEFAULT_IMAGE_URL)
                            .into(view)
                        }
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe()

                }

                override fun onResourceReady() {}

            })
            .load(
                Uri.parse(
                    String.format(Locale.getDefault(), Constant.IMAGE_URL, code)
                ), view
            )


    }

    @BindingAdapter("android:sharePrice")
    @JvmStatic
    fun sharePrice(view : TextView, price : Double?) {

        // Hissenin fiyatı

        /**
         * Eğer fiyat 100 ve üzerinde ise küsürat tek haneli aksi halde çift haneli
         * Örneğin : 100.7 [Tek haneli] - 25.60 [Çift haneli]
         */

        view.text = price?.let { String.format(if(it >= 100) "%.1f" else "%.2f", it).convertChar() } ?: "--" // Eğer fiyat yok ise -- yazılacak

    }

    @BindingAdapter("android:shareChangePrice")
    @JvmStatic
    fun shareChangePrice(view : TextView, changePrice : Double?) {

        // Hissenin değişen fiyatı (+Yeşil -Kırmızı 0 Gri) -> [+0.16] / [-7.13] / [0.00]

        view.apply {


            text = changePrice?.let {

                // Eğer fiyat 0.00'dan az ise başına -, fazla ise + ekle
                when{

                    it < 0.00 -> context.getString(R.string.negativeprice, String.format(Locale.getDefault(),"%.2f", it)).convertChar()

                    it == 0.00 -> context.getString(R.string.zeroprice).convertChar()

                    else -> context.getString(R.string.positiveprice, String.format(Locale.getDefault(),"%.2f", it)).convertChar()

                }

            } ?: "--"

        }

    }

    @BindingAdapter("android:shareChangeAbs")
    @JvmStatic
    fun shareChangeAbs(view : TextView, changeAbs : Double?) {

        // Değişim yüzdesi (+Yeşil -Kırmızı 0 Gri) -> [+0.44%] / [-4.00%] / [0.00%]

        view.apply {


            text = changeAbs?.let {

                // Eğer değişim 0.00'dan az ise başına -, fazla ise + ekle

                when {

                    it < 0.00 -> context.getString(R.string.negativechangeabs, String.format(Locale.getDefault(),"%.2f", it), "%").convertChar()

                    it == 0.00 -> context.getString(R.string.zerochangeabs).convertChar()

                    else -> context.getString(R.string.positivechangeabs, String.format(Locale.getDefault(),"%.2f", it), "%").convertChar()

                }

            } ?: "--"

        }

    }

}