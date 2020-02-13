package com.viclab.ocr.mathpix;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.viclab.ocr.mathpix.api.request.SingleProcessRequest;
import com.viclab.ocr.mathpix.api.response.DetectionResult;
import okhttp3.*;

public class MathpixOCR extends AsyncTask<MathpixOCR.UploadParams, Void, MathpixOCR.Result> {

    private final ResultListener listener;

    public MathpixOCR(ResultListener listener) {
        this.listener = listener;
    }

    @Override
    protected Result doInBackground(UploadParams... arr) {
        UploadParams params = arr[0];
        Result result;
        try {
            OkHttpClient client = new OkHttpClient();
            SingleProcessRequest singleProcessRequest = new SingleProcessRequest(params.image);
            MediaType JSON = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(new Gson().toJson(singleProcessRequest), JSON);

            Request request = new Request.Builder()
                    .url(Constant.base_Url)
                    .addHeader("content-type", "application/json")
                    .addHeader("app_id", Constant.app_id)
                    .addHeader("app_key", Constant.app_key)
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            String responseString = response.body().string();
            Log.d("MathPix", responseString);
            DetectionResult detectionResult = new Gson().fromJson(responseString, DetectionResult.class);
            if (detectionResult != null && detectionResult.latex != null) {
                result = new ResultSuccessful(detectionResult.latex);
            } else if (detectionResult != null && detectionResult.error != null) {
                result = new ResultFailed(detectionResult.error);
            } else {
                result = new ResultFailed("Math not found");
            }
        } catch (Exception e) {
            result = new ResultFailed("Failed to send to server. Check your connection and try again");
        }
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result instanceof ResultSuccessful) {
            ResultSuccessful successful = (ResultSuccessful) result;
            listener.onSuccess(successful.latex);
        } else if (result instanceof ResultFailed) {
            ResultFailed failed = (ResultFailed) result;
            listener.onError(failed.message);
        }
    }

    public interface ResultListener {
        void onError(String message);

        void onSuccess(String url);
    }

    public static class UploadParams {
        private Bitmap image;

        public UploadParams(Bitmap image) {
            this.image = image;
        }
    }

    public static class Result {
    }

    private static class ResultSuccessful extends Result {
        String latex;

        ResultSuccessful(String latex) {
            this.latex = latex;
        }
    }

    private static class ResultFailed extends Result {
        String message;

        ResultFailed(String message) {
            this.message = message;
        }
    }
}
