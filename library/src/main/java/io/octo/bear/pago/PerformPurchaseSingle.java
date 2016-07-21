package io.octo.bear.pago;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import io.octo.bear.pago.model.entity.Order;
import io.octo.bear.pago.model.entity.Purchase;
import io.octo.bear.pago.model.entity.PurchaseType;
import io.octo.bear.pago.model.entity.ResponseCode;
import io.octo.bear.pago.model.exception.BillingException;
import rx.Single;
import rx.SingleSubscriber;

import static io.octo.bear.pago.BillingServiceUtils.GSON;
import static io.octo.bear.pago.BillingServiceUtils.checkResponseAndThrowIfError;
import static io.octo.bear.pago.BillingServiceUtils.retrieveResponseCode;

/**
 * Created by shc on 14.07.16.
 */

class PerformPurchaseSingle extends Single<Order> {

    private static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    private static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    private static final String RESPONSE_INAPP_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";

    PerformPurchaseSingle(final Context context, final PurchaseType type, final String sku, String payload) {
        super(subscriber -> new BillingServiceConnection(context, service -> {
                    try {
                        final Bundle buyIntentBundle = service.getBuyIntent(Pago.BILLING_API_VERSION, context.getPackageName(),
                                sku, type.value, payload);

                        final ResponseCode responseCode = retrieveResponseCode(buyIntentBundle);

                        checkResponseAndThrowIfError(responseCode);

                        final PendingIntent buyIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
                        if (buyIntent == null) {
                            throw new RuntimeException("unable to retrieve buy intent");
                        }

                        LocalBroadcastManager
                                .getInstance(context)
                                .registerReceiver(
                                        createPurchaseBroadcastReceiver(payload, subscriber),
                                        new IntentFilter(BillingActivity.ACTION_PURCHASE));

                        BillingActivity.start(context, buyIntent);
                    } catch (BillingException e) {
                        subscriber.onError(e);
                    }

                }).bindService()
        );
    }

    private static BroadcastReceiver createPurchaseBroadcastReceiver(
            final String payload, final SingleSubscriber<? super Order> subscriber) {

        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent data) {
                try {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);

                    final Bundle result = data.getExtras();

                    final boolean success = result.getBoolean(BillingActivity.EXTRA_SUCCESS, false);
                    if (!success) {
                        throw new BillingException(ResponseCode.ITEM_UNAVAILABLE);
                    }

                    final ResponseCode code = retrieveResponseCode(result);

                    checkResponseAndThrowIfError(code);

                    final Purchase purchase = GSON.fromJson(result.getString(RESPONSE_INAPP_PURCHASE_DATA), Purchase.class);
                    final Order order = new Order(purchase, result.getString(RESPONSE_INAPP_DATA_SIGNATURE));

                    final boolean purchaseDataIsCorrect = TextUtils.equals(payload, purchase.developerPayload);

                    if (purchaseDataIsCorrect) {
                        subscriber.onSuccess(order);
                    } else {
                        throw new RuntimeException("purchase data doesn't match with data that was sent in request");
                    }
                } catch (BillingException e) {
                    subscriber.onError(e);
                }
            }
        };
    }

}