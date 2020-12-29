package com.god.seep.base.binding;

import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.god.seep.base.util.ImageLoader;
import com.god.seep.base.widget.GlideRoundTransform;

public class ImageViewBindingAdapter {

    @BindingAdapter(value = {"imageUrl", "centerCrop", "roundCorner", "radius"}, requireAll = false)
    public static void loadImage(ImageView view, String url, Boolean centerCrop, boolean roundCorner, float radius) {
        if (centerCrop == null) {
            centerCrop = true;
        }
        if (roundCorner) {
            ImageLoader.loadImage(view.getContext(), url, view, new GlideRoundTransform(view.getContext()), centerCrop);
        } else {
            ImageLoader.loadImage(view.getContext(), url, view, centerCrop);
        }
    }
}
