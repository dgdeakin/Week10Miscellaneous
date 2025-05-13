package com.example.week10miscellaneous;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
import com.google.android.gms.wallet.contract.TaskResultContracts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CheckoutActivity1 extends AppCompatActivity {

    private PaymentsClient paymentsClient;
    private PayButton googlePayButton;
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

    private final ActivityResultLauncher<Task<PaymentData>> paymentDataLauncher =
            registerForActivityResult(new TaskResultContracts.GetPaymentDataResult(), result -> {
                int statusCode = result.getStatus().getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.SUCCESS:
                        handlePaymentSuccess(result.getResult());
                        break;
                    case CommonStatusCodes.CANCELED:
                        Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
                        break;
                    case CommonStatusCodes.DEVELOPER_ERROR:
                        handleError(statusCode, result.getStatus().getStatusMessage());
                        break;
                    default:
                        handleError(statusCode, "Unexpected error");
                        break;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Initialize PaymentsClient
        paymentsClient = Wallet.getPaymentsClient(this,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build());

        // Initialize UI
        googlePayButton = findViewById(R.id.googlePayButton);
        try {
            googlePayButton.initialize(
                    ButtonOptions.newBuilder()
                            .setAllowedPaymentMethods(getAllowedPaymentMethods().toString())
                            .build()
            );
            googlePayButton.setOnClickListener(this::requestPayment);
        } catch (JSONException e) {
            Log.e("CheckoutActivity", "Error initializing button", e);
        }

        // Check if Google Pay is available
        checkGooglePayAvailability();
    }

    private void checkGooglePayAvailability() {
        try {
            JSONObject isReadyToPayJson = getIsReadyToPayRequest();
            IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString());
            Task<Boolean> task = paymentsClient.isReadyToPay(request);
            task.addOnCompleteListener(taskResult -> {
                if (taskResult.isSuccessful() && taskResult.getResult()) {
                    googlePayButton.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Google Pay is not available", Toast.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            Log.e("CheckoutActivity", "Error checking availability", e);
        }
    }

    private JSONObject getBaseRequest() throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0);
    }

    private JSONObject getGatewayTokenizationSpecification() throws JSONException {
        return new JSONObject()
                .put("type", "PAYMENT_GATEWAY")
                .put("parameters", new JSONObject()
                        .put("gateway", "example")
                        .put("gatewayMerchantId", "exampleGatewayMerchantId"));
    }

    private JSONArray getAllowedCardNetworks() {
        return new JSONArray()
                .put("AMEX")
                .put("DISCOVER")
                .put("JCB")
                .put("MASTERCARD")
                .put("VISA");
    }

    private JSONArray getAllowedCardAuthMethods() {
        return new JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS");
    }

    private JSONObject getBaseCardPaymentMethod() throws JSONException {
        return new JSONObject()
                .put("type", "CARD")
                .put("parameters", new JSONObject()
                        .put("allowedAuthMethods", getAllowedCardAuthMethods())
                        .put("allowedCardNetworks", getAllowedCardNetworks())
                        .put("billingAddressRequired", true)
                        .put("billingAddressParameters", new JSONObject()
                                .put("format", "FULL")));
    }

    private JSONObject getCardPaymentMethod() throws JSONException {
        return getBaseCardPaymentMethod()
                .put("tokenizationSpecification", getGatewayTokenizationSpecification());
    }

    private JSONArray getAllowedPaymentMethods() throws JSONException {
        return new JSONArray().put(getCardPaymentMethod());
    }

    private JSONObject getIsReadyToPayRequest() throws JSONException {
        return getBaseRequest()
                .put("allowedPaymentMethods", new JSONArray().put(getBaseCardPaymentMethod()));
    }

    private JSONObject getTransactionInfo() throws JSONException {
        return new JSONObject()
                .put("totalPrice", "10.00")
                .put("totalPriceStatus", "FINAL")
                .put("countryCode", "US")
                .put("currencyCode", "USD")
                .put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE");
    }

    private JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject().put("merchantName", "Example Merchant");
    }

    private JSONObject getPaymentDataRequest() throws JSONException {
        return getBaseRequest()
                .put("allowedPaymentMethods", getAllowedPaymentMethods())
                .put("transactionInfo", getTransactionInfo())
                .put("merchantInfo", getMerchantInfo())
                .put("shippingAddressRequired", true)
                .put("shippingAddressParameters", new JSONObject()
                        .put("phoneNumberRequired", false)
                        .put("allowedCountryCodes", new JSONArray().put("US")));
    }

    public void requestPayment(View view) {
        try {
            JSONObject paymentDataRequestJson = getPaymentDataRequest();
            PaymentDataRequest request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString());
            Task<PaymentData> task = paymentsClient.loadPaymentData(request);
            paymentDataLauncher.launch(task);
        } catch (JSONException e) {
            Log.e("CheckoutActivity", "Error requesting payment", e);
        }
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        try {
            JSONObject paymentInfo = new JSONObject(paymentData.toJson());
            JSONObject paymentMethodData = paymentInfo.getJSONObject("paymentMethodData");
            JSONObject info = paymentMethodData.getJSONObject("info");
            String billingName = info.getJSONObject("billingAddress").getString("name");

            // Sample token handling (using provided sample tokens)
            String token = paymentMethodData.getJSONObject("tokenizationData").getString("token");
            Log.d("Google Pay token", token);

            // Example of processing sample Visa token
            JSONObject sampleVisaToken = new JSONObject()
                    .put("gatewayMerchantId", "some-merchant-id")
                    .put("messageExpiration", "1650574736277")
                    .put("messageId", "AH2Ejtc88ZHJ-2aYBQWzHwvp6l0JsCHgxVt8s91A-ZUikaXNbcjsFm6gg9ExeVR-jzIyT-mJvA_ntvfRsDDOH2jnKMjdTtXIJvPt0NBUU45R7-gnjxkx-sI0ldcWvbDHsV0735yFDbWk")
                    .put("paymentMethod", "CARD")
                    .put("paymentMethodDetails", new JSONObject()
                            .put("expirationYear", 2028)
                            .put("expirationMonth", 12)
                            .put("pan", "4111111111111111")
                            .put("authMethod", "PAN_ONLY"));

            Toast.makeText(this, "Payment successful for " + billingName, Toast.LENGTH_LONG).show();

            // Start success activity or return to main
            startActivity(new Intent(this, MainActivity.class));
        } catch (JSONException e) {
            Log.e("handlePaymentSuccess", "Error: " + e);
        }
    }

    private void handleError(int statusCode, @Nullable String message) {
        Log.e("loadPaymentData failed",
                String.format("Error code: %d, Message: %s", statusCode, message));
        Toast.makeText(this, "Payment error: " + message, Toast.LENGTH_LONG).show();
    }
}