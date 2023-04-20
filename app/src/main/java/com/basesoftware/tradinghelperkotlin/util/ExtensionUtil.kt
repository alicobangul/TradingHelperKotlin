package com.basesoftware.tradinghelperkotlin.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basesoftware.tradinghelperkotlin.R
import com.basesoftware.tradinghelperkotlin.adapter.ShareAdapter
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

    fun JSONObject.toResponseModel() : ApiResponseModel = Gson().fromJson(this.toString(), ApiResponseModel::class.java) // Gelen JSONObject türündeki veriyi modele çevir

    fun HttpURLConnection.connectionSettings() {

        this.apply {
            requestMethod = "POST" // Kullanılacak method
            useCaches = false // Cache kullanılsın mı
            allowUserInteraction = false // userInteraction değeri
            doInput = true // Giriş/çıkış için url kullanımı
            doOutput = true // Giriş/çıkış için url kullanımı
        }

    } // HttpUrlConnection settings ayarları

    fun RecyclerView.settings(context: Context, shareAdapter: ShareAdapter) {

        apply {
            setHasFixedSize(true) // Boyutunun değişmeyeceği bildirilidi (performans için)
            layoutManager = LinearLayoutManager(context) // LayoutManager ayarlandı
            adapter = shareAdapter // Adaptör bağlandı
            adapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY // Recyclerview state kaydedildi eğer veri var ise eski konuma gidecek
        }

    }

    fun Any?.isNotNull() = this != null // Boş değilse true, boş ise false

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

        if(price == null) view.text = "--" // Eğer fiyat yok ise -- yazılacak

        /**
         * Eğer fiyat 100 ve üzerinde ise küsürat tek haneli aksi halde çift haneli
         * Örneğin : 100.7 [Tek haneli] - 25.60 [Çift haneli]
         */
        price?.let { view.text = String.format(if(it >= 100) "%.1f" else "%.2f", it).convertChar() }

    }

    @BindingAdapter("android:shareChangePrice")
    @JvmStatic
    fun shareChangePrice(view : TextView, changePrice : Double?) {

        // Hissenin değişen fiyatı (+Yeşil -Kırmızı 0 Gri) -> [+0.16] / [-7.13] / [0.00]

        view.apply {

            if (changePrice == null) {
                setTextColor(ContextCompat.getColor(view.context, R.color.gray)) // Değişen fiyat yok ise yazı gri renk
                text = "--"
            }

            changePrice?.let {

                // Eğer fiyat 0.00'dan az ise başına -, fazla ise + ekle
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
        // Değişim yüzdesi (+Yeşil -Kırmızı 0 Gri) -> [+0.44%] / [-4.00%] / [0.00%]

        view.apply {

            if(changeAbs == null) {

                setTextColor(ContextCompat.getColor(view.context, R.color.gray)) // Eğer değişim yüzdesi yoksa yazı gri renk
                text = "--"

            }

            changeAbs?.let {

                // Eğer değişim 0.00'dan az ise başına -, fazla ise + ekle

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