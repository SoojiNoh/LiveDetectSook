package com.n0xx1.livedetect.barcode;

import android.os.AsyncTask;
import android.util.Log;

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

    private Barcode barcode;

    List<BarcodedProduct> listProduct;
    JsoupAsyncTask jsoupAsyncTask;

    public ProductCrawlEngine(WorkflowModel workflowModel, Barcode barcode){

        this.workflowModel = workflowModel;
        this.barcode = barcode;

        htmlPageUrl="https://search.shopping.naver.com/search/all.nhn?origQuery="+barcode.getName()+"pagingIndex=1&pagingSize=40&viewType=list&sort=price_asc&frm=NVSHATC&query="+barcode.getName();

        Log.i(TAG, "*****"+htmlPageUrl);

        jsoupAsyncTask = new JsoupAsyncTask();

    }

    public void crawl(){
        jsoupAsyncTask.execute();
    }

//    public String getHtmlContent(){
//        return htmlContentInStringFormat;
//    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {

        String title, price, imgUrl, siteUrl;

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
            BarcodedEntity barcodedEntity = new BarcodedEntity(barcode, listProduct);
            workflowModel.barcodedEntity.setValue(barcodedEntity);
        }

        private void crawl(){
            try {
                listProduct = new ArrayList<BarcodedProduct>();

                Document doc = Jsoup.connect(htmlPageUrl).get();

                for (int i=1 ; i<=doc.select("ul[class=goods_list]").size() ; i++){
                    title = doc.select("li[data-expose-rank="+i+"] div[class=tit]").text();
                    price = doc.select("li[data-expose-rank="+i+"] span[class=price] em").text().replaceAll("[^0-9]", "");
                    imgUrl = doc.select("li[data-expose-rank="+i+"] img[class=_productLazyImg]").attr("data-original");
                    siteUrl = doc.select("li[data-expose-rank="+i+"] div[class=tit] a").text();
                    listProduct.add(new BarcodedProduct(title, price, imgUrl, siteUrl));
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }



}
