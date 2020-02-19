package com.n0xx1.livedetect.productsearch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.Vision.Images.Annotate;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.n0xx1.livedetect.BuildConfig;
import com.n0xx1.livedetect.MainActivity;
import com.n0xx1.livedetect.PackageManagerUtils;
import com.n0xx1.livedetect.objectdetection.DetectedObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;

public class SearchEngine {


    private static final String CLOUD_VISION_API_KEY = BuildConfig.apikey;
//    private static final String CLOUD_VISION_API_KEY = "AIzaSyA798GIEtoRBkoNioZBhPe0DV5MZHccb10";


    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final int MAX_DIMENSION = 1200;
    private static final int MAX_LABEL_RESULTS = 10;


    private static final String TAG = "SearchEngine";

    public interface SearchResultListener {
        void onSearchCompleted(DetectedObject object, List<Product> productList);
    }

    private final Context mContext;
    private final Activity mActivity;
    private SearchResultListener mListener;
    private DetectedObject mObject;

    private final ExecutorService requestCreationExecutor;

    public SearchEngine(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
//        searchRequestQueue = Volley.newRequestQueue(context);
        requestCreationExecutor = Executors.newSingleThreadExecutor();
    }

    public void search(Activity activity, DetectedObject object, SearchResultListener listener) {
        // Crops the object image out of the full image is expensive, so do it off the UI thread.

        mListener = listener;
        mObject = object;

        Tasks.call(requestCreationExecutor, () -> new DetectionTask((MainActivity) mActivity, prepareAnnotationRequest(object.getBitmap()), object.getBitmap())
//                prepareAnnotationRequest(object.getBitmap()))
        ).addOnSuccessListener(
                detectionTask -> detectionTask.execute()
        ).addOnFailureListener(
                e -> {
                    Log.e(TAG, "Failed to create product search request!", e);
                    // Remove the below dummy code after your own product search backed hooked up.
                    List<Product> productList = new ArrayList<>();
                    for (int i = 0; i < 8; i++) {
                        productList.add(
                                new Product(/* imageUrl= */ "", "Product title " + i, "Product subtitle " + i));
                    }
                    listener.onSearchCompleted(object, productList);
                });


    }

    private Annotate createRequest(DetectedObject searchingObject) throws Exception {
        byte[] objectImageData = searchingObject.getImageData();
        if (objectImageData == null) {
            throw new Exception("Failed to get object image data!");
        }

        // Hooks up with your own product search backend here.
        throw new Exception("Hooks up with your own product search backend.");

    }

    private Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
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

                        String packageName = mActivity.getClass().getPackage().getName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(mActivity.getPackageManager(), packageName);

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


    private class DetectionTask extends AsyncTask<Object, Void, BatchAnnotateImagesResponse> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;
        private Bitmap mBitmap;


        DetectionTask(MainActivity activity, Vision.Images.Annotate annotate, Bitmap bitmap) {
            mActivityWeakReference = new WeakReference<MainActivity>(activity);
            mRequest = annotate;
            mBitmap = bitmap;
        }

        @Override
        protected BatchAnnotateImagesResponse doInBackground(Object... params) {

            BatchAnnotateImagesResponse result = null;

            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();

//                labels = response.getResponses().get(0).getLabelAnnotations();
//                texts = response.getResponses().get(0).getTextAnnotations();


//                return convertResponseToString(labels, texts);
                return response;

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
//            return "Cloud Vision API request failed. Check logs for details.";

            return result;
        }

        protected void onPostExecute(BatchAnnotateImagesResponse result) {
            MainActivity activity = mActivityWeakReference.get();

            if (activity != null) {

                List<Product> productList = new ArrayList<>();


                List<EntityAnnotation> labels = result.getResponses().get(0).getLabelAnnotations();
                List<EntityAnnotation> texts = result.getResponses().get(0).getTextAnnotations();


                if (!labels.isEmpty()) {
                    for (int i = 0; i < labels.size(); i++) {

                        EntityAnnotation label = labels.get(i);
                        productList.add(
                                new Product(/* imageUrl= */ "", label.get("description").toString(), label.get("score").toString()));
                    }

                } else {

                    Log.d(TAG, "no search result");
//                    for (int i = 0; i < 8; i++) {
//                        productList.add(
//                                new Product(/* imageUrl= */ "", "Product title " + i, "Product subtitle " + i));
//                    }
                }


                mListener.onSearchCompleted(mObject, productList);

            }
        }


    }


    public void shutdown() {
//        searchRequestQueue.cancelAll(TAG);
        requestCreationExecutor.shutdown();
    }
}


