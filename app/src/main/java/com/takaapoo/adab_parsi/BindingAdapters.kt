package com.takaapoo.adab_parsi

import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.takaapoo.adab_parsi.add.imagePrefix
import com.takaapoo.adab_parsi.network.PoetProperty
import com.takaapoo.adab_parsi.util.GlideApp
import java.io.File
import kotlin.math.min

@BindingAdapter("imageUrl" , "ancient")
fun bindImage(imgView: ImageView, imgUrl: String?, ancient: Int) {
    val width = imgView.layoutParams.width

    GlideApp.with(imgView)
        .load(
            if (imgUrl.isNullOrEmpty()) {
                if (ancient == 0) R.drawable.tomb else R.drawable.person
            } else imgUrl
        )
        .timeout(15000)
        .placeholder(R.drawable.poet_loading_placeholder)
        .transform(RoundedCorners((width/9)))
        .apply(
            RequestOptions()/*.override(width, width)*/
            .error( if (ancient == 0) R.drawable.tomb else R.drawable.person )
        )
        .into(imgView)
}

@BindingAdapter(value = ["poetId", "allPoet"], requireAll = false)
fun publicationText(text: TextView, poetID: Int, allPoet: List<PoetProperty>){
    var myText = "شامل : \n"
    allPoet.filter { it.poetID == poetID && it.parentID != 0 }.forEach {item ->
        myText += "•  ${item.text} \n"}
    text.text = myText.removeSuffix("\n")
}

@BindingAdapter("poetText")
fun poetText(textView: TextView, text: String?){
    textView.text = text?.substringBefore("*")
}

@BindingAdapter("poetDescriptionText")
fun poetDescripText(textView: TextView, text: String){
    textView.text = text.substringAfter("*")
}

@BindingAdapter(value = ["property", "allPoet"], requireAll = false)
fun spacerVisibility(view: View, property: PoetProperty, allPoet: List<PoetProperty>){
    view.visibility = if(!allPoet.contains(property)
        || property.poetID == allPoet.lastOrNull { it.ancient == 0 && it.parentID == 0}?.poetID
        || property.poetID == allPoet.lastOrNull { it.ancient == 1 && it.parentID == 0}?.poetID) View.INVISIBLE
        else View.VISIBLE
}


//@BindingAdapter("viewImage", "ancient")
//fun imageViewImage(imgView: ImageView, id: Int, ancient: Int){
//    if (id == 2) {
//        imgView.setImageResource(R.drawable.hafez)
////        image = ResourcesCompat.getDrawable(imgView.resources, R.drawable.hafez, null)?.toBitmap()
//        return
//    }
//    val filePath = "${imgView.context.filesDir}/" + imagePrefix + "$id"
//    val image = if (File(filePath).exists())
//        BitmapFactory.decodeFile(filePath)
//    else
//        ResourcesCompat.getDrawable(imgView.resources,
//            if (ancient == 1) R.drawable.person_3_4 else R.drawable.tomb_16_9, null)?.toBitmap()
//
//    imgView.setImageBitmap(image)
//}

@BindingAdapter("viewImage", "ancient")
fun imageViewImage(imgView: ImageView, id: Int, ancient: Int){
    val fragment = imgView.findFragment<Fragment>()
    if (id == 2) {
        GlideApp.with(fragment).load(R.drawable.hafez).into(imgView)
        return
    }
    File("${imgView.context.filesDir}/" + imagePrefix + "$id").let {
        if (it.exists()) {
            GlideApp.with(fragment).load(Uri.fromFile(it)).into(imgView)
        } else
            GlideApp.with(fragment).load(if (ancient == 1) R.drawable.person_3_4 else R.drawable.tomb_16_9)
                .into(imgView)
    }
}

@BindingAdapter("collapsetoolbarTitle")
fun title(toolbar: com.google.android.material.appbar.CollapsingToolbarLayout, text: String?){
    toolbar.title = text?.substringBefore("*")
}

@BindingAdapter("toolbarTitle")
fun toolbarTitle(toolbar: androidx.appcompat.widget.Toolbar, text: String?){
    toolbar.title = text?.substringBefore("*")
}


fun Int.px(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

@BindingAdapter(value = ["open", "play"], requireAll = true)
fun setHeight(viewGroup: ViewGroup, open: Boolean, play: Boolean) {
    val triAngle = viewGroup.getChildAt(0)
    val recyclerView = viewGroup.getChildAt(3)
    val params = recyclerView.layoutParams
    val speed = 3.px(recyclerView.context)

    if (open){
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val recHeight = recyclerView.measuredHeight
        if (play) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = min ((recyclerView.measuredHeight / speed).toLong(), 500)
                addUpdateListener { updatedAnimation ->
                    params.height = ((updatedAnimation.animatedValue as Float) * recHeight).toInt()
                    recyclerView.layoutParams = params
                    triAngle.rotation = (updatedAnimation.animatedValue as Float) * (-90f)
                }
                start()
            }
        }
        else{
            triAngle.rotation = -90f
            params.height = WRAP_CONTENT
            recyclerView.layoutParams = params
        }

    } else{
        if (play) {
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = min((recyclerView.measuredHeight / speed).toLong(), 500)
                addUpdateListener { updatedAnimation ->
                    params.height =
                        ((updatedAnimation.animatedValue as Float) * recyclerView.height).toInt()
                    recyclerView.layoutParams = params
                    triAngle.rotation = (updatedAnimation.animatedValue as Float) * (-90f)
                }
            }.start()
        } else{
            triAngle.rotation = 0f
            params.height = 0
            recyclerView.layoutParams = params
        }
    }
}

@BindingAdapter("borderImage")
fun borderImage(imgView: ImageView, id: Int){
    imgView.setImageResource(R.drawable.border)
}


