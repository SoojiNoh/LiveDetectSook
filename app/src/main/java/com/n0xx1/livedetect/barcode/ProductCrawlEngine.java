package com.n0xx1.livedetect.barcode;

import android.os.AsyncTask;

import com.n0xx1.livedetect.camera.WorkflowModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProductCrawlEngine {

    private static final String TAG = "BarcodeCrawlEngine";

    private WorkflowModel workflowModel;

    private String htmlPageUrl;
//    private String htmlContentInStringFormat="";

    private BarcodedObject barcodedObject;

    List<Product> listProduct;

    public ProductCrawlEngine(WorkflowModel workflowModel, BarcodedObject barcodedObject){

        this.workflowModel = workflowModel;
        this.barcodedObject = barcodedObject;

        htmlPageUrl="https://search.shopping.naver.com/search/all.nhn?query="+barcodedObject.getName()+"&cat_id=&frm=NVSHATC";


        JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
        jsoupAsyncTask.execute();

    }

//    public String getHtmlContent(){
//        return htmlContentInStringFormat;
//    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {

        String title,price,img;

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
            workflowModel.barcodedProducts.setValue(new BarcodedProducts(barcodedObject, listProduct ));
        }

        private void crawl(){
            try {
                listProduct = new ArrayList<Product>();

                Document doc = Jsoup.connect(htmlPageUrl).get();


                for (int i=1 ; i<=5 ; i++){
                    title = doc.select("li[data-expose-rank="+i+"] div[class=tit] em").text();
                    price = doc.select("li[data-expose-rank="+i+"] span[class=price] em").text();
                    img = doc.select("li[data-expose-rank="+i+"] img[class=_productLazyImg]").attr("src");
                    listProduct.add(new Product(title, price, img));
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
