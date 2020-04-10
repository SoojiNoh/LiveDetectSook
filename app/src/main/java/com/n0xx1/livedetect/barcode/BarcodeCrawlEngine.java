package com.n0xx1.livedetect.barcode;

import android.os.AsyncTask;

import com.n0xx1.livedetect.camera.WorkflowModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BarcodeCrawlEngine {

    private static final String TAG = "BarcodeCrawlEngine";

    private WorkflowModel workflowModel;

    private String htmlPageUrl;
//    private String htmlContentInStringFormat="";

    private String barcodeValue;
    private String title;
    private String description;
    private JsoupAsyncTask jsoupAsyncTask;

    public BarcodeCrawlEngine(WorkflowModel workflowModel, String barcode){

        this.workflowModel = workflowModel;
        this.barcodeValue = barcode;
        htmlPageUrl = "http://www.koreannet.or.kr/home/hpisSrchGtin.gs1?gtin="+barcode;

        jsoupAsyncTask = new JsoupAsyncTask();

    }

    public void crawl(){
        jsoupAsyncTask.execute();
    }

//    public String getHtmlContent(){
//        return htmlContentInStringFormat;
//    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {

        Elements name;
        Elements content;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            crawl();
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            Barcode barcode = new Barcode(title, barcodeValue, description);
            workflowModel.barcode.setValue(barcode);
        }

        private void crawl(){
            try {
                Document doc = Jsoup.connect(htmlPageUrl).get();
                name = doc.select("div[class=productTit]");
                content = doc.select("dd[class=productDetail]");


                String name_string = "";
                Matcher matcher;
                Pattern r;
//                Log.i("logcat", htmlPageUrl);
                String pattern_name = "(\\d+)(\\s*)(.*)";
                r = Pattern.compile(pattern_name);

                matcher = r.matcher(name.text());
                if (matcher.find( )) {
                    name_string = matcher.group(3);
                }



//                htmlContentInStringFormat = "";
//                Log.i("logcat","zz"+name.text().trim()+"zz");
                if (name_string.equals("")){
//                    htmlContentInStringFormat="본 상품의 정보를 찾을 수 없습니다. 죄송합니다.";
                } else {
                    title = name_string.trim();
                    description = content.text().trim();
//                    htmlContentInStringFormat+="본 상품의 이름은 " + (name_string.trim()) + "입니다.";
//                    htmlContentInStringFormat+="더 자세하게 말씀드리자면 " + (content.text().trim()) + "입니다.";
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
