package com.seven.itempurchase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.billingclient.api.*
import com.seven.itempurchase.databinding.ActivityMainBinding
import java.io.IOException
import java.security.Signature
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var billingClient:BillingClient? = null
    var response: String? = null
    var des: String? = null
    var sku: String? = null
    var isSuccess = false
    var isRemoveAdds = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isRemoveAdds == true){
           // binding.textView27.visibility = View.VISIBLE
            binding.textView27.text = "Already View Privacy Policy"
        }else{
           // binding.textView27.visibility = View.GONE
            binding.textView27.text = "View Privacy Policy"
        }

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        //getPrice()
        query_purchase()

    }

    fun btn_sub_click(view: View){
        billingClient!!.startConnection(object :BillingClientStateListener{
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("click500")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()

                )
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                billingClient!!.queryProductDetailsAsync(params.build()){
                    billingResult,productDetailsList ->

                    for (productDetails in productDetailsList){
                        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken
                        val productDetailsParamsList =
                            listOf(
                                offerToken?.let {
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(it)
                                        .build()
                                }
                            )
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                        val billingResult = billingClient!!.launchBillingFlow(this@MainActivity,billingFlowParams)
                    }
                }
            }

        })
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, Purchase ->

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && Purchase != null) {
            for (purchase in Purchase) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            binding.tvSubsts.setText("Already Purchase")
            isSuccess = true
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            binding.tvSubsts.setText("FEATURE_NOT_SUPPORTED")

        } else {
            Toast.makeText(
                applicationContext,
                "Error " + billingResult.debugMessage,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

     fun handlePurchase(purchase: Purchase) {
         val consumeParams = ConsumeParams.newBuilder()
             .setPurchaseToken(purchase.purchaseToken)
             .build()
         val listener = ConsumeResponseListener { billingResult, s ->
             if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

             }
         }
         billingClient!!.consumeAsync(consumeParams, listener)
         if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
             if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                 Toast.makeText(
                     applicationContext, "Error : invalid Purchase",
                     Toast.LENGTH_SHORT
                 ).show()
                 return
             }
             if (!purchase.isAcknowledged) {
                 val acknowledgePurchaseParams = AcknowledgePurchaseParams
                     .newBuilder()
                     .setPurchaseToken(purchase.purchaseToken)
                     .build()
                 billingClient!!.acknowledgePurchase(
                     acknowledgePurchaseParams,
                     acknowledgePurchaseResponseListener)
                 binding.tvSubsts.text = "Purchased !!"
                 isSuccess = true
                 binding.btnSub.visibility = View.GONE
             } else {
                 binding.tvSubsts.text = "Already Purchase"
                 binding.btnSub.visibility = View.GONE
             }
         }  else if (purchase.purchaseState == Purchase.PurchaseState.PENDING){
             binding.tvSubsts.text = "Purchase Pending"
         }  else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE){
             binding.tvSubsts.text = "UNSPECIFIED_STATE"
         }

    }

    var acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                binding.tvSubsts.text = "Purchased"
                isSuccess = true
            }

    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            val base64Key ="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtpOxWdiDIqrl9RP1HTMuauAI0Rz7D9CZ2qRWtr/EVcyEMo8klwuMBWJTQVibydD//sJ0pEnDCErotmxmifm4wjkH2Nk7tss92m6TAue9uvP19yVcWPFBjISxbPmQ9QSgnh/p6RW3swctY9cYOnA1M5Fp4MnXtesq5i4zfnINOESHqMDlqgOk1yyVx/UBVpRlD1r+P5/RhgtMp0nogXVatPgc+09CFazjfjb/mvHqjLGEDYmLxNMfbdl0+XTVo8QfibnLD8FckTWI53qVM1Ywgpzttk23Cx/ivScVDSOAaazpLqq+WI8bYSuF2hglxfIfjUNLP9qucfmPephyEU6obQIDAQAB"
            val security = Security()
            security.verifyPurchase(base64Key,signedData,signature)
        } catch (e: IOException){
            false
        }
    }

    private fun getPrice(){
        billingClient!!.startConnection(object :BillingClientStateListener{
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute{
                    val productList =
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("click500")
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
                    billingClient!!.queryProductDetailsAsync(params.build()){billingResult, productDetailsList ->
                        for (productDetails in productDetailsList){
                            response = productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.
                                    pricingPhaseList?.get(0)?.formattedPrice
                            sku = productDetails.name
                            val ds = productDetails.description
                            des = "$sku : $ds : price: $response"
                        }
                    }
                }
                runOnUiThread{
                    try {
                        Thread.sleep(4000)
                    }catch (e: InterruptedException){
                        e.printStackTrace()
                    }

                    binding.txtprice.text = "Price: $response"
                    binding.tvBenifit.text = des
                    binding.tvSubid.text = sku

                }
            }

        })
    }

    fun btn_get_price(view: View){
        getPrice()
    }

    fun btn_quit(view: View){
        finish()
    }

    private fun query_purchase() {
        billingClient!!.startConnection(object :BillingClientStateListener{
            override fun onBillingServiceDisconnected() {

            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute{
                    try {
                        billingClient!!.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        ){
                            billingResult, purchaseList ->
                            for (purchase in purchaseList) {
                                if (purchase != null && purchase.isAcknowledged){
                                    isSuccess = true
                                    isRemoveAdds = true
                                    binding.btnSub.visibility = View.GONE
                                    binding.tvSubsts.text = "Already Purchased"
                                  //  sku = purchase.originalJson.toString()
                                }
                            }
                        }
                    } catch (ex: java.lang.Exception){
                        isRemoveAdds = false
                    }
                }
                runOnUiThread{
                    try {
                        Thread.sleep(2000)
                    } catch (e: InterruptedException){
                        e.printStackTrace()
                    }
                }
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null){
            billingClient!!.endConnection()
        }
    }
}