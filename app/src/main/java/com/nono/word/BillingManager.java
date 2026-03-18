package com.nono.word;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    private final Activity activity;
    private final BillingClient billingClient;
    private ProductDetails removeAdsProduct;
    private OnPurchaseListener listener;

    public interface OnPurchaseListener {
        void onAdsRemoved();
    }

    public BillingManager(Activity activity) {
        this.activity = activity;
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        connectAndQueryPurchases();
    }

    private void connectAndQueryPurchases() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProduct();
                    restorePurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // 재연결은 다음 launchPurchase 시 시도
            }
        });
    }

    private void queryProduct() {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Constants.PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && !productDetailsList.isEmpty()) {
                removeAdsProduct = productDetailsList.get(0);
            }
        });
    }

    /** 이전 구매 복원 (앱 재설치 등) */
    private void restorePurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getProducts().contains(Constants.PRODUCT_REMOVE_ADS)
                                    && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                acknowledgeAndSave(purchase);
                            }
                        }
                    }
                }
        );
    }

    /** 결제 화면 실행 */
    public void launchPurchase() {
        if (isAdsRemoved()) {
            Toast.makeText(activity, "이미 광고가 제거되었습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (removeAdsProduct == null) {
            Toast.makeText(activity, "상품 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            if (!billingClient.isReady()) {
                connectAndQueryPurchases();
            }
            return;
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(removeAdsProduct)
                                .build()
                ))
                .build();

        billingClient.launchBillingFlow(activity, flowParams);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    acknowledgeAndSave(purchase);
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // 사용자 취소 - 아무 것도 안 함
        } else {
            Toast.makeText(activity, "결제 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void acknowledgeAndSave(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            com.android.billingclient.api.AcknowledgePurchaseParams params =
                    com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
            billingClient.acknowledgePurchase(params, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    saveAdsRemoved();
                }
            });
        } else {
            saveAdsRemoved();
        }
    }

    private void saveAdsRemoved() {
        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.KEY_ADS_REMOVED, true).apply();
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, "광고가 제거되었습니다! 감사합니다 🎉", Toast.LENGTH_LONG).show();
            if (listener != null) listener.onAdsRemoved();
        });
    }

    public boolean isAdsRemoved() {
        SharedPreferences prefs = activity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(Constants.KEY_ADS_REMOVED, false);
    }

    public static boolean isAdsRemoved(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(Constants.KEY_ADS_REMOVED, false);
    }

    public void setOnPurchaseListener(OnPurchaseListener listener) {
        this.listener = listener;
    }

    public void destroy() {
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}
