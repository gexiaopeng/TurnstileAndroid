/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.aip.face.turnstile.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class ImageUtil {

    public static void resize(Bitmap bitmap, File outputFile, int maxWidth, int maxHeight) {
        try {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            // 图片大于最大高宽，按大的值缩放
            if (bitmapWidth > maxHeight || bitmapHeight > maxWidth) {
                float widthScale = maxWidth * 1.0f / bitmapWidth;
                float heightScale = maxHeight * 1.0f / bitmapHeight;

                float scale = Math.min(widthScale, heightScale);
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, false);
            }
            LogUtil.i("APIService", "upload face size" + bitmap.getWidth() + "*" + bitmap.getHeight());
            // save image
            FileOutputStream out = new FileOutputStream(outputFile);
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resize(String inputPath, String outputPath, int dstWidth, int dstHeight) {
        try {
            int inWidth;
            int inHeight;

            // decode image size (decode metadata only, not the whole image)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(inputPath, options);

            // save width and height
            inWidth = options.outWidth;
            inHeight = options.outHeight;
            LogUtil.i("APIService", "origin " + inWidth + " " + inHeight);

            Matrix m = new Matrix();
            ExifInterface exif = new ExifInterface(inputPath);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (rotation != 0) {
                m.preRotate(ExifUtil.exifToDegrees(rotation));
            }

            int maxPreviewImageSize = Math.max(dstWidth, dstHeight);
            int size = Math.min(options.outWidth, options.outHeight);
            size = Math.min(size, maxPreviewImageSize);

            options = new BitmapFactory.Options();
            options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
            options.inScaled = true;
            options.inDensity = options.outWidth;
            options.inTargetDensity = size * options.inSampleSize;

            Bitmap roughBitmap = BitmapFactory.decodeFile(inputPath, options);
            roughBitmap = Bitmap.createBitmap(roughBitmap, 0, 0, roughBitmap.getWidth(),
                    roughBitmap.getHeight(), m, false);
            // save image
            FileOutputStream out = new FileOutputStream(outputPath);
            try {
                roughBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static void saveBitmap(String outputPath, Bitmap bitmap) {
        // save image
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputPath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
