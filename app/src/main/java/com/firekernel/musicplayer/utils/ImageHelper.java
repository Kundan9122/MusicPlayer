package com.firekernel.musicplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.util.LruCache;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firekernel.musicplayer.FireApplication;
import com.firekernel.musicplayer.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by Ashish on 6/22/2017.
 * Responsible for Album Art image loading and Blurred background loading
 * Caches the blurred bitmaps to increase the UI performance
 */

public class ImageHelper {
    private static final String TAG = FireLog.makeLogTag(ImageHelper.class);
    private static final int MAX_CACHE_SIZE = 5 * 1024 * 1024;  // 5 MB
    private static final int IN_SAMPLE_SIZE = 6;
    private static final String DEFAULT_ART_KEY = "default_art_key";
    private static final String DEFAULT_BG_KEY = "default_bg_key";
    private static LruCache<String, Bitmap> blurredDrawableCache;

    static {
        int maxSize = Math.min(MAX_CACHE_SIZE,
                (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 4)));
        blurredDrawableCache = new LruCache<>(maxSize);
    }

    public static void loadAvatar(@NonNull Context context, @NonNull ImageView view, @NonNull String artUri) {
        FireLog.d(TAG, "(++) loadAvatar: context=" + context + ", view=" + view + ", artUri=" + artUri);
    }

    public static void loadArt(@NonNull final Context context, @NonNull final ImageView artView,
                               @NonNull MediaDescriptionCompat description) {
        FireLog.e(TAG, "(++) loadArt: context=" + context + ", artView=" + artView + ", description=" + description);

        String artUrl = null;
        Uri iconUri = description.getIconUri();
        if (iconUri != null) {
            artUrl = iconUri.toString();
        }
        Glide.with(context)
                .load(artUrl)
                .error(R.drawable.ic_default_art)
                .into(artView);
    }

    public static void loadBlurBg(Context context, ImageView view) {
        loadBlurBg(context, view, getDefaultBgArt(), DEFAULT_BG_KEY);
    }

    public static void loadBlurBg(@NonNull final Context context, @NonNull final ImageView bgView,
                                  @NonNull MediaDescriptionCompat description) {
        Uri iconUri = description.getIconUri();
        if (iconUri == null) {
            ImageHelper.loadBlurBg(context, bgView, getDefaultArt(), DEFAULT_ART_KEY);
            return;
        }
        final String artUrl = iconUri.toString();
        Glide.with(context)
                .load(artUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(200, 200) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        ImageHelper.loadBlurBg(context, bgView, resource, artUrl);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        ImageHelper.loadBlurBg(context, bgView, getDefaultArt(), DEFAULT_ART_KEY);
                    }
                });
    }

    public static void loadArtAndBlurBg(@NonNull final Context context, @NonNull final ImageView artView,
                                        @NonNull final ImageView bgView, @NonNull MediaDescriptionCompat description) {
        Uri iconUri = description.getIconUri();
        if (iconUri == null) {
            artView.setImageBitmap(getDefaultArt());
            ImageHelper.loadBlurBg(context, bgView, getDefaultArt(), DEFAULT_ART_KEY);
            return;
        }
        final String artUrl = iconUri.toString();
        Glide.with(context)
                .load(artUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(200, 200) {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation glideAnimation) {
                        artView.setImageBitmap(resource);
                        ImageHelper.loadBlurBg(context, bgView, resource, artUrl);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        artView.setImageBitmap(getDefaultArt());
                        ImageHelper.loadBlurBg(context, bgView, getDefaultArt(), DEFAULT_ART_KEY);
                    }
                });
    }

    private static void loadBlurBg(Context context, ImageView view, Bitmap loadedImage, String key) {
        FireLog.d(TAG, "(++) loadBlurBg: context=" + context + ", view=" + view + ", loaded image="
                + loadedImage + ", key=" + key);

        if (context == null || view == null || loadedImage == null) {
            return;
        }

        /*
         * if blurred image is found in cache then load it from cache
         */
        if (key != null && blurredDrawableCache.get(key) != null) {
            Bitmap drawable = blurredDrawableCache.get(key);
            setBackground(context, view, drawable);
            return;
        }

        /*
         * if key not found in cache load blurred image async
         */
        LoadBlurredImageTask blurredImageTask = new LoadBlurredImageTask(context, view, key);
        blurredImageTask.execute(loadedImage);
    }

    private static Bitmap getDefaultBgArt() {
        return BitmapFactory.decodeResource(FireApplication.getInstance().getResources(), R.drawable.bg_art);
    }

    private static Bitmap getDefaultArt() {
        return BitmapFactory.decodeResource(FireApplication.getInstance().getResources(), R.drawable.ic_default_art);
    }

    private static void setBackground(@NonNull Context context, @NonNull ImageView imageView, @NonNull Bitmap bitmap) {
        Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);

        if (imageView.getDrawable() != null) {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{imageView.getDrawable(), drawable});
            imageView.setImageDrawable(td);
            td.startTransition(200);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }

    private static Bitmap createBlurredBitmapFromBitmap(Bitmap bitmap, Context context, int inSampleSize) {
        RenderScript rs = RenderScript.create(context);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);
        Bitmap blurTemplate = BitmapFactory.decodeStream(bis, null, options);

        Allocation input = Allocation.createFromBitmap(rs, blurTemplate);
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(8f);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(blurTemplate);
        return blurTemplate;
    }

    private static class LoadBlurredImageTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private Context context;
        private ImageView imageView;
        private String key;

        LoadBlurredImageTask(Context context, ImageView imageView, String key) {
            this.context = context;
            this.imageView = imageView;
            this.key = key;
        }

        @Override
        protected Bitmap doInBackground(Bitmap... loadedImage) {
            Bitmap drawable = null;
            try {
                drawable = createBlurredBitmapFromBitmap(loadedImage[0], context, IN_SAMPLE_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (context == null || ((Activity) context).isFinishing()
                    || ((Activity) context).isDestroyed()) {
                return;
            }
            if (result != null) {
                if (key != null)
                    blurredDrawableCache.put(key, result);
                setBackground(context, imageView, result);
            }
        }
    }
}
