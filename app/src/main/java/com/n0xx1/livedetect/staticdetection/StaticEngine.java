package com.n0xx1.livedetect.staticdetection;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Vertex;
import com.n0xx1.livedetect.BuildConfig;
import com.n0xx1.livedetect.PackageManagerUtils;
import com.n0xx1.livedetect.camera.GraphicOverlay;
import com.n0xx1.livedetect.camera.WorkflowModel;
import com.n0xx1.livedetect.camera.WorkflowModel.WorkflowState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaticEngine {

    private static final String TAG = "StaticEngine";

    private static final int MAX_DIMENSION = 1200;
    private static final int MAX_LABEL_RESULTS = 10;

    private static final String CLOUD_VISION_API_KEY = BuildConfig.apikey;


    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    public interface StaticResultListener {
        void onStaticCompleted(List<Text> textList, Bitmap image, Bitmap image_rect);
    }

    private final Context mContext;
    private StaticResultListener mListener;
    private final WorkflowModel workflowModel;
    private final GraphicOverlay graphicOverlay;
    private Bitmap image;

    public StaticEngine(Context mContext, WorkflowModel workflowModel, GraphicOverlay graphicOverlay) {

        this.mContext = mContext;
        this.workflowModel = workflowModel;
        this.graphicOverlay = graphicOverlay;

    }

    public void detect(Bitmap image, StaticResultListener listener){

        this.image = image;
        this.mListener = listener;

        if (image != null) {
            // scale the image to save on bandwidth
            Bitmap bitmap =
                    scaleBitmapDown(image,MAX_DIMENSION);

//                mMainImage.setImageBitmap(bitmap);

        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(mContext, "Image picker gave us a null image.", Toast.LENGTH_SHORT).show();
        }

        ValueAnimator loadingAnimator = createLoadingAnimator();
        loadingAnimator.start();
        graphicOverlay.add(new StaticLoadingGraphic(graphicOverlay, loadingAnimator));
        workflowModel.setWorkflowState(WorkflowState.SEARCHING);

        callCloudVision(image);
    }

    public Bitmap getImage(){
        return image;
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
//        mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, AnnotateImageResponse> DetectionTask = new DetectionTask(prepareAnnotationRequest(bitmap), bitmap);
            DetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private class DetectionTask extends AsyncTask<Object, Void, AnnotateImageResponse> {
        private Vision.Images.Annotate mRequest;
        private Bitmap mBitmap;
        private List<EntityAnnotation> labels;
        List<EntityAnnotation> texts;

        DetectionTask(Vision.Images.Annotate annotate, Bitmap bitmap) {
            mRequest = annotate;
            mBitmap= bitmap;
        }

        @Override
        protected AnnotateImageResponse doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();

//                labels = response.getResponses().get(0).getLabelAnnotations();
//                texts = response.getResponses().get(0).getTextAnnotations();

//                return convertResponseToString(labels, texts);

                return response.getResponses().get(0);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
//            return "Cloud Vision API request failed. Check logs for details.";
              return new AnnotateImageResponse();
        }

        protected void onPostExecute(AnnotateImageResponse result) {

            List<EntityAnnotation> texts = result.getTextAnnotations();

            List<Text> textList = new ArrayList<>();

            if (texts!= null && !texts.isEmpty()) {
                for (int i = 0; i < texts.size(); i++) {

                    EntityAnnotation text = texts.get(i);
                    textList.add(
                            new Text(/* imageUrl= */ null, text.getDescription(), (ArrayList)text.getBoundingPoly().getVertices()));
                }

            } else {

                Log.d(TAG, "no search result");

            }

            Bitmap mBitmapRect = textRect(texts, mBitmap);
            Log.d(TAG, "******mBitmapRect01: "+mBitmapRect);

            mListener.onStaticCompleted(textList, mBitmap, mBitmapRect);
        }
    }


    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = mContext.getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(mContext.getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{

            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);



            annotateImageRequest.setImage(base64EncodedImage);


            // add the features we want :: label_detection
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("label_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);

                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                add(textDetection);
            }});



            // Add the list of one thing to the request
            add(annotateImageRequest);

        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static String convertResponseToString(List<EntityAnnotation> labels, List<EntityAnnotation> texts) {
        StringBuilder message = new StringBuilder("I found these things:\n\n");

        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message.append(String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription()));
                message.append("\n\n");
            }
        } else {
            message.append("nothing");
        }

        if (texts != null) {
            for (EntityAnnotation text : texts) {
                message.append(text.getDescription()+"\n\n");
                message.append(text.getBoundingPoly());
            }
        } else {
            message.append("nothing");
        }



        return message.toString();
    }

    private static Bitmap textRect(List<EntityAnnotation> texts, Bitmap bitmap) {

        //The Color of the Rectangle to Draw on top of Text
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);

        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);

        for (EntityAnnotation text : texts) {

            Path path = new Path();
            //                path.reset(); // only needed when reusing this path for a new build

            ArrayList <Vertex> vertices = (ArrayList) text.getBoundingPoly().getVertices();

            Vertex vertexFirst = vertices.get(0);
            path.moveTo(vertexFirst.getX(), vertexFirst.getY()); // used for first point
            vertices.remove(0);

            for (Vertex vertex : vertices){
                path.lineTo(vertex.getX(), vertex.getY()); // there is a setLastPoint action but i found it not to work as expected
            }
            path.lineTo(vertexFirst.getX(), vertexFirst.getY());
            canvas.drawPath(path, paint);
        }

        return tempBitmap;

    }



    private ValueAnimator createLoadingAnimator() {
        float endProgress = 1.1f;
        ValueAnimator loadingAnimator = ValueAnimator.ofFloat(0f, endProgress);
        loadingAnimator.setDuration(2000);
        loadingAnimator.addUpdateListener(
                animation -> {
                    if (Float.compare((float) loadingAnimator.getAnimatedValue(), endProgress) >= 0) {
                        graphicOverlay.clear();
                    } else {
                        graphicOverlay.invalidate();
                    }
                });
        return loadingAnimator;
    }
}
